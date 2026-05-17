package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Book;
import org.example.model.UserProfile;
import org.example.service.GraphVisualizationService;
import org.example.service.RdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/")
public class BookController {

    @Autowired
    private RdfService rdfService;

    @Autowired
    private GraphVisualizationService graphVisualizationService;

    @GetMapping
    public String index(Model model, HttpSession session) {
        try {
            String rdfPath = "books.rdf";
            rdfService.loadRdfFromFile(rdfPath);
            List<Book> books = rdfService.getAllBooks();
            List<UserProfile> users = rdfService.getAllUsers();

            String currentUserUri = (String) session.getAttribute("currentUserUri");
            if (currentUserUri == null && !users.isEmpty()) {
                currentUserUri = users.get(0).getUri();
                session.setAttribute("currentUserUri", currentUserUri);
            }

            UserProfile currentUser = currentUserUri != null ? rdfService.getUserByUri(currentUserUri) : null;
            List<Book> recommendations = currentUserUri != null
                    ? rdfService.getRecommendedBooksForUser(currentUserUri)
                    : List.of();

            model.addAttribute("books", books);
            model.addAttribute("bookCount", books.size());
            model.addAttribute("users", users);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("recommendations", recommendations);
        } catch (IOException e) {
            log.error("Error loading RDF file", e);
            model.addAttribute("error", "Error loading RDF file");
        }
        return "index";
    }

    @GetMapping("/books")
    public String listBooks(Model model) {
        List<Book> books = rdfService.getAllBooks();
        model.addAttribute("books", books);
        model.addAttribute("bookCount", books.size());
        return "books";
    }

    @GetMapping("/book/{id}")
    public String viewBook(@PathVariable String id, Model model) {
        String bookUri = "http://example.org/book/" + id;
        Book book = rdfService.getBookByUri(bookUri);

        if (book == null) {
            model.addAttribute("error", "Book not found");
            return "error";
        }

        model.addAttribute("book", book);

        // Find similar books (share at least one theme, excluding the current book)
        List<Book> similarBooks = rdfService.getAllBooks().stream()
                .filter(b -> !b.getUri().equals(bookUri))
                .filter(b -> b.getThemes().stream()
                        .anyMatch(t -> book.getThemes().contains(t)))
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("similarBooks", similarBooks);

        return "book-detail";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            rdfService.loadRdfFromInputStream(file.getInputStream());
            model.addAttribute("message", "RDF file uploaded successfully!");
            model.addAttribute("statistics", graphVisualizationService.getModelStatistics(rdfService.getModel()));
            return "upload-success";
        } catch (IOException e) {
            log.error("Error uploading RDF file", e);
            model.addAttribute("error", "Error uploading file: " + e.getMessage());
            return "upload";
        }
    }

    @GetMapping("/visualize")
    public String visualizeGraph(Model model) {
        try {
            Map<String, Object> graph = graphVisualizationService.modelToGraphJson(rdfService.getModel());
            model.addAttribute("graphJson", new com.google.gson.Gson().toJson(graph));
            model.addAttribute("statistics", graphVisualizationService.getModelStatistics(rdfService.getModel()));
            return "visualization";
        } catch (Exception e) {
            log.error("Error visualizing graph", e);
            model.addAttribute("error", "Error visualizing graph: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/manage")
    public String managePage(Model model) {
        List<Book> books = rdfService.getAllBooks();
        model.addAttribute("books", books);
        return "manage";
    }

    @PostMapping("/book/add")
    public String addBook(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String themes,
            @RequestParam String readingLevel,
            Model model) {
        try {
            List<String> themeList = List.of(themes.split(","));
            rdfService.addBook(title, author, themeList, readingLevel);
            rdfService.saveRdfToFile("books.rdf");
            model.addAttribute("message", "Book added successfully!");
        } catch (Exception e) {
            log.error("Error adding book", e);
            model.addAttribute("error", "Error adding book: " + e.getMessage());
        }
        return "redirect:/manage";
    }

    @PostMapping("/book/{id}/update")
    public String updateBook(
            @PathVariable String id,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String themes,
            @RequestParam String readingLevel,
            Model model) {
        try {
            String bookUri = "http://example.org/book/" + id;
            List<String> themeList = List.of(themes.split(","));
            rdfService.updateBook(bookUri, title, author, themeList, readingLevel);
            rdfService.saveRdfToFile("books.rdf");
            model.addAttribute("message", "Book updated successfully!");
        } catch (Exception e) {
            log.error("Error updating book", e);
            model.addAttribute("error", "Error updating book: " + e.getMessage());
        }
        return "redirect:/manage";
    }

    @PostMapping("/book/{id}/delete")
    public String deleteBook(@PathVariable String id, Model model) {
        try {
            String bookUri = "http://example.org/book/" + id;
            rdfService.deleteBook(bookUri);
            rdfService.saveRdfToFile("books.rdf");
            model.addAttribute("message", "Book deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting book", e);
            model.addAttribute("error", "Error deleting book: " + e.getMessage());
        }
        return "redirect:/manage";
    }
}
