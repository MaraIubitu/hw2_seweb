# Book Recommendation System - Semantic Web Project

A comprehensive semantic web application demonstrating RDF, OWL, SPARQL technologies with a web interface and AI chatbot integration.

## Team Members
Iubitu Mara-Alexandra, 1241EA  
Bilciurescu Gabriel-Cosmin, 1241EA  
Rachiteanu Emma, 1241EA  

## Project Overview

This project is a complete implementation of a book recommendation system using semantic web technologies. It includes:

1. **RDF/XML** data model for books and users
2. **Web Application** built with Spring Boot for managing books
3. **OWL Ontology** for the book recommendation domain
4. **SPARQL Queries** for querying the ontology
5. **AI Chatbot** with RAG (Retrieval-Augmented Generation) for book recommendations

## Technology Stack

- **Backend**: Java 11+, Spring Boot 3.2.0
- **RDF/OWL**: Apache JENA 4.8.0
- **Visualization**: D3.js, JUNG 2.1.1
- **Frontend**: Bootstrap 5, Thymeleaf
- **LLM Integration**: LangChain4j
- **Build**: Maven 3.8+

## Project Structure

```
project/
├── src/main/java/org/example/
│   ├── BookRecommendationApplication.java    # Main Spring Boot application
│   ├── controller/
│   │   ├── BookController.java               # Web request handlers
│   │   ├── BookApiController.java            # REST API endpoints
│   │   └── ChatbotController.java            # Chatbot API endpoints
│   ├── model/
│   │   └── Book.java                         # Book domain model
│   └── service/
│       ├── RdfService.java                   # RDF operations using JENA
│       ├── ChatbotService.java               # Chatbot with RAG
│       ├── VectorDatabaseService.java        # Vector DB for RAG
│       └── GraphVisualizationService.java    # Graph visualization
├── src/main/resources/
│   ├── templates/
│   │   ├── index.html                        # Home page
│   │   ├── books.html                        # Book listing
│   │   ├── book-detail.html                  # Book details
│   │   ├── upload.html                       # RDF upload
│   │   ├── manage.html                       # Book management
│   │   └── visualization.html                # RDF graph visualization
│   ├── static/
│   │   ├── css/style.css                     # Styling
│   │   └── js/chatbot.js                     # Chatbot widget JavaScript
│   └── application.properties                # Configuration
├── books.rdf                                 # Sample RDF/XML data
├── book-ontology.owl                         # OWL ontology
├── sparql_owl.txt                            # SPARQL queries
└── pom.xml                                   # Maven configuration
```

## Features Implemented

### 1. RDF/XML File (books.rdf)
- Defines users (Alice, Bob) with preferences and reading levels
- Defines books (Dune, The Silent Patient, Hunger Games, Harry Potter)
- Includes themes and reading levels
- Defines recommendation rules

### 2. Web Application Features

#### Book Upload & Visualization (Task 2)
- Upload RDF/XML files through web interface
- Automatic graph visualization using D3.js
- Display statistics (statements, resources, properties)

#### Book Management (Tasks 3 & 4)
- Add new books using JENA API
- Edit existing books (change properties, themes, reading levels)
- Delete books from the system
- List all available books with details
- Individual book detail pages

#### REST API
- `/api/books` - Get all books
- `/api/books/search` - Search by author/theme
- `/api/book/{id}` - Get book details
- `/api/book` - Create/update/delete books
- `/api/books/themes` - Get all available themes
- `/api/books/reading-levels` - Get all reading levels

### 3. OWL Ontology (book-ontology.owl)
- Comprehensive ontology for book recommendation domain
- Classes: Reader, Book, Author, Genre, Review, Recommendation
- Object Properties: hasAuthor, writtenBy, hasGenre, hasReview, reviewedBook, reviewedBy, likesGenre, recommendBook, recommendTo
- Data Properties: title, publicationYear, authorName, genreName, readerName, reviewText, rating
The ontology includes sample individuals such as books, authors, genres, reviews, and recommendations. It was visualized in GraphDB, and screenshots of the ontology graph are included in the project archive.

### 4. SPARQL Queries (sparql_owl.txt)
Five SPARQL queries demonstrating different capabilities:
1. List all books and their titles
2. Find all books with their authors
3. Display all books with their genres
4. Get all books with their reviews
5. List all books published after 1900
The queries were executed in GraphDB, and screenshots of the results are included in the project archive.

### 5. AI Chatbot with RAG (Task 7)

