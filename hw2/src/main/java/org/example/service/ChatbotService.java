package org.example.service;

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

    @Value("${llm.api.url:http://localhost:1234/v1}")
    private String llmApiUrl;

    @Value("${llm.api.key:}")
    private String llmApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Generate context-aware conversation starters based on current page
     */
    public List<String> generateConversationStarters(String pageContext) {
        List<String> starters = new ArrayList<>();

        if (pageContext == null || pageContext.isEmpty()) {
            // General starters for the main page
            starters.add("What book would you recommend for me?");
            starters.add("Show me all available books");
            starters.add("What are the different book genres available?");
        } else if (pageContext.startsWith("book:")) {
            // Specific book starters
            String bookId = pageContext.substring(5);
            Book book = rdfService.getBookByUri("http://example.org/book/" + bookId);

            if (book != null) {
                starters.add("Tell me more about " + book.getTitle());
                starters.add("What do readers say about " + book.getTitle() + "?");
                starters.add("Are there similar books to " + book.getTitle() + "?");
            }
        } else if (pageContext.equals("books-list")) {
            // Book listing page starters
            starters.add("What is a book I am most likely to enjoy from this list?");
            starters.add("Filter books by theme");
            starters.add("Show me books suitable for beginners");
        }

        return starters;
    }

    /**
     * Process chatbot query with RAG
     */
    public String processChatQuery(String message, String pageContext) {
        try {
            // Get relevant context from vector database
            String context = vectorDatabaseService.getContextForQuery(message);

            // Build the prompt with RAG context
            String prompt = buildPromptWithContext(message, context);

            // Call LLM
            String response = callLLM(prompt);

            return response;
        } catch (Exception e) {
            log.error("Error processing chat query", e);
            return "I'm having trouble processing your request. Please try again.";
        }
    }

    /**
     * Build a prompt that includes RAG context
     */
    private String buildPromptWithContext(String userMessage, String context) {
        return String.format(
            "You are a helpful book recommendation assistant. Use the following book information to answer questions accurately.\n\n" +
            "BOOK DATABASE:\n%s\n\n" +
            "IMPORTANT: Answer based ONLY on the books in the database above. Do not use general knowledge about books.\n" +
            "If a book is not in the database, say so explicitly.\n\n" +
            "User Question: %s\n\n" +
            "Assistant:",
            context, userMessage
        );
    }

    /**
     * Call LLM API (local or cloud-based)
     */
    private String callLLM(String prompt) throws IOException, InterruptedException {
        // For now, return a simple response
        // In production, this would call an actual LLM API
        
        try {
            return callOpenRouterAPI(prompt);
        } catch (Exception e) {
            log.warn("LLM API call failed, using fallback response", e);
            return generateFallbackResponse(prompt);
        }
    }

    /**
     * Call OpenRouter.ai API
     */
    private String callOpenRouterAPI(String prompt) throws IOException, InterruptedException {
        // This requires an API key from OpenRouter.ai
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null) {
            return generateFallbackResponse(prompt);
        }

        String payload = "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"" 
            + escapeJsonString(prompt) + "\"}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse JSON response (simplified)
        // In production, use a JSON library
        if (response.statusCode() == 200) {
            // Extract the message from the response
            String body = response.body();
            if (body.contains("\"content\"")) {
                int start = body.indexOf("\"content\":\"") + 11;
                int end = body.indexOf("\"", start);
                if (start > 10 && end > start) {
                    return unescapeJsonString(body.substring(start, end));
                }
            }
        }

        return generateFallbackResponse(prompt);
    }

    /**
     * Generate a fallback response based on the prompt
     */
    private String generateFallbackResponse(String prompt) {
        // Simple rule-based responses
        String promptLower = prompt.toLowerCase();

        if (promptLower.contains("author") && promptLower.contains("frank herbert")) {
            List<Book> books = rdfService.getAllBooks().stream()
                    .filter(b -> b.getAuthor().toLowerCase().contains("frank herbert"))
                    .collect(Collectors.toList());
            if (!books.isEmpty()) {
                return "Based on our database, Frank Herbert is the author of: " 
                    + books.stream().map(Book::getTitle).collect(Collectors.joining(", ")) + ".";
            }
        }

        if (promptLower.contains("theme") || promptLower.contains("genre")) {
            List<String> themes = rdfService.getAllBooks().stream()
                    .flatMap(b -> b.getThemes().stream())
                    .distinct()
                    .collect(Collectors.toList());
            return "Available themes in our database: " + String.join(", ", themes) + ".";
        }

        if (promptLower.contains("book") && promptLower.contains("beginner")) {
            List<Book> beginnerBooks = rdfService.getAllBooks().stream()
                    .filter(b -> b.getReadingLevel().equalsIgnoreCase("Beginner"))
                    .collect(Collectors.toList());
            if (!beginnerBooks.isEmpty()) {
                return "Books suitable for beginners: " 
                    + beginnerBooks.stream().map(Book::getTitle).collect(Collectors.joining(", ")) + ".";
            }
        }

        return "I can help you find books! Ask me about authors, themes, reading levels, or specific books in our database.";
    }

    private String escapeJsonString(String input) {
        return input.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String unescapeJsonString(String input) {
        return input.replace("\\\"", "\"").replace("\\n", "\n");
    }
}
