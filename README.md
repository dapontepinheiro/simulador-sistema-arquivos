# Simulador de Sistema de Arquivos com Journaling

Este projeto foi desenvolvido para a disciplina de Sistemas Operacionais. A ideia principal e simular, em Java, algumas operacoes basicas de um sistema de arquivos, como criar diretorios, copiar arquivos, apagar, renomear e listar conteudos.

O simulador tambem usa journaling, que e uma tecnica importante para manter a integridade dos dados. Com isso, antes de uma operacao alterar o sistema de arquivos virtual, ela fica registrada em um log.

> Link do GitHub: inserir aqui o link do repositorio depois de publicar o projeto.

## Metodologia

O trabalho foi implementado em Java, usando classes para representar arquivos, diretorios, o sistema de arquivos e o journal. O programa pode ser usado por chamadas de metodos e tambem por um modo shell, no qual o usuario digita comandos parecidos com comandos de um sistema operacional.

O simulador trabalha com dois arquivos principais:

- `filesystem.img`: representa a imagem do sistema de arquivos virtual.
- `journal.log`: guarda o historico das operacoes realizadas.

Uma observacao importante e que os arquivos e diretorios criados dentro do simulador nao sao arquivos reais do Windows. Eles ficam armazenados dentro da imagem virtual `filesystem.img`. Por isso, o programa nao lista, copia, apaga ou renomeia arquivos do gerenciador de arquivos do computador. Ele manipula apenas o sistema de arquivos simulado.

Cada operacao que modifica o sistema passa por tres etapas: primeiro ela e validada, depois e registrada no journal e, por fim, e aplicada na imagem do sistema de arquivos. Esse processo ajuda a mostrar como um sistema pode se recuperar melhor caso aconteca alguma falha.

## Parte 1: Introducao ao Sistema de Arquivos com Journaling

Um sistema de arquivos e uma parte essencial do sistema operacional. Ele e responsavel por organizar os dados em arquivos e diretorios, permitindo que o usuario e os programas encontrem, salvem e modifiquem informacoes de forma mais simples.

Sem um sistema de arquivos, o usuario teria que lidar diretamente com detalhes fisicos do disco, como blocos e enderecos de armazenamento. O sistema de arquivos esconde essa complexidade e oferece uma estrutura mais facil de entender, baseada em nomes, caminhos e pastas.

### Journaling

Journaling e uma tecnica usada para aumentar a seguranca e a integridade das informacoes em um sistema de arquivos. A ideia e registrar uma operacao em um log antes de alterar definitivamente a estrutura principal.

Neste simulador, foi usado o conceito de write-ahead logging. Isso significa que a operacao e escrita primeiro no journal e so depois e aplicada no sistema de arquivos virtual. Se o programa for encerrado depois do registro da operacao, o simulador pode ler o journal na proxima execucao e recuperar as operacoes que ja tinham sido confirmadas.

Alguns tipos de journaling sao:

- Write-ahead logging: registra a operacao antes da alteracao definitiva.
- Log-structured file system: organiza escritas em uma estrutura baseada em log.
- Metadata journaling: registra principalmente mudancas nos metadados, como criacao e remocao de entradas.
- Full data journaling: registra metadados e tambem dados dos arquivos, oferecendo mais seguranca, mas com maior custo.

## Parte 2: Arquitetura do Simulador

### Estrutura de Dados

O sistema de arquivos foi representado como uma arvore. A raiz e o diretorio `/`, e dentro dele podem existir outros diretorios e arquivos.

As classes principais sao:

- `FileSystemEntry`: classe base para arquivos e diretorios.
- `File`: representa um arquivo virtual, com nome, conteudo e tamanho.
- `Directory`: representa um diretorio e guarda suas entradas filhas.
- `FileSystemSimulator`: concentra as operacoes principais do sistema.
- `Journal`: registra e le as operacoes do log.
- `Main`: executa o modo shell.
- `FileSystemSelfTest`: executa um teste automatico das funcionalidades.

