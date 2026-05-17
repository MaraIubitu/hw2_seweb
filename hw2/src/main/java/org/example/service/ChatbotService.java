package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatbotService {

    @Autowired
    private VectorDatabaseService vectorDatabaseService;

    @Autowired
    private RdfService rdfService;

    @Value("${llm.api.url:https://router.huggingface.co/v1}")
    private String llmApiUrl;

    @Value("${llm.api.key:}")
    private String llmApiKey;

    @Value("${llm.model:Qwen/Qwen2.5-7B-Instruct}")
    private String llmModel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate context-aware conversation starters based on current page.
     */
    public List<String> generateConversationStarters(String pageContext) {
        List<String> starters = new ArrayList<>();

        if (pageContext == null || pageContext.isEmpty()) {
            starters.add("What book would you recommend for me?");
            starters.add("Show me all available books");
            starters.add("What are the different book genres available?");
        } else if (pageContext.startsWith("book:")) {
            String bookId = pageContext.substring(5);
            Book book = rdfService.getBookByUri("http://example.org/book/" + bookId);

            if (book != null) {
                starters.add("Tell me more about " + book.getTitle());
                starters.add("What do readers say about " + book.getTitle() + "?");
                starters.add("Are there similar books to " + book.getTitle() + "?");
            }
        } else if (pageContext.equals("books-list")) {
            starters.add("What is a book I am most likely to enjoy from this list?");
            starters.add("Filter books by theme");
            starters.add("Show me books suitable for beginners");
        }

        return starters;
    }

    /**
     * Process chatbot query with RAG.
     */
    public String processChatQuery(String message, String pageContext) {
        try {
            // Get relevant context from vector database
            String context = vectorDatabaseService.getContextForQuery(message);

            // Build the prompt with RAG context
            String prompt = buildPromptWithContext(message, context);

            // Try LLM first, fall back to rule-based using original message
            return callLLM(prompt, message);
        } catch (Exception e) {
            log.error("Error processing chat query", e);
            return generateFallbackResponse(message);
        }
    }

    /**
     * Build a prompt that includes RAG context.
     */
    private String buildPromptWithContext(String userMessage, String context) {
        return String.format(
            "You are a helpful book recommendation assistant. Use the following book information to answer questions accurately.\n\n" +
            "BOOK DATABASE:\n%s\n\n" +
            "IMPORTANT: Answer based ONLY on the books in the database above. Do not use general knowledge about books.\n" +
            "If a book is not in the database, say so explicitly.\n" +
            "Keep your responses concise and helpful.\n\n" +
            "User Question: %s",
            context, userMessage
        );
    }

    /**
     * Call LLM API. Tries configured API, falls back to rule-based.
     * @param prompt The full RAG prompt for the LLM
     * @param originalMessage The original user message for fallback
     */
    private String callLLM(String prompt, String originalMessage) {
        // Check for API key from config or environment
        String apiKey = (llmApiKey != null && !llmApiKey.isBlank()) ? llmApiKey : System.getenv("HF_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            log.info("No Hugging Face API key configured, using rule-based fallback");
            return generateFallbackResponse(originalMessage);
        }

        try {
            return callHuggingFaceApi(prompt, apiKey);
        } catch (Exception e) {
            log.warn("Hugging Face API call failed: {}, using fallback", e.getMessage());
            return generateFallbackResponse(originalMessage);
        }
    }

    /**
     * Call Hugging Face Inference API (OpenAI-compatible chat/completions format).
     */
    private String callHuggingFaceApi(String prompt, String apiKey) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmModel);
        requestBody.put("max_tokens", 300);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        String payload = objectMapper.writeValueAsString(requestBody);

        String baseUrl = llmApiUrl.endsWith("/") ? llmApiUrl.substring(0, llmApiUrl.length() - 1) : llmApiUrl;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText("");
                if (!content.isBlank()) {
                    return content.trim();
                }
            }
        }

        log.warn("Hugging Face API returned status {}: {}", response.statusCode(), response.body());
        return generateFallbackResponse(prompt);
    }

    /**
     * Generate a rule-based fallback response by analyzing the user query
     * and matching against the RDF book database.
     */
    private String generateFallbackResponse(String prompt) {
        String queryLower = prompt.toLowerCase();
        List<Book> allBooks = rdfService.getAllBooks();

        // Search by author + theme combination
        if (queryLower.contains("author") && queryLower.contains("theme")) {
            return handleAuthorThemeSearch(queryLower, allBooks);
        }

        // Search by author only
        if (queryLower.contains("author") || queryLower.contains("written by") || queryLower.contains("who wrote")) {
            return handleAuthorSearch(queryLower, allBooks);
        }

        // Search by theme/genre
        if (queryLower.contains("theme") || queryLower.contains("genre")) {
            return handleThemeSearch(queryLower, allBooks);
        }

        // Search for beginner books
        if (queryLower.contains("beginner")) {
            List<Book> beginnerBooks = allBooks.stream()
                    .filter(b -> "Beginner".equalsIgnoreCase(b.getReadingLevel()))
                    .collect(Collectors.toList());
            if (!beginnerBooks.isEmpty()) {
                return "Books suitable for beginners: " + formatBookList(beginnerBooks);
            }
            return "No beginner-level books found in the database.";
        }

        // Search for intermediate books
        if (queryLower.contains("intermediate")) {
            List<Book> books = allBooks.stream()
                    .filter(b -> "Intermediate".equalsIgnoreCase(b.getReadingLevel()))
                    .collect(Collectors.toList());
            if (!books.isEmpty()) {
                return "Books for intermediate readers: " + formatBookList(books);
            }
            return "No intermediate-level books found in the database.";
        }

        // Search for advanced books
        if (queryLower.contains("advanced")) {
            List<Book> books = allBooks.stream()
                    .filter(b -> "Advanced".equalsIgnoreCase(b.getReadingLevel()))
                    .collect(Collectors.toList());
            if (!books.isEmpty()) {
                return "Books for advanced readers: " + formatBookList(books);
            }
            return "No advanced-level books found in the database.";
        }

        // Show all books
        if (queryLower.contains("all books") || queryLower.contains("available books") || queryLower.contains("show me")) {
            if (!allBooks.isEmpty()) {
                return "Here are all the books in our database:\n" + formatBookListDetailed(allBooks);
            }
            return "No books found in the database.";
        }

        // Recommend
        if (queryLower.contains("recommend")) {
            return "Based on our database, here are the available books:\n" + formatBookListDetailed(allBooks) +
                    "\nPlease tell me your preferred theme or reading level for a personalized recommendation!";
        }

        // Search for specific book by title keywords
        for (Book book : allBooks) {
            if (queryLower.contains(book.getTitle().toLowerCase())) {
                return String.format("'%s' by %s is a %s level book with themes: %s.",
                        book.getTitle(), book.getAuthor(), book.getReadingLevel(),
                        String.join(", ", book.getThemes()));
            }
        }

        // Default response
        return "I can help you find books! Here's what I can do:\n" +
                "- Search by author (e.g., 'books by Frank Herbert')\n" +
                "- Search by theme (e.g., 'Science Fiction books')\n" +
                "- Search by author and theme (e.g., 'What book has author Frank Herbert and theme Science Fiction?')\n" +
                "- Filter by reading level (beginner, intermediate, advanced)\n" +
                "- Show all available books\n\n" +
                "We currently have " + allBooks.size() + " books in the database.";
    }

    /**
     * Handle combined author + theme search.
     */
    private String handleAuthorThemeSearch(String queryLower, List<Book> allBooks) {
        // Extract author and theme names from the query by matching against known values
        List<Book> matches = new ArrayList<>();

        for (Book book : allBooks) {
            boolean authorMatch = queryLower.contains(book.getAuthor().toLowerCase());
            boolean themeMatch = book.getThemes().stream()
                    .anyMatch(t -> queryLower.contains(t.toLowerCase()));

            if (authorMatch && themeMatch) {
                matches.add(book);
            }
        }

        if (!matches.isEmpty()) {
            if (matches.size() == 1) {
                Book b = matches.get(0);
                return String.format("Based on our database, the book that matches is '%s' by %s. " +
                        "It is a %s level book with themes: %s.",
                        b.getTitle(), b.getAuthor(), b.getReadingLevel(),
                        String.join(", ", b.getThemes()));
            }
            return "Books matching your criteria: " + formatBookList(matches);
        }

        // Try partial matching
        return handleAuthorSearch(queryLower, allBooks);
    }

    /**
     * Handle author-only search.
     */
    private String handleAuthorSearch(String queryLower, List<Book> allBooks) {
        for (Book book : allBooks) {
            String authorLower = book.getAuthor().toLowerCase();
            // Check if any part of the author name appears in the query
            String[] authorParts = authorLower.split("\\s+");
            boolean found = false;
            for (String part : authorParts) {
                if (part.length() > 2 && queryLower.contains(part)) {
                    found = true;
                    break;
                }
            }
            if (found || queryLower.contains(authorLower)) {
                List<Book> authorBooks = allBooks.stream()
                        .filter(b -> b.getAuthor().equalsIgnoreCase(book.getAuthor()))
                        .collect(Collectors.toList());
                return "Books by " + book.getAuthor() + ": " + formatBookList(authorBooks);
            }
        }

        // Collect all unique authors
        List<String> authors = allBooks.stream()
                .map(Book::getAuthor)
                .distinct()
                .collect(Collectors.toList());
        return "I couldn't find that author. Available authors in our database: " + String.join(", ", authors) + ".";
    }

    /**
     * Handle theme-only search.
     */
    private String handleThemeSearch(String queryLower, List<Book> allBooks) {
        // Collect all known themes
        Set<String> allThemes = allBooks.stream()
                .flatMap(b -> b.getThemes().stream())
                .collect(Collectors.toSet());

        // Try to match a theme from the query
        for (String theme : allThemes) {
            if (queryLower.contains(theme.toLowerCase())) {
                List<Book> themeBooks = allBooks.stream()
                        .filter(b -> b.getThemes().stream()
                                .anyMatch(t -> t.equalsIgnoreCase(theme)))
                        .collect(Collectors.toList());
                return "Books with theme '" + theme + "': " + formatBookList(themeBooks);
            }
        }

        return "Available themes in our database: " + String.join(", ", allThemes) + ".";
    }

    private String formatBookList(List<Book> books) {
        return books.stream()
                .map(b -> String.format("'%s' by %s (%s)", b.getTitle(), b.getAuthor(), b.getReadingLevel()))
                .collect(Collectors.joining(", "));
    }

    private String formatBookListDetailed(List<Book> books) {
        StringBuilder sb = new StringBuilder();
        for (Book book : books) {
            sb.append(String.format("- '%s' by %s | Level: %s | Themes: %s\n",
                    book.getTitle(), book.getAuthor(), book.getReadingLevel(),
                    String.join(", ", book.getThemes())));
        }
        return sb.toString();
    }
}