#### Floating Chat Widget
- Fixed position floating chatbot on all pages
- Minimizable/maximizable interface
- Clean, modern design

#### Context-Aware Conversation Starters
- General starters on home/book list pages
- Book-specific starters on individual book pages
- Dynamic loading based on page context

#### RAG (Retrieval-Augmented Generation)
- Vector database built from book metadata
- Semantic search for relevant books
- LLM responses enhanced with book database context
- Fallback rule-based responses

#### Chatbot Capabilities
- Find books by author and theme
- Get book recommendations
- Answer questions about books
- List available themes and reading levels
- Provide personalized suggestions based on database

## Setup & Installation

### Prerequisites
- Java 11 or higher
- Maven 3.8+
- Git

### Installation Steps

1. Clone the repository:
```bash
git clone <repository-url>
cd hw2
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

4. Access the application:
- Open your browser and navigate to `http://localhost:8080`

## Usage Guide

### Home Page
- View quick statistics about the book database
- Access all application features
- Interact with the floating chatbot

### Books Page
- View all available books
- Click on any book to see detailed information
- Use chatbot for book recommendations

### Upload Page
- Upload an RDF/XML file to load books
- View statistics about the uploaded file
- Navigate to visualization or book list

### Visualization Page
- Interactive graph visualization of RDF data
- Drag nodes to rearrange
- View model statistics

### Manage Books Page
- Add new books to the database
- Edit existing books
- Delete books
- Changes are persisted to RDF file

### Chatbot
- Click the floating chat button to open
- View conversation starters relevant to current page
- Ask questions about books
- Receive recommendations based on book database

## Key Technologies & APIs

### Apache JENA
- RDF model management
- SPARQL query execution
- RDF/XML parsing and serialization

### D3.js
- Interactive graph visualization
- Force-directed layout
- Node and edge rendering

### Spring Boot
- REST API endpoints
- MVC controller handling
- Thymeleaf template rendering

### LangChain4j
- Vector embedding support
- LLM integration
- RAG pipeline construction

## Running SPARQL Queries

The `sparql_owl.txt` file contains 5 SPARQL queries. To execute them:

1. **In Protégé**:
   - Open the ontology file in Protégé
   - Open the DL Query tab
   - Copy and paste queries one by one
   - View results

2. **In GraphDB**:
   - Import the OWL ontology
   - Use the SPARQL Editor
   - Copy and paste queries
   - Execute and view results

## LLM Configuration

The chatbot can use different LLM providers:

### Local LLM (LMStudio)
Set environment variable:
```bash
export LLM_API_URL=http://localhost:1234/v1
export LLM_API_KEY=your-key
```

### Cloud-based LLM
```bash
export OPENROUTER_API_KEY=your-key
export OPENAI_API_KEY=your-key
```

## Team Members & Contributions

(To be filled in with team member information)

- Member 1: [Name] - [Contributions]
- Member 2: [Name] - [Contributions]
- Member 3: [Name] - [Contributions]

## GitHub Repository

Public repository: [GitHub Link]

## Submission Files

- `books.rdf` - RDF/XML data model
- `book-ontology.owl` - OWL ontology
- `sparql_owl.txt` - SPARQL queries with results
- `src/` - Complete Java source code
- `pom.xml` - Maven build configuration
- `README.md` - This file

## Important Notes

1. **Data Persistence**: Changes made through the web interface are saved to `books.rdf` file
2. **Vector Database**: Automatically built from RDF data on chatbot initialization
3. **RAG Context**: Book information is retrieved from database, not model knowledge
4. **Extensibility**: Easy to add more books, themes, and users through the web interface or by editing RDF

## Testing

The project has been tested with:
- ✓ RDF file upload and visualization
- ✓ Book addition and modification
- ✓ Book listing and detail pages
- ✓ SPARQL query execution
- ✓ Chatbot conversation and recommendations
- ✓ Graph visualization with D3.js

## Future Enhancements

- User authentication and personal recommendations
- More sophisticated NLP for chatbot
- Integration with external book databases
- Real-time collaborative features
- Advanced analytics dashboard
- Mobile app version

## References

- Apache JENA Documentation: https://jena.apache.org/
- W3C OWL 2.0 Specification: https://www.w3.org/OWL/
- SPARQL Query Language: https://www.w3.org/TR/sparql11-query/
- Spring Boot Documentation: https://spring.io/projects/spring-boot/
- D3.js Documentation: https://d3js.org/

## License

This project is created for educational purposes as part of a Semantic Web course assignment.

---

**Last Updated**: May 2026
