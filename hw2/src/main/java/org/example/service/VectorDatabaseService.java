package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vector database service using TF-IDF embeddings and cosine similarity
 * for semantic search over book metadata.
 */
@Slf4j
@Service
public class VectorDatabaseService {

    @Autowired
    private RdfService rdfService;

    // Each entry: bookUri -> { term -> tfidf weight }
    private final Map<String, Map<String, Double>> bookVectors = new HashMap<>();
    // Raw text documents for context building
    private final Map<String, String> bookDocuments = new HashMap<>();
    // Document frequency: term -> number of documents containing term
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    // Total number of documents
    private int totalDocuments = 0;

    @PostConstruct
    public void init() {
        try {
            buildVectorDatabase();
        } catch (Exception e) {
            log.warn("Could not build vector database at startup (RDF may not be loaded yet)", e);
        }
    }

    /**
     * Build vector database from all books in the RDF model.
     * Creates TF-IDF vectors for each book document.
     */
    public void buildVectorDatabase() {
        List<Book> books = rdfService.getAllBooks();
        bookVectors.clear();
        bookDocuments.clear();
        documentFrequency.clear();

        if (books.isEmpty()) {
            log.warn("No books found to build vector database");
            return;
        }

        // Step 1: Create text documents and compute term frequencies
        Map<String, Map<String, Integer>> termFrequencies = new HashMap<>();

        for (Book book : books) {
            String bookId = book.getUri();
            String document = createBookDocument(book);
            bookDocuments.put(bookId, document);

            // Tokenize and compute term frequency
            List<String> tokens = tokenize(document);
            Map<String, Integer> tf = new HashMap<>();
            for (String token : tokens) {
                tf.merge(token, 1, Integer::sum);
            }
            termFrequencies.put(bookId, tf);

            // Update document frequency
            for (String term : tf.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        totalDocuments = books.size();

        // Step 2: Compute TF-IDF vectors
        for (Map.Entry<String, Map<String, Integer>> entry : termFrequencies.entrySet()) {
            String bookId = entry.getKey();
            Map<String, Integer> tf = entry.getValue();
            Map<String, Double> tfidf = new HashMap<>();

            int maxTf = tf.values().stream().max(Integer::compareTo).orElse(1);

            for (Map.Entry<String, Integer> termEntry : tf.entrySet()) {
                String term = termEntry.getKey();
                double normalizedTf = 0.5 + 0.5 * ((double) termEntry.getValue() / maxTf);
                double idf = Math.log((double) totalDocuments / documentFrequency.getOrDefault(term, 1));
                tfidf.put(term, normalizedTf * idf);
            }

            bookVectors.put(bookId, tfidf);
        }

        log.info("Vector database built with {} books, vocabulary size: {}",
                books.size(), documentFrequency.size());
    }

    /**
     * Create a text representation of a book for embedding.
     */
    private String createBookDocument(Book book) {
        StringBuilder doc = new StringBuilder();
        doc.append("Title: ").append(book.getTitle()).append(". ");
        doc.append("Author: ").append(book.getAuthor()).append(". ");
        doc.append("Themes: ").append(String.join(", ", book.getThemes())).append(". ");
        doc.append("Reading Level: ").append(book.getReadingLevel()).append(". ");
        doc.append("This is a ").append(book.getReadingLevel()).append(" level book ")
           .append("about ").append(String.join(" and ", book.getThemes()))
           .append(" written by ").append(book.getAuthor()).append(".");
        return doc.toString();
    }

    /**
     * Tokenize text into lowercase terms, removing punctuation.
     */
    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+"))
                .filter(s -> !s.isBlank() && s.length() > 1)
                .collect(Collectors.toList());
    }

    /**
     * Compute cosine similarity between two vectors.
     */
    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        Set<String> allTerms = new HashSet<>(v1.keySet());
        allTerms.addAll(v2.keySet());

        for (String term : allTerms) {
            double a = v1.getOrDefault(term, 0.0);
            double b = v2.getOrDefault(term, 0.0);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Convert a query string into a TF-IDF vector using the existing IDF values.
     */
    private Map<String, Double> queryToVector(String query) {
        List<String> tokens = tokenize(query);
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            tf.merge(token, 1, Integer::sum);
        }

        int maxTf = tf.values().stream().max(Integer::compareTo).orElse(1);
        Map<String, Double> vector = new HashMap<>();

        for (Map.Entry<String, Integer> entry : tf.entrySet()) {
            String term = entry.getKey();
            double normalizedTf = 0.5 + 0.5 * ((double) entry.getValue() / maxTf);
            double idf = Math.log((double) Math.max(totalDocuments, 1) / documentFrequency.getOrDefault(term, 1));
            vector.put(term, normalizedTf * idf);
        }

        return vector;
    }

    /**
     * Search for relevant books using vector similarity.
     * Returns books ranked by cosine similarity to the query.
     */
    public List<Book> searchRelevantBooks(String query) {
        if (bookVectors.isEmpty()) {
            buildVectorDatabase();
        }

        Map<String, Double> queryVector = queryToVector(query);

        // Score all books by similarity
        List<Map.Entry<String, Double>> scored = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : bookVectors.entrySet()) {
            double similarity = cosineSimilarity(queryVector, entry.getValue());
            scored.add(Map.entry(entry.getKey(), similarity));
        }

        // Sort by similarity descending
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Return books with similarity > 0, or all books if none match
        List<Book> allBooks = rdfService.getAllBooks();
        Map<String, Book> bookMap = allBooks.stream()
                .collect(Collectors.toMap(Book::getUri, b -> b));

        List<Book> results = scored.stream()
                .filter(e -> e.getValue() > 0.05)
                .map(e -> bookMap.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // If no good matches, return all books as context
        if (results.isEmpty()) {
            return allBooks;
        }

        return results;
    }

    /**
     * Get context string for the chatbot RAG pipeline.
     * Retrieves the most relevant book documents based on query similarity.
     */
    public String getContextForQuery(String query) {
        List<Book> relevantBooks = searchRelevantBooks(query);

        StringBuilder context = new StringBuilder();
        context.append("Relevant books from the database:\n\n");

        for (Book book : relevantBooks) {
            String doc = bookDocuments.get(book.getUri());
            if (doc != null) {
                context.append("- ").append(doc).append("\n");
            } else {
                context.append("- ").append(createBookDocument(book)).append("\n");
            }
        }

        return context.toString();
    }

    /**
     * Get all book documents.
     */
    public Map<String, String> getAllBookDocuments() {
        return new HashMap<>(bookDocuments);
    }
}
