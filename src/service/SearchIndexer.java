package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import model.Document;

public class SearchIndexer {

    private final TextProcessor textProcessor = new TextProcessor();

    private Map<Integer, Document> documentsById = new HashMap<>();

    private Map<String, Map<Integer, Integer>> invertedIndex =
            new HashMap<>();

    public void buildIndex(List<Document> documents) {
        if (documents == null) {
            throw new IllegalArgumentException(
                    "Document list cannot be null."
            );
        }

        Map<Integer, Document> newDocuments = new HashMap<>();

        Map<String, Map<Integer, Integer>> newIndex = new HashMap<>();

        for (Document document : documents) {
            if (document == null) {
                throw new IllegalArgumentException(
                        "Document list cannot contain null."
                );
            }

            if (newDocuments.containsKey(document.getId())) {
                throw new IllegalArgumentException(
                        "Duplicate document ID: " + document.getId()
                );
            }

            newDocuments.put(document.getId(), document);

            List<String> tokens = textProcessor.process(
                    document.getContent()
            );

            for (String term : tokens) {
                Map<Integer, Integer> frequencies =
                        newIndex.computeIfAbsent(
                                term,
                                key -> new HashMap<>()
                        );

                frequencies.merge(document.getId(), 1, Integer::sum);
            }
        }

        documentsById = newDocuments;
        invertedIndex = newIndex;
    }

    public Map<Integer, Integer> getPostings(String term) {
        if (term == null) {
            throw new IllegalArgumentException(
                    "Search term cannot be null."
            );
        }

        Map<Integer, Integer> frequencies = invertedIndex.get(term);

        if (frequencies == null) {
            return Map.of();
        }

        return Collections.unmodifiableMap(
                new TreeMap<>(frequencies)
        );
    }

    public Document getDocument(int documentId) {
        return documentsById.get(documentId);
    }

    public List<Document> getDocuments() {
        List<Document> documents =
                new ArrayList<>(documentsById.values());

        documents.sort(
                java.util.Comparator.comparingInt(Document::getId)
        );

        return List.copyOf(documents);
    }

    public List<String> getIndexedTerms() {
        List<String> terms = new ArrayList<>(invertedIndex.keySet());

        Collections.sort(terms);

        return List.copyOf(terms);
    }

    public int getDocumentCount() {
        return documentsById.size();
    }

    public int getTermCount() {
        return invertedIndex.size();
    }
}