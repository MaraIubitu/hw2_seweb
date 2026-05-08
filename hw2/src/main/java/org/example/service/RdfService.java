package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.example.model.Book;
import org.example.model.UserProfile;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;

@Slf4j
@Service
public class RdfService {

    private static final String BOOK_NS = "http://example.org/book/";
    private Model model;

    public RdfService() {
        this.model = ModelFactory.createDefaultModel();
    }

    @PostConstruct
    public void initializeDefaultRdf() {
        File rootFile = new File("books.rdf");
        try {
            if (rootFile.exists()) {
                loadRdfFromFile(rootFile.getAbsolutePath());
                return;
            }

            InputStream classpathRdf = getClass().getClassLoader().getResourceAsStream("books.rdf");
            if (classpathRdf != null) {
                try (InputStream is = classpathRdf) {
                    loadRdfFromInputStream(is);
                }
            }
        } catch (IOException e) {
            log.warn("Could not initialize RDF model at startup", e);
        }
    }

    /**
     * Load RDF from file
     */
    public void loadRdfFromFile(String filePath) throws IOException {
        this.model = ModelFactory.createDefaultModel();
        try (InputStream is = new FileInputStream(filePath)) {
            model.read(is, null, "RDF/XML");
            log.info("RDF file loaded successfully from: {}", filePath);
        }
    }

    /**
     * Load RDF from InputStream (for file uploads)
     */
    public void loadRdfFromInputStream(InputStream is) throws IOException {
        this.model = ModelFactory.createDefaultModel();
        model.read(is, null, "RDF/XML");
        log.info("RDF loaded from input stream");
    }

    /**
     * Save RDF to file
     */
    public void saveRdfToFile(String filePath) throws IOException {
        try (OutputStream os = new FileOutputStream(filePath)) {
            model.write(os, "RDF/XML");
            log.info("RDF saved to: {}", filePath);
        }
    }

