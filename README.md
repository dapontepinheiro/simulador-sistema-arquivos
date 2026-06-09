Aqui está o seu texto com a acentuação e a pontuação corrigidas:

# Simulador de Sistema de Arquivos com Journaling

Este projeto foi desenvolvido para a disciplina de Sistemas Operacionais. A ideia principal é simular, em Java, algumas operações básicas de um sistema de arquivos, como criar diretórios, copiar arquivos, apagar, renomear e listar conteúdos.

O simulador também usa journaling, que é uma técnica importante para manter a integridade dos dados. Com isso, antes de uma operação alterar o sistema de arquivos virtual, ela fica registrada em um log.

> Link do GitHub: [https://github.com/dapontepinheiro/simulador-sistema-arquivos](https://github.com/dapontepinheiro/simulador-sistema-arquivos)

## Metodologia

O trabalho foi implementado em Java, usando classes para representar arquivos, diretórios, o sistema de arquivos e o journal. O programa pode ser usado por chamadas de métodos e também por um modo shell, no qual o usuário digita comandos parecidos com comandos de um sistema operacional.

O simulador trabalha com dois arquivos principais:

* `filesystem.img`: representa a imagem do sistema de arquivos virtual.
* `journal.log`: guarda o histórico das operações realizadas.

Uma observação importante é que os arquivos e diretórios criados dentro do simulador não são arquivos reais do Windows. Eles ficam armazenados dentro da imagem virtual `filesystem.img`. Por isso, o programa não lista, copia, apaga ou renomeia arquivos do gerenciador de arquivos do computador. Ele manipula apenas o sistema de arquivos simulado.

Cada operação que modifica o sistema passa por três etapas: primeiro ela é validada, depois é registrada no journal e, por fim, é aplicada na imagem do sistema de arquivos. Esse processo ajuda a mostrar como um sistema pode se recuperar melhor caso aconteça alguma falha.

## Parte 1: Introdução ao Sistema de Arquivos com Journaling

Um sistema de arquivos é uma parte essencial do sistema operacional. Ele é responsável por organizar os dados em arquivos e diretórios, permitindo que o usuário e os programas encontrem, salvem e modifiquem informações de forma mais simples.

Sem um sistema de arquivos, o usuário teria que lidar diretamente com detalhes físicos do disco, como blocos e endereços de armazenamento. O sistema de arquivos esconde essa complexidade e oferece uma estrutura mais fácil de entender, baseada em nomes, caminhos e pastas.

### Journaling

Journaling é uma técnica usada para aumentar a segurança e a integridade das informações em um sistema de arquivos. A ideia é registrar uma operação em um log antes de alterar definitivamente a estrutura principal.

Neste simulador, foi usado o conceito de write-ahead logging. Isso significa que a operação é escrita primeiro no journal e só depois é aplicada no sistema de arquivos virtual. Se o programa for encerrado depois do registro da operação, o simulador pode ler o journal na próxima execução e recuperar as operações que já tinham sido confirmadas.

Alguns tipos de journaling são:

* Write-ahead logging: registra a operação antes da alteração definitiva.
* Log-structured file system: organiza escritas em uma estrutura baseada em log.
* Metadata journaling: registra principalmente mudanças nos metadados, como criação e remoção de entradas.
* Full data journaling: registra metadados e também dados dos arquivos, oferecendo mais segurança, mas com maior custo.

## Parte 2: Arquitetura do Simulador

### Estrutura de Dados

O sistema de arquivos foi representado como uma árvore. A raiz é o diretório `/`, e dentro dele podem existir outros diretórios e arquivos.

As classes principais são:

* `FileSystemEntry`: classe base para arquivos e diretórios.
* `File`: representa um arquivo virtual, com nome, conteúdo e tamanho.
* `Directory`: representa um diretório e guarda suas entradas filhas.
* `FileSystemSimulator`: concentra as operações principais do sistema.
* `Journal`: registra e lê as operações do log.
* `Main`: executa o modo shell.
* `FileSystemSelfTest`: executa um teste automático das funcionalidades.

Na classe `Directory`, os filhos são guardados em um `LinkedHashMap`. Essa estrutura permite encontrar entradas pelo nome e também manter a ordem em que elas foram inseridas, o que deixa a listagem mais organizada.

### Journaling

O journal foi implementado como um arquivo de texto. Cada operação recebe um número de transação e é registrada com três partes:

* `BEGIN`: indica o início da transação.
* `OP`: guarda a operação e seus parâmetros.
* `COMMIT`: confirma que a operação foi registrada.

Exemplo de registro:

```text
BEGIN|1|2026-06-09T10:02:19
OP|1|CREATE_DIRECTORY|L2RvY3M
COMMIT|1|2026-06-09T10:02:19

```

Os parâmetros são salvos em Base64 para evitar problemas com espaços, barras e caracteres especiais nos caminhos.

## Parte 3: Implementação em Java

### Classe `FileSystemSimulator`

Esta é a principal classe do projeto. Ela carrega a imagem do sistema de arquivos, aplica a recuperação pelo journal e disponibiliza os métodos usados para executar as operações.

Operações implementadas:

* `copyFile(String sourcePath, String targetPath)`
* `deleteFile(String path)`
* `renameFile(String oldPath, String newPath)`
* `createDirectory(String path)`
* `deleteDirectory(String path)`
* `renameDirectory(String oldPath, String newPath)`
* `listDirectory(String path)`

Também foi criado o método `createFile(String path, String content)`. Ele serve para facilitar os testes e o uso do shell, porque a cópia de arquivos precisa que um arquivo já exista dentro do sistema virtual.

### Classes `File` e `Directory`

A classe `File` representa um arquivo dentro do simulador. Ela armazena o nome e o conteúdo textual do arquivo.

A classe `Directory` representa uma pasta. Ela guarda arquivos e outros diretórios, permitindo formar uma estrutura em árvore semelhante a um sistema de arquivos real.

### Classe `Journal`

A classe `Journal` é responsável por registrar as operações no arquivo `journal.log`. Quando o simulador inicia, ele verifica quais transações já foram confirmadas e aplica as que ainda não estavam refletidas na imagem `filesystem.img`.

## Parte 4: Instalação e funcionamento

### Recursos usados

* Java 8 ou superior.
* Serialização de objetos do Java para salvar a imagem do sistema virtual.
* `java.nio.file` para gravar e ler apenas os arquivos internos do simulador.
* Shell interativo para executar os comandos.

### Como compilar

No terminal, dentro da pasta do projeto, execute:

```bash
javac -d out src/br/edu/so/filesystem/*.java

```

### Como executar

Depois de compilar, execute:

```bash
java -cp out br.edu.so.filesystem.Main

```

Também é possível escolher nomes personalizados para a imagem e para o journal:

```bash
java -cp out br.edu.so.filesystem.Main minha_imagem.img meu_journal.log

```

### Como testar automaticamente

O projeto possui uma classe de teste simples, sem depender de bibliotecas externas:

```bash
java -cp out br.edu.so.filesystem.FileSystemSelfTest

```

Esse teste executa as principais operações exigidas no trabalho e confirma se o simulador manteve os dados corretamente.

### Comandos do shell

```text
help
touch <arquivo> [conteúdo]
mkdir <diretório>
ls <diretório>
cpfile <origem> <destino>
rmfile <arquivo>
mvfile <origem> <destino>
rmdir <diretório>
mvdir <origem> <destino>
tree
exit

```

O comando `rmdir` remove apenas diretórios vazios. Essa escolha foi feita para evitar apagamentos acidentais e deixar a operação mais controlada.

Exemplo de uso:

```text
mkdir /docs
touch /docs/aula.txt Sistemas Operacionais
cpfile /docs/aula.txt /docs/copia.txt
ls /docs
mvfile /docs/copia.txt /docs/resumo.txt
rmfile /docs/aula.txt
tree
exit

```

## Resultados Esperados

Com este simulador, é possível visualizar de forma prática como um sistema de arquivos pode organizar informações em arquivos e diretórios. O projeto também mostra como o journaling ajuda a manter a integridade dos dados, registrando as operações antes da atualização definitiva da imagem.

Assim, o trabalho ajuda a relacionar comandos comuns de manipulação de arquivos com conceitos estudados em Sistemas Operacionais, como estrutura em árvore, persistência, recuperação, metadados e log de transações.