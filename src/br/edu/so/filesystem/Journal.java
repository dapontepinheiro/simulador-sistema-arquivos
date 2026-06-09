package br.edu.so.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Journal {
    public enum OperationType {
        CREATE_FILE,
        COPY_FILE,
        DELETE_FILE,
        RENAME_FILE,
        CREATE_DIRECTORY,
        DELETE_DIRECTORY,
        RENAME_DIRECTORY
    }

    public static class Operation {
        private final long transactionId;
        private final OperationType type;
        private final List<String> arguments;

        public Operation(long transactionId, OperationType type, List<String> arguments) {
            this.transactionId = transactionId;
            this.type = type;
            this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
        }

        public long getTransactionId() {
            return transactionId;
        }

        public OperationType getType() {
            return type;
        }

        public List<String> getArguments() {
            return arguments;
        }
    }

    private final Path journalPath;

    public Journal(Path journalPath) {
        this.journalPath = journalPath;
    }

    public void ensureExists() throws IOException {
        if (journalPath.getParent() != null) {
            Files.createDirectories(journalPath.getParent());
        }
        if (!Files.exists(journalPath)) {
            Files.createFile(journalPath);
        }
    }

    public long findLastTransactionId() throws IOException {
        ensureExists();
        long lastId = 0;
        for (String line : Files.readAllLines(journalPath, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\\|", -1);
            if (parts.length >= 2) {
                try {
                    lastId = Math.max(lastId, Long.parseLong(parts[1]));
                } catch (NumberFormatException ignored) {
                    // Linhas invalidas sao ignoradas para manter a recuperacao simples.
                }
            }
        }
        return lastId;
    }

    public void appendCommitted(long transactionId, OperationType type, String... arguments) throws IOException {
        ensureExists();
        appendLine("BEGIN|" + transactionId + "|" + LocalDateTime.now());
        appendLine("OP|" + transactionId + "|" + type.name() + encodeArguments(arguments));
        appendLine("COMMIT|" + transactionId + "|" + LocalDateTime.now());
    }

    public List<Operation> readCommittedOperationsAfter(long lastAppliedTransactionId) throws IOException {
        ensureExists();
        Map<Long, Operation> operations = new HashMap<>();
        Set<Long> committed = new HashSet<>();

        for (String line : Files.readAllLines(journalPath, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 2) {
                continue;
            }

            long transactionId;
            try {
                transactionId = Long.parseLong(parts[1]);
            } catch (NumberFormatException exception) {
                continue;
            }

            if ("OP".equals(parts[0]) && parts.length >= 3) {
                OperationType type = OperationType.valueOf(parts[2]);
                List<String> arguments = new ArrayList<>();
                for (int i = 3; i < parts.length; i++) {
                    arguments.add(decode(parts[i]));
                }
                operations.put(transactionId, new Operation(transactionId, type, arguments));
            } else if ("COMMIT".equals(parts[0])) {
                committed.add(transactionId);
            }
        }

        List<Operation> result = new ArrayList<>();
        for (Long transactionId : committed) {
            if (transactionId > lastAppliedTransactionId && operations.containsKey(transactionId)) {
                result.add(operations.get(transactionId));
            }
        }
        result.sort((a, b) -> Long.compare(a.getTransactionId(), b.getTransactionId()));
        return result;
    }

    private void appendLine(String line) throws IOException {
        Files.write(
                journalPath,
                (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private String encodeArguments(String... arguments) {
        StringBuilder builder = new StringBuilder();
        for (String argument : arguments) {
            builder.append('|').append(encode(argument));
        }
        return builder.toString();
    }

    private String encode(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(safeValue.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
