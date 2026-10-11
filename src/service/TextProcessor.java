package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import util.StopWords;

public class TextProcessor {

    public List<String> process(String text) {
        if (text == null) {
            throw new IllegalArgumentException(
                    "Text cannot be null."
            );
        }

        String normalized = text
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();

        List<String> tokens = new ArrayList<>();

        if (normalized.isEmpty()) {
            return tokens;
        }

        String[] words = normalized.split("\\s+");

        for (String word : words) {
            if (!StopWords.contains(word)) {
                tokens.add(word);
            }
        }

        return tokens;
    }
}