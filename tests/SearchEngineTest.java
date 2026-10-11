import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import model.Document;
import model.SearchResult;
import service.DocumentLoader;
import service.SearchEngine;
import service.SearchIndexer;
import service.TextProcessor;

public class SearchEngineTest {

    private static int passed = 0;
    private static int failed = 0;

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface DirectoryTest {
        void run(Path directory) throws Exception;
    }

    public static void main(String[] args) {
        System.out.println("Running Spy-der tests...\n");

        test("Document validation", () -> {
            expect(
                    IllegalArgumentException.class,
                    () -> new Document(0, "file.txt", "content")
            );

            expect(
                    IllegalArgumentException.class,
                    () -> new Document(1, " ", "content")
            );

            expect(
                    IllegalArgumentException.class,
                    () -> new Document(1, "file.txt", null)
            );

            equal("", new Document(1, "empty.txt", "").getContent());
        });

        test("Loads text files in filename order", () -> {
            withTemporaryDirectory(directory -> {
                write(directory, "b.txt", "Second document");
                write(directory, "a.txt", "First document");
                write(directory, "ignore.csv", "Not a text document");

                Files.createDirectory(directory.resolve("folder.txt"));

                List<Document> documents =
                        new DocumentLoader().loadDocuments(directory);

                equal(2, documents.size());
                equal("a.txt", documents.get(0).getFilename());
                equal(1, documents.get(0).getId());
                equal("First document", documents.get(0).getContent());
                equal("b.txt", documents.get(1).getFilename());
                equal(2, documents.get(1).getId());
            });
        });

        test("Reads UTF-8 and uppercase TXT extensions", () -> {
            withTemporaryDirectory(directory -> {
                write(directory, "notes.TXT", "Café résumé");

                List<Document> documents =
                        new DocumentLoader().loadDocuments(directory);

                equal(1, documents.size());
                equal("Café résumé", documents.get(0).getContent());
            });
        });

        test("Handles an empty directory", () -> {
            withTemporaryDirectory(directory -> {
                equal(
                        0,
                        new DocumentLoader()
                                .loadDocuments(directory)
                                .size()
                );
            });
        });

        test("Rejects missing directories and file paths", () -> {
            withTemporaryDirectory(directory -> {
                DocumentLoader loader = new DocumentLoader();

                expect(
                        IOException.class,
                        () -> loader.loadDocuments(
                                directory.resolve("missing")
                        )
                );

                Path file = write(directory, "file.txt", "content");

                expect(
                        IOException.class,
                        () -> loader.loadDocuments(file)
                );
            });
        });

        test("Skips invalid UTF-8 and continues loading", () -> {
            withTemporaryDirectory(directory -> {
                Files.write(
                        directory.resolve("bad.txt"),
                        new byte[]{(byte) 0xC3, (byte) 0x28}
                );

                write(directory, "good.txt", "Readable document");

                List<Document> documents =
                        new DocumentLoader().loadDocuments(directory);

                equal(1, documents.size());
                equal("good.txt", documents.get(0).getFilename());
                equal(1, documents.get(0).getId());
            });
        });

        test("Normalizes case and separates punctuation", () -> {
            TextProcessor processor = new TextProcessor();

            equal(
                    List.of("java", "python", "java", "21"),
                    processor.process("JAVA,Python! Java-21")
            );
        });

        test("Removes stop words and keeps repetitions", () -> {
            TextProcessor processor = new TextProcessor();

            equal(
                    List.of("java", "java", "useful"),
                    processor.process("The Java and JAVA are useful.")
            );
        });

        test("Handles empty text and Unicode letters", () -> {
            TextProcessor processor = new TextProcessor();

            equal(List.of(), processor.process(""));
            equal(List.of(), processor.process("  !!!  "));
            equal(List.of(), processor.process("the and is"));

            equal(
                    List.of("café", "résumé"),
                    processor.process("CAFÉ résumé")
            );

            expect(
                    IllegalArgumentException.class,
                    () -> processor.process(null)
            );
        });

        test("Builds correct term frequencies", () -> {
            SearchIndexer indexer = sampleIndex();

            equal(3, indexer.getDocumentCount());
            equal(4, indexer.getTermCount());

            equal(
                    Map.of(1, 2, 2, 1),
                    indexer.getPostings("java")
            );

            equal(
                    Map.of(1, 1, 3, 1),
                    indexer.getPostings("collections")
            );

            equal(
                    List.of("collections", "java", "python", "sql"),
                    indexer.getIndexedTerms()
            );

            equal(Map.of(), indexer.getPostings("missing"));
        });

        test("Rebuilding replaces the previous index", () -> {
            SearchIndexer indexer = sampleIndex();

            indexer.buildIndex(
                    List.of(new Document(4, "new.txt", "network"))
            );

            equal(1, indexer.getDocumentCount());
            equal(Map.of(), indexer.getPostings("java"));
            equal(Map.of(4, 1), indexer.getPostings("network"));
            equal(null, indexer.getDocument(1));
        });

        test("Duplicate IDs leave the previous index intact", () -> {
            SearchIndexer indexer = sampleIndex();

            expect(
                    IllegalArgumentException.class,
                    () -> indexer.buildIndex(
                            List.of(
                                    new Document(9, "one.txt", "first"),
                                    new Document(9, "two.txt", "second")
                            )
                    )
            );

            equal(3, indexer.getDocumentCount());
            equal(Map.of(1, 2, 2, 1), indexer.getPostings("java"));
            equal(Map.of(), indexer.getPostings("first"));
        });

        test("Single-term search ranks by frequency", () -> {
            SearchEngine engine = new SearchEngine(sampleIndex());
            List<SearchResult> results = engine.search("java");

            equal(2, results.size());
            equal("alpha.txt", results.get(0).getDocument().getFilename());
            equal(2, results.get(0).getScore());
            equal("beta.txt", results.get(1).getDocument().getFilename());
            equal(1, results.get(1).getScore());
        });

        test("Multi-term search scores all matching documents", () -> {
            SearchEngine engine = new SearchEngine(sampleIndex());

            List<SearchResult> results =
                    engine.search("java collections");

            equal(3, results.size());

            equal("alpha.txt", results.get(0).getDocument().getFilename());
            equal(3, results.get(0).getScore());
            equal(
                    List.of("java", "collections"),
                    results.get(0).getMatchedTerms()
            );

            equal("beta.txt", results.get(1).getDocument().getFilename());
            equal(1, results.get(1).getScore());
            equal(List.of("java"), results.get(1).getMatchedTerms());

            equal("gamma.txt", results.get(2).getDocument().getFilename());
            equal(1, results.get(2).getScore());
            equal(
                    List.of("collections"),
                    results.get(2).getMatchedTerms()
            );
        });

        test("Query normalization and duplicates preserve scores", () -> {
            SearchEngine engine = new SearchEngine(sampleIndex());

            List<SearchResult> expected = engine.search("java");

            List<SearchResult> actual =
                    engine.search("THE JAVA, java!");

            sameResults(expected, actual);
        });

        test("Unknown terms and empty indexes return no results", () -> {
            SearchEngine engine = new SearchEngine(sampleIndex());

            equal(0, engine.search("blockchain").size());

            sameResults(
                    engine.search("java"),
                    engine.search("java blockchain")
            );

            SearchEngine emptyEngine =
                    new SearchEngine(new SearchIndexer());

            equal(0, emptyEngine.search("java").size());
        });

        test("Rejects queries without searchable terms", () -> {
            SearchEngine engine = new SearchEngine(sampleIndex());

            expect(
                    IllegalArgumentException.class,
                    () -> engine.search(null)
            );

            expect(
                    IllegalArgumentException.class,
                    () -> engine.search("   ")
            );

            expect(
                    IllegalArgumentException.class,
                    () -> engine.search("the and is")
            );

            expect(
                    IllegalArgumentException.class,
                    () -> engine.search("!!!")
            );
        });

        test("Equal scores sort by filename then ID", () -> {
            SearchIndexer indexer = new SearchIndexer();

            indexer.buildIndex(
                    List.of(
                            new Document(3, "zeta.txt", "java"),
                            new Document(2, "alpha.txt", "java"),
                            new Document(1, "alpha.txt", "java")
                    )
            );

            List<SearchResult> results =
                    new SearchEngine(indexer).search("java");

            equal(1, results.get(0).getDocument().getId());
            equal(2, results.get(1).getDocument().getId());
            equal(3, results.get(2).getDocument().getId());
        });

        test("Returned collections cannot be modified", () -> {
            SearchIndexer indexer = sampleIndex();

            expect(
                    UnsupportedOperationException.class,
                    () -> indexer.getPostings("java").put(99, 100)
            );

            expect(
                    UnsupportedOperationException.class,
                    () -> indexer.getDocuments().clear()
            );

            expect(
                    UnsupportedOperationException.class,
                    () -> indexer.getIndexedTerms().clear()
            );

            List<SearchResult> results =
                    new SearchEngine(indexer).search("java");

            expect(
                    UnsupportedOperationException.class,
                    () -> results.clear()
            );

            expect(
                    UnsupportedOperationException.class,
                    () -> results.get(0).getMatchedTerms().clear()
            );
        });

        test("Loads indexes and searches real temporary files", () -> {
            withTemporaryDirectory(directory -> {
                write(directory, "java.txt", "Java java collections");
                write(directory, "python.txt", "Python collections");

                List<Document> documents =
                        new DocumentLoader().loadDocuments(directory);

                SearchIndexer indexer = new SearchIndexer();
                indexer.buildIndex(documents);

                List<SearchResult> results =
                        new SearchEngine(indexer)
                                .search("JAVA collections");

                equal(2, results.size());
                equal("java.txt", results.get(0).getDocument().getFilename());
                equal(3, results.get(0).getScore());
                equal("python.txt", results.get(1).getDocument().getFilename());
                equal(1, results.get(1).getScore());
            });
        });

        System.out.println(
                "\nResults: " + passed + " passed, "
                        + failed + " failed."
        );

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static SearchIndexer sampleIndex() {
        SearchIndexer indexer = new SearchIndexer();

        indexer.buildIndex(
                List.of(
                        new Document(
                                1,
                                "alpha.txt",
                                "Java java collections"
                        ),
                        new Document(
                                2,
                                "beta.txt",
                                "Java SQL"
                        ),
                        new Document(
                                3,
                                "gamma.txt",
                                "Python collections"
                        )
                )
        );

        return indexer;
    }

    private static void sameResults(
            List<SearchResult> expected,
            List<SearchResult> actual) {

        equal(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            equal(
                    expected.get(i).getDocument().getId(),
                    actual.get(i).getDocument().getId()
            );

            equal(
                    expected.get(i).getScore(),
                    actual.get(i).getScore()
            );

            equal(
                    expected.get(i).getMatchedTerms(),
                    actual.get(i).getMatchedTerms()
            );
        }
    }

    private static Path write(
            Path directory,
            String filename,
            String content) throws IOException {

        return Files.writeString(
                directory.resolve(filename),
                content,
                StandardCharsets.UTF_8
        );
    }

    private static void withTemporaryDirectory(
            DirectoryTest action) throws Exception {

        Path directory = Files.createTempDirectory("spy-der-test-");

        try {
            action.run(directory);
        } finally {
            List<Path> paths;

            try (Stream<Path> stream = Files.walk(directory)) {
                paths = stream
                        .sorted(Comparator.reverseOrder())
                        .toList();
            }

            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void test(String name, TestCase action) {
        try {
            action.run();
            passed++;
            System.out.println("PASS: " + name);

        } catch (Exception | AssertionError error) {
            failed++;

            System.out.println(
                    "FAIL: " + name + " - " + error.getMessage()
            );
        }
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(
                    "Expected " + expected + ", got " + actual
            );
        }
    }

    private static void expect(
            Class<? extends Exception> expectedType,
            TestCase action) throws Exception {

        try {
            action.run();

        } catch (Exception error) {
            if (expectedType.isInstance(error)) {
                return;
            }

            throw new AssertionError(
                    "Expected " + expectedType.getSimpleName()
                            + ", got " + error.getClass().getSimpleName(),
                    error
            );
        }

        throw new AssertionError(
                "Expected " + expectedType.getSimpleName()
                        + " but no exception was thrown."
        );
    }
}