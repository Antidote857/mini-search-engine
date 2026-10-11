package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Document;
import model.SearchResult;

public class SearchEngine {

    private final SearchIndexer indexer;
    private final TextProcessor textProcessor = new TextProcessor();

    public SearchEngine(SearchIndexer indexer) {
        if (indexer == null) {
            throw new IllegalArgumentException(
                    "Search indexer cannot be null."
            );
        }

        this.indexer = indexer;
    }

    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Please enter a search query."
            );
        }

        Set<String> queryTerms = new LinkedHashSet<>(
                textProcessor.process(query)
        );

        if (queryTerms.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please enter at least one searchable word."
            );
        }

        Map<Integer, Integer> scores = new HashMap<>();

        Map<Integer, List<String>> matchedTermsByDocument =
                new HashMap<>();

        for (String term : queryTerms) {
            Map<Integer, Integer> postings = indexer.getPostings(term);

            for (Map.Entry<Integer, Integer> entry
                    : postings.entrySet()) {

                int documentId = entry.getKey();
                int frequency = entry.getValue();

                scores.merge(documentId, frequency, Integer::sum);

                matchedTermsByDocument
                        .computeIfAbsent(
                                documentId,
                                key -> new ArrayList<>()
                        )
                        .add(term);
            }
        }

        List<SearchResult> results = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : scores.entrySet()) {
            int documentId = entry.getKey();
            Document document = indexer.getDocument(documentId);

            results.add(
                    new SearchResult(
                            document,
                            entry.getValue(),
                            matchedTermsByDocument.get(documentId)
                    )
            );
        }

        results.sort(
                Comparator.comparingInt(SearchResult::getScore)
                        .reversed()
                        .thenComparing(
                                result -> result.getDocument().getFilename()
                        )
                        .thenComparingInt(
                                result -> result.getDocument().getId()
                        )
        );

        return List.copyOf(results);
    }
}