package br.edu.so.filesystem;

import br.edu.so.filesystem.Journal.Operation;
import br.edu.so.filesystem.Journal.OperationType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileSystemSimulator {
    private final Path imagePath;
    private final Journal journal;
    private FileSystemState state;
    private long nextTransactionId;

    public FileSystemSimulator(Path imagePath, Path journalPath) throws IOException, ClassNotFoundException {
        this.imagePath = imagePath;
        this.journal = new Journal(journalPath);
        this.state = loadState();
        recoverCommittedOperations();
        this.nextTransactionId = Math.max(journal.findLastTransactionId(), state.getLastAppliedTransactionId()) + 1;
    }

    public void createFile(String path, String content) throws IOException {
        executeJournaled(OperationType.CREATE_FILE, path, content);
    }

    public void copyFile(String sourcePath, String targetPath) throws IOException {
        executeJournaled(OperationType.COPY_FILE, sourcePath, targetPath);
    }

    public void deleteFile(String path) throws IOException {
        executeJournaled(OperationType.DELETE_FILE, path);
    }

    public void renameFile(String oldPath, String newPath) throws IOException {
        executeJournaled(OperationType.RENAME_FILE, oldPath, newPath);
    }

    public void createDirectory(String path) throws IOException {
        executeJournaled(OperationType.CREATE_DIRECTORY, path);
    }

    public void deleteDirectory(String path) throws IOException {
        executeJournaled(OperationType.DELETE_DIRECTORY, path);
    }

    public void renameDirectory(String oldPath, String newPath) throws IOException {
        executeJournaled(OperationType.RENAME_DIRECTORY, oldPath, newPath);
    }

    public List<String> listDirectory(String path) {
        Directory directory = resolveDirectory(path);
        List<String> lines = new ArrayList<>();

        for (FileSystemEntry entry : directory.getChildren()) {
            String type = entry.isDirectory() ? "<DIR>" : "     ";
            String size = entry.isDirectory() ? "-" : String.valueOf(((File) entry).getSize());
            lines.add(String.format("%s %-20s %8s bytes  criado em %s",
                    type,
                    entry.getName(),
                    size,
                    entry.getFormattedCreatedAt()));
        }

        return lines;
    }

    public String tree() {
        StringBuilder builder = new StringBuilder();
        builder.append("/").append(System.lineSeparator());
        appendTree(state.getRoot(), builder, "");
        return builder.toString();
    }

    private void executeJournaled(OperationType type, String... arguments) throws IOException {
        long transactionId = nextTransactionId++;
        validateOperation(type, arguments);
        journal.appendCommitted(transactionId, type, arguments);
        applyOperation(new Operation(transactionId, type, Arrays.asList(arguments)));
        state.setLastAppliedTransactionId(transactionId);
        saveState();
    }

    private void recoverCommittedOperations() throws IOException {
        List<Operation> operations = journal.readCommittedOperationsAfter(state.getLastAppliedTransactionId());
        if (operations.isEmpty()) {
            return;
        }

        for (Operation operation : operations) {
            applyOperation(operation);
            state.setLastAppliedTransactionId(operation.getTransactionId());
        }
        saveState();
    }

    private void applyOperation(Operation operation) {
        List<String> args = operation.getArguments();

        switch (operation.getType()) {
            case CREATE_FILE:
                applyCreateFile(args.get(0), args.get(1));
                break;
            case COPY_FILE:
                applyCopyFile(args.get(0), args.get(1));
                break;
            case DELETE_FILE:
                applyDeleteFile(args.get(0));
                break;
            case RENAME_FILE:
                applyRenameFile(args.get(0), args.get(1));
                break;
            case CREATE_DIRECTORY:
                applyCreateDirectory(args.get(0));
                break;
            case DELETE_DIRECTORY:
                applyDeleteDirectory(args.get(0));
                break;
            case RENAME_DIRECTORY:
                applyRenameDirectory(args.get(0), args.get(1));
                break;
            default:
                throw new IllegalArgumentException("Operacao desconhecida: " + operation.getType());
        }
    }

    private void validateOperation(OperationType type, String... args) {
        switch (type) {
            case CREATE_FILE:
                validateCreateEntry(args[0]);
                break;
            case COPY_FILE:
                resolveFile(args[0]);
                validateCreateEntry(args[1]);
                break;
            case DELETE_FILE:
                resolveFile(args[0]);
                break;
            case RENAME_FILE:
                resolveFile(args[0]);
                validateMove(args[0], args[1], false);
                break;
            case CREATE_DIRECTORY:
                validateCreateEntry(args[0]);
                break;
            case DELETE_DIRECTORY:
                validateDeleteDirectory(args[0]);
                break;
            case RENAME_DIRECTORY:
                resolveDirectory(args[0]);
                validateMove(args[0], args[1], true);
                break;
            default:
                throw new IllegalArgumentException("Operacao desconhecida: " + type);
        }
    }

    private void validateCreateEntry(String path) {
        PathParts parts = splitPath(path);
        Directory parent = resolveDirectory(parts.parentPath);
        if (parent.contains(parts.name)) {
            throw new IllegalArgumentException("Ja existe uma entrada em: " + path);
        }
    }

    private void validateDeleteDirectory(String path) {
        if (normalizePath(path).equals("/")) {
            throw new IllegalArgumentException("A raiz nao pode ser apagada.");
        }

        Directory directory = resolveDirectory(path);
        if (!directory.isEmpty()) {
            throw new IllegalArgumentException("O diretorio precisa estar vazio para ser apagado: " + path);
        }
    }

    private void validateMove(String oldPath, String newPath, boolean directoryExpected) {
        PathParts newParts = splitPath(newPath);
        Directory newParent = resolveDirectory(newParts.parentPath);

        if (newParent.contains(newParts.name)) {
            throw new IllegalArgumentException("Ja existe uma entrada em: " + newPath);
        }

        if (directoryExpected && normalizePath(oldPath).equals("/")) {
            throw new IllegalArgumentException("A raiz nao pode ser renomeada.");
        }

        if (directoryExpected && isMovingDirectoryInsideItself(oldPath, newPath)) {
            throw new IllegalArgumentException("Um diretorio nao pode ser movido para dentro dele mesmo.");
        }
    }

    private void applyCreateFile(String path, String content) {
        PathParts parts = splitPath(path);
        Directory parent = resolveDirectory(parts.parentPath);
        if (parent.contains(parts.name)) {
            throw new IllegalArgumentException("Ja existe uma entrada em: " + path);
        }
        parent.addChild(new File(parts.name, content));
    }

    private void applyCopyFile(String sourcePath, String targetPath) {
        File source = resolveFile(sourcePath);
        PathParts target = splitPath(targetPath);
        Directory targetParent = resolveDirectory(target.parentPath);

        if (targetParent.contains(target.name)) {
            throw new IllegalArgumentException("Ja existe uma entrada em: " + targetPath);
        }
        targetParent.addChild(new File(target.name, source.getContent()));
    }

    private void applyDeleteFile(String path) {
        PathParts parts = splitPath(path);
        Directory parent = resolveDirectory(parts.parentPath);
        FileSystemEntry entry = parent.getChild(parts.name);

        if (!(entry instanceof File)) {
            throw new IllegalArgumentException("Arquivo nao encontrado: " + path);
        }
        parent.removeChild(parts.name);
    }

    private void applyRenameFile(String oldPath, String newPath) {
        File file = resolveFile(oldPath);
        moveOrRename(file, oldPath, newPath, false);
    }

    private void applyCreateDirectory(String path) {
        PathParts parts = splitPath(path);
        Directory parent = resolveDirectory(parts.parentPath);
        if (parent.contains(parts.name)) {
            throw new IllegalArgumentException("Ja existe uma entrada em: " + path);
        }
        parent.addChild(new Directory(parts.name));
    }

    private void applyDeleteDirectory(String path) {
        PathParts parts = splitPath(path);
        Directory parent = resolveDirectory(parts.parentPath);
        parent.removeChild(parts.name);
    }

    private void applyRenameDirectory(String oldPath, String newPath) {
        Directory directory = resolveDirectory(oldPath);
        moveOrRename(directory, oldPath, newPath, true);
    }

    private void moveOrRename(FileSystemEntry entry, String oldPath, String newPath, boolean directoryExpected) {
        PathParts oldParts = splitPath(oldPath);
        PathParts newParts = splitPath(newPath);
        Directory oldParent = resolveDirectory(oldParts.parentPath);
        Directory newParent = resolveDirectory(newParts.parentPath);

        if (newParent.contains(newParts.name)) {
            throw new IllegalArgumentException("Ja existe uma entrada em: " + newPath);
        }

        if (directoryExpected && isMovingDirectoryInsideItself(oldPath, newPath)) {
            throw new IllegalArgumentException("Um diretorio nao pode ser movido para dentro dele mesmo.");
        }

        oldParent.removeChild(oldParts.name);
        entry.rename(newParts.name);
        newParent.addChild(entry);
    }

    private boolean isMovingDirectoryInsideItself(String oldPath, String newPath) {
        String normalizedOldPath = normalizePath(oldPath);
        String normalizedNewPath = normalizePath(newPath);
        return normalizedNewPath.startsWith(normalizedOldPath + "/");
    }

    private Directory resolveDirectory(String path) {
        FileSystemEntry entry = resolveEntry(path);
        if (entry instanceof Directory) {
            return (Directory) entry;
        }
        throw new IllegalArgumentException("Nao e um diretorio: " + path);
    }

    private File resolveFile(String path) {
        FileSystemEntry entry = resolveEntry(path);
        if (entry instanceof File) {
            return (File) entry;
        }
        throw new IllegalArgumentException("Nao e um arquivo: " + path);
    }

    private FileSystemEntry resolveEntry(String path) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath.equals("/")) {
            return state.getRoot();
        }

        String[] parts = normalizedPath.substring(1).split("/");
        Directory current = state.getRoot();

        for (int i = 0; i < parts.length; i++) {
            FileSystemEntry child = current.getChild(parts[i]);
            if (child == null) {
                throw new IllegalArgumentException("Caminho nao encontrado: " + path);
            }
            if (i == parts.length - 1) {
                return child;
            }
            if (!(child instanceof Directory)) {
                throw new IllegalArgumentException("Parte do caminho nao e diretorio: " + parts[i]);
            }
            Directory directory = (Directory) child;
            current = directory;
        }

        return current;
    }

    private PathParts splitPath(String path) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath.equals("/")) {
            throw new IllegalArgumentException("A operacao nao pode usar a raiz como arquivo ou novo diretorio.");
        }

        int lastSlash = normalizedPath.lastIndexOf('/');
        String parentPath = lastSlash == 0 ? "/" : normalizedPath.substring(0, lastSlash);
        String name = normalizedPath.substring(lastSlash + 1);
        FileSystemEntry.validateName(name);
        return new PathParts(parentPath, name);
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Caminho nao pode ser vazio.");
        }

        String normalizedPath = path.trim().replace("\\", "/");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        while (normalizedPath.contains("//")) {
            normalizedPath = normalizedPath.replace("//", "/");
        }
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        return normalizedPath;
    }

    private void appendTree(Directory directory, StringBuilder builder, String prefix) {
        List<FileSystemEntry> entries = new ArrayList<>(directory.getChildren());
        for (int i = 0; i < entries.size(); i++) {
            FileSystemEntry entry = entries.get(i);
            boolean last = i == entries.size() - 1;
            builder.append(prefix)
                    .append(last ? "`-- " : "|-- ")
                    .append(entry.getName())
                    .append(entry.isDirectory() ? "/" : "")
                    .append(System.lineSeparator());

            if (entry instanceof Directory) {
                Directory childDirectory = (Directory) entry;
                appendTree(childDirectory, builder, prefix + (last ? "    " : "|   "));
            }
        }
    }

    private FileSystemState loadState() throws IOException, ClassNotFoundException {
        if (!Files.exists(imagePath)) {
            return new FileSystemState();
        }

        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(imagePath))) {
            return (FileSystemState) input.readObject();
        }
    }

    private void saveState() throws IOException {
        if (imagePath.getParent() != null) {
            Files.createDirectories(imagePath.getParent());
        }

        Path tempPath = imagePath.resolveSibling(imagePath.getFileName() + ".tmp");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(tempPath))) {
            output.writeObject(state);
        }
        try {
            Files.move(tempPath, imagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempPath, imagePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static class PathParts {
        private final String parentPath;
        private final String name;

        private PathParts(String parentPath, String name) {
            this.parentPath = parentPath;
            this.name = name;
        }
    }

    private static class FileSystemState implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Directory root = new Directory("root");
        private long lastAppliedTransactionId;

        public Directory getRoot() {
            return root;
        }

        public long getLastAppliedTransactionId() {
            return lastAppliedTransactionId;
        }

        public void setLastAppliedTransactionId(long lastAppliedTransactionId) {
            this.lastAppliedTransactionId = lastAppliedTransactionId;
        }
    }
}
