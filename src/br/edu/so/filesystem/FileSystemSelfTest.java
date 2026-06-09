package br.edu.so.filesystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileSystemSelfTest {
    public static void main(String[] args) throws Exception {
        Path testDirectory = Paths.get("test-data");
        Path imagePath = testDirectory.resolve("selftest-filesystem.img");
        Path journalPath = testDirectory.resolve("selftest-journal.log");

        Files.createDirectories(testDirectory);
        Files.deleteIfExists(imagePath);
        Files.deleteIfExists(journalPath);

        FileSystemSimulator simulator = new FileSystemSimulator(imagePath, journalPath);

        simulator.createDirectory("/docs");
        simulator.createDirectory("/backup");
        simulator.createFile("/docs/aula.txt", "Sistemas Operacionais");
        simulator.copyFile("/docs/aula.txt", "/backup/aula-copia.txt");
        simulator.renameFile("/backup/aula-copia.txt", "/backup/resumo.txt");

        List<String> backupEntries = simulator.listDirectory("/backup");
        assertContains(backupEntries, "resumo.txt", "Arquivo copiado e renomeado deve aparecer em /backup.");

        simulator.deleteFile("/docs/aula.txt");
        assertNotContains(simulator.listDirectory("/docs"), "aula.txt", "Arquivo apagado nao deve aparecer em /docs.");

        simulator.renameDirectory("/backup", "/arquivados");
        assertContains(simulator.listDirectory("/arquivados"), "resumo.txt", "Diretorio renomeado deve manter seus arquivos.");

        simulator.deleteFile("/arquivados/resumo.txt");
        simulator.deleteDirectory("/arquivados");
        assertNotContains(simulator.listDirectory("/"), "arquivados", "Diretorio vazio apagado nao deve aparecer na raiz.");

        FileSystemSimulator recoveredSimulator = new FileSystemSimulator(imagePath, journalPath);
        assertContains(recoveredSimulator.listDirectory("/"), "docs", "Imagem e journal devem preservar o diretorio /docs.");
        assertNotContains(recoveredSimulator.listDirectory("/"), "arquivados", "Recuperacao nao deve trazer diretorio apagado.");

        System.out.println("Todos os testes do simulador passaram.");
        System.out.println("Imagem virtual usada no teste: " + imagePath.toAbsolutePath());
        System.out.println("Journal usado no teste: " + journalPath.toAbsolutePath());
    }

    private static void assertContains(List<String> lines, String expectedText, String message) {
        for (String line : lines) {
            if (line.contains(expectedText)) {
                return;
            }
        }
        throw new IllegalStateException(message);
    }

    private static void assertNotContains(List<String> lines, String unexpectedText, String message) {
        for (String line : lines) {
            if (line.contains(unexpectedText)) {
                throw new IllegalStateException(message);
            }
        }
    }
}
