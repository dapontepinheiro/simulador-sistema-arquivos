package br.edu.so.filesystem;

public class File extends FileSystemEntry {
    private static final long serialVersionUID = 1L;

    private String content;

    public File(String name, String content) {
        super(name);
        this.content = content == null ? "" : content;
    }

    public String getContent() {
        return content;
    }

    public long getSize() {
        return content.length();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }
}
