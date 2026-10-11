import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import model.Document;
import service.DocumentLoader;
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

            System.out.println(
                    "\nDocuments indexed: " + indexer.getDocumentCount()
            );

            System.out.println(
                    "Unique searchable terms: " + indexer.getTermCount()
            );

            System.out.println("\nINDEXED TERMS");
            System.out.println(
                    String.join(", ", indexer.getIndexedTerms())
            );

            displayPostings(indexer, "java");
            displayPostings(indexer, "collections");
            displayPostings(indexer, "blockchain");

            System.out.println("\nREBUILD CHECK");

            Map<Integer, Integer> before =
                    indexer.getPostings("java");

            indexer.buildIndex(documents);

            Map<Integer, Integer> after =
                    indexer.getPostings("java");

            if (before.equals(after)) {
                System.out.println(
                        "PASS: Rebuilding preserves the java frequencies."
                );
            } else {
                throw new IllegalStateException(
                        "Rebuilding changed the java frequencies."
                );
            }

        } catch (IOException exception) {
            System.out.println(
                    "\nUnable to load documents: "
                            + exception.getMessage()
            );
        }
    }

    private static void displayPostings(
            SearchIndexer indexer,
            String term) {

        System.out.println("\nTERM: " + term);

        Map<Integer, Integer> postings = indexer.getPostings(term);

        if (postings.isEmpty()) {
            System.out.println("No documents contain this term.");
            return;
        }

        for (Map.Entry<Integer, Integer> entry : postings.entrySet()) {
            Document document = indexer.getDocument(entry.getKey());

            System.out.println(
                    document.getFilename()
                            + " -> " + entry.getValue()
                            + " occurrence(s)"
            );
        }
    }
}