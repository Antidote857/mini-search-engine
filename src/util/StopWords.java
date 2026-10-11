package util;

import java.util.Set;

public final class StopWords {

    private static final Set<String> WORDS = Set.of(
            "a", "an", "the",
            "is", "are", "was", "were", "be", "been",
            "of", "to", "in", "on", "at",
            "and", "or", "for", "from", "with",
            "this", "that", "these", "those",
            "it", "its", "as", "by"
    );

    private StopWords() {
        // This utility class does not need objects.
    }

    public static boolean contains(String word) {
        return WORDS.contains(word);
    }
}