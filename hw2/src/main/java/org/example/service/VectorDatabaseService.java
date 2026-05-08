package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VectorDatabaseService {

    @Autowired
    private RdfService rdfService;

    private Map<String, String> bookDocuments = new HashMap<>();

    public VectorDatabaseService() {
    }

    /**
     * Build vector database from all books
     */
    public void buildVectorDatabase() {
        List<Book> books = rdfService.getAllBooks();
        bookDocuments.clear();

        for (Book book : books) {
            String bookId = book.getUri().substring(book.getUri().lastIndexOf("/") + 1);
            String documentText = createBookDocument(book);
            bookDocuments.put(bookId, documentText);
            
            // In a real implementation, you would embed and store this document
            log.info("Added book to vector database: {}", book.getTitle());
        }
    }

    /**
     * Create a text representation of a book for embedding
     */
    private String createBookDocument(Book book) {
        StringBuilder doc = new StringBuilder();
        doc.append("Title: ").append(book.getTitle()).append("\n");
        doc.append("Author: ").append(book.getAuthor()).append("\n");
        doc.append("Themes: ").append(String.join(", ", book.getThemes())).append("\n");
        doc.append("Reading Level: ").append(book.getReadingLevel()).append("\n");
        
        // Add context-aware information
        doc.append("This is a ").append(book.getReadingLevel()).append(" level book ")
           .append("about ").append(String.join(" and ", book.getThemes()))
           .append(" written by ").append(book.getAuthor()).append(".");
        
        return doc.toString();
    }

    /**
     * Search for relevant books based on a query
     */
    public List<Book> searchRelevantBooks(String query) {
        // Simple keyword-based search
        List<Book> allBooks = rdfService.getAllBooks();
        
        String queryLower = query.toLowerCase();
        
        return allBooks.stream()
                .filter(book -> 
                    book.getTitle().toLowerCase().contains(queryLower) ||
                    book.getAuthor().toLowerCase().contains(queryLower) ||
                    book.getThemes().stream().anyMatch(t -> t.toLowerCase().contains(queryLower)) ||
                    book.getReadingLevel().toLowerCase().contains(queryLower)
                )
                .collect(Collectors.toList());
    }

    /**
     * Get all books with their text representations
     */
    public Map<String, String> getAllBookDocuments() {
        return new HashMap<>(bookDocuments);
    }

    /**
     * Get a specific book document
     */
    public String getBookDocument(String bookId) {
        return bookDocuments.get(bookId);
    }

    /**
     * Get context for chatbot responses
     */
    public String getContextForQuery(String query) {
        List<Book> relevantBooks = searchRelevantBooks(query);
        
        StringBuilder context = new StringBuilder();
        context.append("Available books:\n");
        
        for (Book book : relevantBooks) {
            context.append("- ").append(createBookDocument(book)).append("\n");
        }
        
        return context.toString();
    }
}
