package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Book;
import org.example.service.RdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class BookApiController {

    @Autowired
    private RdfService rdfService;

    @GetMapping("/books")
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = rdfService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/books/search")
    public ResponseEntity<List<Book>> searchBooks(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String theme) {
        
        List<Book> allBooks = rdfService.getAllBooks();
        
        List<Book> filtered = allBooks.stream()
                .filter(book -> author == null || book.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .filter(book -> theme == null || book.getThemes().stream()
                        .anyMatch(t -> t.toLowerCase().contains(theme.toLowerCase())))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/book/{id}")
    public ResponseEntity<Book> getBook(@PathVariable String id) {
        String bookUri = "http://example.org/book/" + id;
        Book book = rdfService.getBookByUri(bookUri);
        
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(book);
    }

    @PostMapping("/book")
    public ResponseEntity<Map<String, String>> createBook(@RequestBody Book book) {
        try {
            rdfService.addBook(book.getTitle(), book.getAuthor(), book.getThemes(), book.getReadingLevel());
            rdfService.saveRdfToFile("books.rdf");
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Book created successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating book", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/book/{id}")
    public ResponseEntity<Map<String, String>> updateBook(
            @PathVariable String id,
            @RequestBody Book book) {
        try {
            String bookUri = "http://example.org/book/" + id;
            rdfService.updateBook(bookUri, book.getTitle(), book.getAuthor(), book.getThemes(), book.getReadingLevel());
            rdfService.saveRdfToFile("books.rdf");
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Book updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating book", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/book/{id}")
    public ResponseEntity<Map<String, String>> deleteBook(@PathVariable String id) {
        try {
            String bookUri = "http://example.org/book/" + id;
            rdfService.deleteBook(bookUri);
            rdfService.saveRdfToFile("books.rdf");
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Book deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting book", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/books/themes")
    public ResponseEntity<List<String>> getAllThemes() {
        List<Book> books = rdfService.getAllBooks();
        List<String> themes = books.stream()
                .flatMap(book -> book.getThemes().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(themes);
    }

    @GetMapping("/books/reading-levels")
    public ResponseEntity<List<String>> getAllReadingLevels() {
        List<Book> books = rdfService.getAllBooks();
        List<String> levels = books.stream()
                .map(Book::getReadingLevel)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(levels);
    }
}
