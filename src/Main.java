import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import model.Document;
import model.SearchResult;
import service.DocumentLoader;
import service.SearchEngine;
import service.SearchIndexer;

public class Main {

    public static void main(String[] args) {
        DocumentLoader loader = new DocumentLoader();
        SearchIndexer indexer = new SearchIndexer();

        try {
            List<Document> documents = loader.loadDocuments(
                    Path.of("documents")
            );

            indexer.buildIndex(documents);

        } catch (IOException exception) {
            System.out.println(
                    "Unable to load documents: "
                            + exception.getMessage()
            );

            System.out.println(
                    "Check the documents folder and restart the application."
            );

            return;
        }

        SearchEngine engine = new SearchEngine(indexer);

        if (indexer.getDocumentCount() == 0) {
            System.out.println(
                    "No documents were loaded. "
                            + "Add readable .txt files to documents "
                            + "and restart the application."
            );
        }

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running) {
                displayMenu(indexer.getDocumentCount());

                String option = readLine(scanner, "Choose an option: ");

                if (option == null) {
                    System.out.println("\nInput closed. Goodbye!");
                    break;
                }

                try {
                    switch (option.trim()) {
                        case "1" -> searchDocuments(scanner, engine);
                        case "2" -> viewIndexedTerms(indexer);
                        case "3" -> viewDocuments(indexer);
                        case "0" -> {
                            running = false;
                            System.out.println(
                                    "Thank you for using Mini Search Engine!"
                            );
                        }
                        default -> System.out.println(
                                "Invalid option. Choose 0, 1, 2, or 3."
                        );
                    }
                } catch (IllegalArgumentException exception) {
                    System.out.println(exception.getMessage());
                }

                if (running) {
                    String acknowledgement = readLine(
                            scanner,
                            "\nPress Enter to return to the menu..."
                    );

                    if (acknowledgement == null) {
                        System.out.println("\nInput closed. Goodbye!");
                        break;
                    }
                }
            }
        }
    }

    private static void displayMenu(int documentCount) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("           MINI SEARCH ENGINE");
        System.out.println("========================================");
        System.out.println("Documents indexed: " + documentCount);
        System.out.println();
        System.out.println("1. Search documents");
        System.out.println("2. View indexed terms");
        System.out.println("3. View document list");
        System.out.println("0. Exit");
        System.out.println();
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        System.out.flush();

        if (!scanner.hasNextLine()) {
            return null;
        }

        return scanner.nextLine();
    }

    private static void searchDocuments(
            Scanner scanner,
            SearchEngine engine) {

        String query = readLine(scanner, "Enter search query: ");

        if (query == null) {
            return;
        }

        List<SearchResult> results = engine.search(query);

        if (results.isEmpty()) {
            System.out.println("\nNo matching documents found.");
            return;
        }

        System.out.println(
                "\nSearch results for: " + query.trim()
        );

        System.out.println(
                "Matching documents: " + results.size()
        );

        int position = 1;

        for (SearchResult result : results) {
            System.out.println();

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
    }

    private static void viewIndexedTerms(SearchIndexer indexer) {
        List<String> terms = indexer.getIndexedTerms();

        System.out.println("\nINDEXED TERMS");

        if (terms.isEmpty()) {
            System.out.println("No searchable terms are indexed.");
            return;
        }

        System.out.println(
                "Unique searchable terms: " + terms.size()
        );

        System.out.println();

        for (String term : terms) {
            int documentCount = indexer.getPostings(term).size();

            System.out.println(
                    term + " - found in "
                            + documentCount + " document(s)"
            );
        }
    }

    private static void viewDocuments(SearchIndexer indexer) {
        List<Document> documents = indexer.getDocuments();

        System.out.println("\nDOCUMENT LIST");

        if (documents.isEmpty()) {
            System.out.println("No documents are indexed.");
            return;
        }

        for (Document document : documents) {
            System.out.println(
                    document.getId()
                            + ". " + document.getFilename()
            );
        }

        System.out.println(
                "\nTotal documents: " + documents.size()
        );
    }
}