Na classe `Directory`, os filhos sao guardados em um `LinkedHashMap`. Essa estrutura permite encontrar entradas pelo nome e tambem manter a ordem em que elas foram inseridas, o que deixa a listagem mais organizada.

### Journaling

O journal foi implementado como um arquivo de texto. Cada operacao recebe um numero de transacao e e registrada com tres partes:

- `BEGIN`: indica o inicio da transacao.
- `OP`: guarda a operacao e seus parametros.
- `COMMIT`: confirma que a operacao foi registrada.

Exemplo de registro:

```text
BEGIN|1|2026-06-09T10:02:19
OP|1|CREATE_DIRECTORY|L2RvY3M
COMMIT|1|2026-06-09T10:02:19
```

Os parametros sao salvos em Base64 para evitar problemas com espacos, barras e caracteres especiais nos caminhos.

## Parte 3: Implementacao em Java

### Classe `FileSystemSimulator`

Esta e a principal classe do projeto. Ela carrega a imagem do sistema de arquivos, aplica a recuperacao pelo journal e disponibiliza os metodos usados para executar as operacoes.

Operacoes implementadas:

- `copyFile(String sourcePath, String targetPath)`
- `deleteFile(String path)`
- `renameFile(String oldPath, String newPath)`
- `createDirectory(String path)`
- `deleteDirectory(String path)`
- `renameDirectory(String oldPath, String newPath)`
- `listDirectory(String path)`

Tambem foi criado o metodo `createFile(String path, String content)`. Ele serve para facilitar os testes e o uso do shell, porque a copia de arquivos precisa que um arquivo ja exista dentro do sistema virtual.

### Classes `File` e `Directory`

A classe `File` representa um arquivo dentro do simulador. Ela armazena o nome e o conteudo textual do arquivo.

A classe `Directory` representa uma pasta. Ela guarda arquivos e outros diretorios, permitindo formar uma estrutura em arvore semelhante a um sistema de arquivos real.

### Classe `Journal`

A classe `Journal` e responsavel por registrar as operacoes no arquivo `journal.log`. Quando o simulador inicia, ele verifica quais transacoes ja foram confirmadas e aplica as que ainda nao estavam refletidas na imagem `filesystem.img`.

## Parte 4: Instalacao e funcionamento

### Recursos usados

- Java 8 ou superior.
- Serializacao de objetos do Java para salvar a imagem do sistema virtual.
- `java.nio.file` para gravar e ler apenas os arquivos internos do simulador.
- Shell interativo para executar os comandos.

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

Tambem e possivel escolher nomes personalizados para a imagem e para o journal:

```bash
java -cp out br.edu.so.filesystem.Main minha_imagem.img meu_journal.log
```

### Como testar automaticamente

O projeto possui uma classe de teste simples, sem depender de bibliotecas externas:

```bash
java -cp out br.edu.so.filesystem.FileSystemSelfTest
```

Esse teste executa as principais operacoes exigidas no trabalho e confirma se o simulador manteve os dados corretamente.

### Comandos do shell

```text
help
touch <arquivo> [conteudo]
mkdir <diretorio>
ls <diretorio>
cpfile <origem> <destino>
rmfile <arquivo>
mvfile <origem> <destino>
rmdir <diretorio>
mvdir <origem> <destino>
tree
exit
```

O comando `rmdir` remove apenas diretorios vazios. Essa escolha foi feita para evitar apagamentos acidentais e deixar a operacao mais controlada.

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

Com este simulador, e possivel visualizar de forma pratica como um sistema de arquivos pode organizar informacoes em arquivos e diretorios. O projeto tambem mostra como o journaling ajuda a manter a integridade dos dados, registrando as operacoes antes da atualizacao definitiva da imagem.

Assim, o trabalho ajuda a relacionar comandos comuns de manipulacao de arquivos com conceitos estudados em Sistemas Operacionais, como estrutura em arvore, persistencia, recuperacao, metadados e log de transacoes.
