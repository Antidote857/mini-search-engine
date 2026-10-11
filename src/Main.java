import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import model.Document;
import service.DocumentLoader;
import service.TextProcessor;

public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("           MINI SEARCH ENGINE");
        System.out.println("========================================");

        DocumentLoader loader = new DocumentLoader();
        TextProcessor processor = new TextProcessor();

        try {
            List<Document> documents = loader.loadDocuments(
                    Path.of("documents")
            );

            if (documents.isEmpty()) {
                System.out.println("\nNo text documents were loaded.");
                return;
            }

            System.out.println(
                    "\nDocuments loaded: " + documents.size()
            );

            for (Document document : documents) {
                List<String> tokens = processor.process(
                        document.getContent()
                );

                System.out.println();
                System.out.println(
                        "Document: " + document.getFilename()
                );
                System.out.println(
                        "Searchable word count: " + tokens.size()
                );
                System.out.println(
                        "Words: " + String.join(", ", tokens)
                );
            }

            System.out.println("\nNORMALIZATION CHECK");

            List<String> example = processor.process(
                    "JAVA, Java! The collections are useful."
            );

            System.out.println(example);

        } catch (IOException exception) {
            System.out.println(
                    "\nUnable to load documents: "
                            + exception.getMessage()
            );
        }
    }
}