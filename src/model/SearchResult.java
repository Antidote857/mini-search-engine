package model;

import java.util.List;

public class SearchResult {

    private final Document document;
    private final int score;
    private final List<String> matchedTerms;

    public SearchResult(
            Document document,
            int score,
            List<String> matchedTerms) {

        if (document == null) {
            throw new IllegalArgumentException(
                    "Document cannot be null."
            );
        }

        if (score <= 0) {
            throw new IllegalArgumentException(
                    "Search result score must be positive."
            );
        }

        if (matchedTerms == null || matchedTerms.isEmpty()) {
            throw new IllegalArgumentException(
                    "Matched terms cannot be empty."
            );
        }

        this.document = document;
        this.score = score;
        this.matchedTerms = List.copyOf(matchedTerms);
    }

    public Document getDocument() {
        return document;
    }

    public int getScore() {
        return score;
    }

    public List<String> getMatchedTerms() {
        return matchedTerms;
    }
}