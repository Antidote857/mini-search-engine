package model;

public class Document {

    private final int id;
    private final String filename;
    private final String content;

    public Document(int id, String filename, String content) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive."
            );
        }

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Filename cannot be empty."
            );
        }

        if (content == null) {
            throw new IllegalArgumentException(
                    "Document content cannot be null."
            );
        }

        this.id = id;
        this.filename = filename;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Document ID: " + id
                + "\nFilename: " + filename
                + "\nContent: " + content;
    }
}