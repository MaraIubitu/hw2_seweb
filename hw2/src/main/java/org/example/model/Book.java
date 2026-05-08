package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    private String uri;
    private String title;
    private String author;
    private List<String> themes;
    private String readingLevel;
}
