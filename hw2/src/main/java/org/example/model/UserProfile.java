package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String uri;
    private String name;
    private String readingLevel;
    private List<String> preferredThemes;
}