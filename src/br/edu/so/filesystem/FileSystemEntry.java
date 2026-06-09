package br.edu.so.filesystem;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class FileSystemEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private final LocalDateTime createdAt;

    protected FileSystemEntry(String name) {
        validateName(name);
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }

    public void rename(String newName) {
        validateName(newName);
        this.name = newName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public abstract boolean isDirectory();

    protected static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome nao pode ser vazio.");
        }
        if (name.contains("/")) {
            throw new IllegalArgumentException("O nome nao pode conter barras.");
        }
    }
}
