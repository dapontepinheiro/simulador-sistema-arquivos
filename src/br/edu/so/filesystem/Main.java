package br.edu.so.filesystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Path imagePath = Paths.get(args.length > 0 ? args[0] : "filesystem.img");
        Path journalPath = Paths.get(args.length > 1 ? args[1] : "journal.log");

        try {
            FileSystemSimulator simulator = new FileSystemSimulator(imagePath, journalPath);
            runShell(simulator);
        } catch (Exception exception) {
            System.out.println("Erro ao iniciar o simulador: " + exception.getMessage());
        }
    }

    private static void runShell(FileSystemSimulator simulator) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Simulador de Sistema de Arquivos com Journaling");
        System.out.println("Digite 'help' para ver os comandos.");

        while (true) {
            System.out.print("fs> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }
            if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                System.out.println("Encerrando simulador.");
                break;
            }

            try {
                executeCommand(simulator, line);
            } catch (Exception exception) {
                System.out.println("Erro: " + exception.getMessage());
            }
        }
    }

    private static void executeCommand(FileSystemSimulator simulator, String line) throws Exception {
        String[] tokens = line.split("\\s+");
        String command = tokens[0].toLowerCase();

        switch (command) {
            case "help":
                printHelp();
                break;
            case "touch":
                requireAtLeast(tokens, 2, "touch <arquivo> [conteudo]");
                String content = tokens.length > 2 ? joinFrom(tokens, 2) : "";
                simulator.createFile(tokens[1], content);
                System.out.println("Arquivo criado.");
                break;
            case "mkdir":
                requireExactly(tokens, 2, "mkdir <diretorio>");
                simulator.createDirectory(tokens[1]);
                System.out.println("Diretorio criado.");
                break;
            case "ls":
                requireExactly(tokens, 2, "ls <diretorio>");
                List<String> entries = simulator.listDirectory(tokens[1]);
                if (entries.isEmpty()) {
                    System.out.println("Diretorio vazio.");
                } else {
                    entries.forEach(System.out::println);
                }
                break;
            case "cpfile":
                requireExactly(tokens, 3, "cpfile <origem> <destino>");
                simulator.copyFile(tokens[1], tokens[2]);
                System.out.println("Arquivo copiado.");
                break;
            case "rmfile":
                requireExactly(tokens, 2, "rmfile <arquivo>");
                simulator.deleteFile(tokens[1]);
                System.out.println("Arquivo apagado.");
                break;
            case "mvfile":
                requireExactly(tokens, 3, "mvfile <origem> <destino>");
                simulator.renameFile(tokens[1], tokens[2]);
                System.out.println("Arquivo renomeado.");
                break;
            case "rmdir":
                requireExactly(tokens, 2, "rmdir <diretorio>");
                simulator.deleteDirectory(tokens[1]);
                System.out.println("Diretorio apagado.");
                break;
            case "mvdir":
                requireExactly(tokens, 3, "mvdir <origem> <destino>");
                simulator.renameDirectory(tokens[1], tokens[2]);
                System.out.println("Diretorio renomeado.");
                break;
            case "tree":
                System.out.print(simulator.tree());
                break;
            default:
                System.out.println("Comando desconhecido. Digite 'help'.");
                break;
        }
    }

    private static void printHelp() {
        System.out.println("Comandos disponiveis:");
        System.out.println("  help");
        System.out.println("  touch <arquivo> [conteudo]");
        System.out.println("  mkdir <diretorio>");
        System.out.println("  ls <diretorio>");
        System.out.println("  cpfile <origem> <destino>");
        System.out.println("  rmfile <arquivo>");
        System.out.println("  mvfile <origem> <destino>");
        System.out.println("  rmdir <diretorio>");
        System.out.println("  mvdir <origem> <destino>");
        System.out.println("  tree");
        System.out.println("  exit");
    }

    private static void requireExactly(String[] tokens, int expected, String usage) {
        if (tokens.length != expected) {
            throw new IllegalArgumentException("Uso: " + usage);
        }
    }

    private static void requireAtLeast(String[] tokens, int expected, String usage) {
        if (tokens.length < expected) {
            throw new IllegalArgumentException("Uso: " + usage);
        }
    }

    private static String joinFrom(String[] tokens, int startIndex) {
        return String.join(" ", Arrays.copyOfRange(tokens, startIndex, tokens.length));
    }
}
