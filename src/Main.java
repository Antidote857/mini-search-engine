import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import model.Document;
import model.SearchResult;
import service.DocumentLoader;
import service.SearchEngine;
import service.SearchIndexer;

public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("           MINI SEARCH ENGINE");
        System.out.println("========================================");

        DocumentLoader loader = new DocumentLoader();
        SearchIndexer indexer = new SearchIndexer();

        try {
            List<Document> documents = loader.loadDocuments(
                    Path.of("documents")
            );

            if (documents.isEmpty()) {
                System.out.println("\nNo text documents were loaded.");
                return;
            }

            indexer.buildIndex(documents);

            SearchEngine engine = new SearchEngine(indexer);

            System.out.println(
                    "\nDocuments indexed: " + indexer.getDocumentCount()
            );

            displaySearch(engine, "java collections");
            displaySearch(engine, "JAVA");
            displaySearch(engine, "java java");
            displaySearch(engine, "blockchain");
            displaySearch(engine, "");
            displaySearch(engine, "the and is");

        } catch (IOException exception) {
            System.out.println(
                    "\nUnable to load documents: "
                            + exception.getMessage()
            );
        }
    }

    private static void displaySearch(
            SearchEngine engine,
            String query) {

        System.out.println("\nSearch query: \"" + query + "\"");

        try {
            List<SearchResult> results = engine.search(query);

            if (results.isEmpty()) {
                System.out.println("No matching documents found.");
                return;
            }

            int position = 1;

            for (SearchResult result : results) {
                System.out.println(
                        position + ". "
                                + result.getDocument().getFilename()
                );

                System.out.println(
                        "   Score: " + result.getScore()
                );

                System.out.println(
                        "   Matched terms: "
                                + String.join(", ", result.getMatchedTerms())
                );

                position++;
            }

        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
    }
}