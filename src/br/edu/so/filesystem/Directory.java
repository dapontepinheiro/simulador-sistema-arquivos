package br.edu.so.filesystem;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Directory extends FileSystemEntry {
    private static final long serialVersionUID = 1L;

    private final Map<String, FileSystemEntry> children = new LinkedHashMap<>();

    public Directory(String name) {
        super(name);
    }

    public Collection<FileSystemEntry> getChildren() {
        return children.values();
    }

    public FileSystemEntry getChild(String name) {
        return children.get(name);
    }

    public boolean contains(String name) {
        return children.containsKey(name);
    }

    public void addChild(FileSystemEntry entry) {
        if (contains(entry.getName())) {
            throw new IllegalArgumentException("Ja existe uma entrada com o nome '" + entry.getName() + "'.");
        }
        children.put(entry.getName(), entry);
    }

    public FileSystemEntry removeChild(String name) {
        FileSystemEntry removed = children.remove(name);
        if (removed == null) {
            throw new IllegalArgumentException("Entrada nao encontrada: " + name);
        }
        return removed;
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public boolean isDirectory() {
        return true;
    }
}