    /**
     * Get all books from the model
     */
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        Resource bookTypeRes = model.createResource(BOOK_NS + "Book");
        
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, bookTypeRes);
        while (iter.hasNext()) {
            Resource bookRes = iter.next();
            books.add(resourceToBook(bookRes));
        }
        return books;
    }

    /**
     * Get all users from the model.
     */
    public List<UserProfile> getAllUsers() {
        List<UserProfile> users = new ArrayList<>();
        Resource userTypeRes = model.createResource(BOOK_NS + "User");

        ResIterator iter = model.listSubjectsWithProperty(RDF.type, userTypeRes);
        while (iter.hasNext()) {
            UserProfile user = resourceToUser(iter.next());
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    /**
     * Get a user by URI.
     */
    public UserProfile getUserByUri(String uri) {
        Resource userRes = model.getResource(uri);
        if (userRes.hasProperty(RDF.type)) {
            return resourceToUser(userRes);
        }
        return null;
    }

    /**
     * Recommend books for a user by matching preferred theme and reading level.
     */
    public List<Book> getRecommendedBooksForUser(String userUri) {
        UserProfile user = getUserByUri(userUri);
        if (user == null) {
            return List.of();
        }

        String preferredTheme = user.getPreferredThemes().isEmpty() ? null : user.getPreferredThemes().get(0);
        return getAllBooks().stream()
                .filter(book -> book.getReadingLevel() != null && user.getReadingLevel() != null
                        && book.getReadingLevel().equalsIgnoreCase(user.getReadingLevel()))
                .filter(book -> preferredTheme == null
                        || book.getThemes().stream().anyMatch(theme -> theme.equalsIgnoreCase(preferredTheme)))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get a specific book by URI
     */
    public Book getBookByUri(String uri) {
        Resource bookRes = model.getResource(uri);
        if (bookRes.hasProperty(RDF.type)) {
            return resourceToBook(bookRes);
        }
        return null;
    }

    /**
     * Add a new book to the model
     */
    public void addBook(String title, String author, List<String> themes, String readingLevel) {
        String bookUri = BOOK_NS + title.toLowerCase().replaceAll("\\s+", "");
        Resource bookRes = model.createResource(bookUri);
        Resource bookType = model.createResource(BOOK_NS + "Book");
        
        bookRes.addProperty(RDF.type, bookType);
        bookRes.addProperty(RDFS.label, title);
        bookRes.addProperty(model.createProperty(BOOK_NS + "author"), author);
        
        for (String theme : themes) {
            Resource themeRes = model.createResource(BOOK_NS + theme.replaceAll("\\s+", ""));
            bookRes.addProperty(model.createProperty(BOOK_NS + "hasTheme"), themeRes);
        }
        
        Resource readingLevelRes = model.createResource(BOOK_NS + readingLevel);
        bookRes.addProperty(model.createProperty(BOOK_NS + "hasReadingLevel"), readingLevelRes);
        
        log.info("Book added: {}", title);
    }

    /**
     * Update a book
     */
    public void updateBook(String bookUri, String title, String author, List<String> themes, String readingLevel) {
        Resource bookRes = model.getResource(bookUri);
        if (bookRes == null) {
            log.warn("Book not found: {}", bookUri);
            return;
        }

        // Remove old properties
        bookRes.removeAll(model.createProperty(BOOK_NS + "hasTheme"));
        bookRes.removeAll(model.createProperty(BOOK_NS + "hasReadingLevel"));

        // Update label
        if (title != null) {
            bookRes.removeAll(RDFS.label);
            bookRes.addProperty(RDFS.label, title);
        }

        // Update author
        if (author != null) {
            bookRes.removeAll(model.createProperty(BOOK_NS + "author"));
            bookRes.addProperty(model.createProperty(BOOK_NS + "author"), author);
        }

        // Update themes
        if (themes != null) {
            for (String theme : themes) {
                Resource themeRes = model.createResource(BOOK_NS + theme.replaceAll("\\s+", ""));
                bookRes.addProperty(model.createProperty(BOOK_NS + "hasTheme"), themeRes);
            }
        }

        // Update reading level
        if (readingLevel != null) {
            Resource readingLevelRes = model.createResource(BOOK_NS + readingLevel);
            bookRes.addProperty(model.createProperty(BOOK_NS + "hasReadingLevel"), readingLevelRes);
        }

        log.info("Book updated: {}", bookUri);
    }

    /**
     * Delete a book
     */
    public void deleteBook(String bookUri) {
        Resource bookRes = model.getResource(bookUri);
        bookRes.removeProperties();
        log.info("Book deleted: {}", bookUri);
    }

    /**
     * Get the RDF model as XML string
     */
    public String getRdfAsXml() {
        StringWriter sw = new StringWriter();
        model.write(sw, "RDF/XML");
        return sw.toString();
    }

    /**
     * Get the RDF model as N-Triples string (for easier parsing)
     */
    public String getRdfAsNTriples() {
        StringWriter sw = new StringWriter();
        model.write(sw, "N-TRIPLES");
        return sw.toString();
    }

    /**
     * Get model for graph visualization
     */
    public Model getModel() {
        return model;
    }

    /**
     * Convert a Resource to a Book object
     */
    private Book resourceToBook(Resource resource) {
        Book book = new Book();
        book.setUri(resource.getURI());
        
        // Get title
        Statement titleStmt = resource.getProperty(RDFS.label);
        if (titleStmt != null) {
            book.setTitle(titleStmt.getString());
        }

        // Get author
        Statement authorStmt = resource.getProperty(model.createProperty(BOOK_NS + "author"));
        if (authorStmt != null) {
            book.setAuthor(authorStmt.getString());
        }

        // Get themes
        List<String> themes = new ArrayList<>();
        StmtIterator themeIter = resource.listProperties(model.createProperty(BOOK_NS + "hasTheme"));
        while (themeIter.hasNext()) {
            Statement stmt = themeIter.next();
            Resource themeRes = stmt.getResource();
            Statement themeLabelStmt = themeRes.getProperty(RDFS.label);
            if (themeLabelStmt != null) {
                themes.add(themeLabelStmt.getString());
            } else {
                themes.add(themeRes.getURI().substring(themeRes.getURI().lastIndexOf("/") + 1));
            }
        }
        book.setThemes(themes);

        // Get reading level
        Statement readingLevelStmt = resource.getProperty(model.createProperty(BOOK_NS + "hasReadingLevel"));
        if (readingLevelStmt != null) {
            Resource levelRes = readingLevelStmt.getResource();
            Statement levelLabelStmt = levelRes.getProperty(RDFS.label);
            if (levelLabelStmt != null) {
                book.setReadingLevel(levelLabelStmt.getString());
            } else {
                book.setReadingLevel(levelRes.getURI().substring(levelRes.getURI().lastIndexOf("/") + 1));
            }
        }

        return book;
    }

    /**
     * Convert a Resource to a UserProfile object.
     */
    private UserProfile resourceToUser(Resource resource) {
        UserProfile user = new UserProfile();
        user.setUri(resource.getURI());

        Statement labelStmt = resource.getProperty(RDFS.label);
        if (labelStmt != null) {
            user.setName(labelStmt.getString());
        }

        Statement readingLevelStmt = resource.getProperty(model.createProperty(BOOK_NS + "hasReadingLevel"));
        if (readingLevelStmt != null && readingLevelStmt.getObject().isResource()) {
            Resource levelRes = readingLevelStmt.getResource();
            Statement levelLabelStmt = levelRes.getProperty(RDFS.label);
            user.setReadingLevel(levelLabelStmt != null ? levelLabelStmt.getString() : extractLocalName(levelRes.getURI()));
        }

        List<String> preferredThemes = new ArrayList<>();
        StmtIterator themeIter = resource.listProperties(model.createProperty(BOOK_NS + "prefers"));
        while (themeIter.hasNext()) {
            Statement stmt = themeIter.next();
            if (stmt.getObject().isResource()) {
                Resource themeRes = stmt.getResource();
                Statement themeLabelStmt = themeRes.getProperty(RDFS.label);
                preferredThemes.add(themeLabelStmt != null ? themeLabelStmt.getString() : extractLocalName(themeRes.getURI()));
            }
        }
        user.setPreferredThemes(preferredThemes);

        return user;
    }

    private String extractLocalName(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        int hashIndex = uri.lastIndexOf('#');
        int slashIndex = uri.lastIndexOf('/');
        int index = Math.max(hashIndex, slashIndex);
        return index >= 0 && index + 1 < uri.length() ? uri.substring(index + 1) : uri;
    }
}
