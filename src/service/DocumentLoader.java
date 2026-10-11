package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import model.Document;

public class DocumentLoader {

    public List<Document> loadDocuments(Path directory)
            throws IOException {

        if (directory == null) {
            throw new IllegalArgumentException(
                    "Documents directory cannot be null."
            );
        }

        if (!Files.isDirectory(directory)) {
            throw new IOException(
                    "Documents directory does not exist or is not a folder: "
                            + directory.toAbsolutePath()
            );
        }

        List<Path> textFiles;

        try (Stream<Path> paths = Files.list(directory)) {
            textFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .toLowerCase(Locale.ROOT)
                            .endsWith(".txt"))
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString()
                    ))
                    .toList();
        }

        List<Document> documents = new ArrayList<>();
        int nextId = 1;

        for (Path file : textFiles) {
            try {
                String content = Files.readString(
                        file,
                        StandardCharsets.UTF_8
                );

                Document document = new Document(
                        nextId,
                        file.getFileName().toString(),
                        content
                );

                documents.add(document);
                nextId++;

            } catch (IOException exception) {
                System.err.println(
                        "Warning: Could not read "
                                + file.getFileName()
                                + " — " + exception.getMessage()
                );
            }
        }

        return documents;
    }
}