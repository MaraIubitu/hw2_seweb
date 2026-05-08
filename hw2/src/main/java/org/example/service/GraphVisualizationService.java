package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class GraphVisualizationService {

    /**
     * Convert RDF model to a graph representation suitable for visualization
     * Returns nodes and edges in a format that can be serialized to JSON
     */
    public Map<String, Object> modelToGraphJson(Model model) {
        Map<String, Object> graph = new HashMap<>();
        
        List<Map<String, String>> nodes = new ArrayList<>();
        List<Map<String, String>> edges = new ArrayList<>();
        Set<String> processedResources = new HashSet<>();

        // Process all statements in the model
        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.next();
            
            Resource subject = stmt.getSubject();
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();

            // Add subject node
            addNodeIfNotProcessed(subject, nodes, processedResources);

            // Add object node if it's a resource
            if (object.isResource()) {
                addNodeIfNotProcessed(object.asResource(), nodes, processedResources);
            } else if (object.isLiteral()) {
                // Create a node for literal values
                String literalNodeId = "literal_" + object.asLiteral().getString().replaceAll("\\s+", "_");
                if (!processedResources.contains(literalNodeId)) {
                    Map<String, String> node = new HashMap<>();
                    node.put("id", literalNodeId);
                    node.put("label", object.asLiteral().getString());
                    node.put("type", "literal");
                    nodes.add(node);
                    processedResources.add(literalNodeId);
                }
            }

            // Add edge
            Map<String, String> edge = new HashMap<>();
            edge.put("source", getNodeId(subject));
            edge.put("target", getNodeId(object));
            edge.put("label", getPredicateLabel(predicate));
            edges.add(edge);
        }

        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    private void addNodeIfNotProcessed(Resource resource, List<Map<String, String>> nodes, Set<String> processed) {
        String nodeId = getNodeId(resource);
        if (!processed.contains(nodeId)) {
            Map<String, String> node = new HashMap<>();
            node.put("id", nodeId);
            node.put("label", getResourceLabel(resource));
            node.put("type", getResourceType(resource));
            nodes.add(node);
            processed.add(nodeId);
        }
    }

    private String getNodeId(RDFNode node) {
        if (node.isResource()) {
            String uri = node.asResource().getURI();
            if (uri != null) {
                return uri;
            }
        }
        return node.toString().replaceAll("\\s+", "_");
    }

    private String getResourceLabel(Resource resource) {
        // Try to get label from RDFS:label
        Statement labelStmt = resource.getProperty(RDFS.label);
        if (labelStmt != null && labelStmt.getObject().isLiteral()) {
            return labelStmt.getString();
        }
        
        // Use the last part of the URI
        String uri = resource.getURI();
        if (uri != null) {
            return extractLocalName(uri);
        }
        
        return resource.toString();
    }

    private String getResourceType(Resource resource) {
        Statement typeStmt = resource.getProperty(RDF.type);
        if (typeStmt != null && typeStmt.getObject().isResource()) {
            String typeUri = typeStmt.getResource().getURI();
            if (typeUri != null) {
                return extractLocalName(typeUri);
            }
        }
        return "resource";
    }

    private String getPredicateLabel(Property predicate) {
        String uri = predicate.getURI();
        if (uri != null) {
            return extractLocalName(uri);
        }
        return predicate.toString();
    }

    private String extractLocalName(String uri) {
        int hashIndex = uri.lastIndexOf('#');
        int slashIndex = uri.lastIndexOf('/');
        int index = Math.max(hashIndex, slashIndex);
        return index >= 0 && index + 1 < uri.length() ? uri.substring(index + 1) : uri;
    }

    /**
     * Get statistics about the model
     */
    public Map<String, Object> getModelStatistics(Model model) {
        Map<String, Object> stats = new HashMap<>();
        
        int statementCount = model.listStatements().toList().size();
        int resourceCount = model.listSubjects().toList().size();
        int propertyCount = (int) model.listStatements().toList().stream()
            .map(Statement::getPredicate)
            .map(Property::getURI)
            .filter(Objects::nonNull)
            .distinct()
            .count();

        stats.put("statements", statementCount);
        stats.put("resources", resourceCount);
        stats.put("properties", propertyCount);

        return stats;
    }
}
