# Squad 45 - Kyoto

## Integrantes

| Integrante | GitHub / E-mail |
|---|---|
| Arthur Vinícius Melo Santos | [arthur.vmelo@souunit.com.br](mailto:arthur.vmelo@souunit.com.br) |
| Caio Barreto Martins | [@CBMSU](https://github.com/CBMSU) |
| Candson Leite Santana | [candson.leite@souunit.com.br](mailto:candson.leite@souunit.com.br) |
| Gustavo Menezes Carvalho | [@GuMenenezesCa](https://github.com/GuMenenezesCa) |
| João Victor da Silva Santos | [@victoorsaants](https://github.com/victoorsaants) |
| Julia Cabral | [@juliacabrl](https://github.com/juliacabrl) |
| Pedro Lucas Fernandes Elias | [Pedro.lfernandes@souunit.com.br](mailto:Pedro.lfernandes@souunit.com.br) |
| Robert João Slingeneyer de Goeswin de Almeida Mello | [@robertslingeneyer](https://github.com/robertslingeneyer) |

## Como rodar o projeto

### Pré-requisitos

- **JDK 21 ou superior** instalado, com a variável `JAVA_HOME` apontando para ele.
- Não é preciso instalar o Maven: o projeto usa o **Maven Wrapper** (`mvnw`), que baixa a versão certa na primeira execução.

### Compilar e rodar os testes

Linux, macOS ou Git Bash:

```bash
./mvnw verify
```

Windows (PowerShell ou CMD):

```powershell
.\mvnw.cmd verify
```

Outros comandos úteis:

| Comando | O que faz |
|---|---|
| `./mvnw compile` | Só compila o código |
| `./mvnw test` | Compila e roda os testes |
| `./mvnw verify` | Compila, roda os testes e gera o `.jar` em `target/` (é o que o CI roda) |
| `./mvnw clean` | Apaga a pasta `target/` |

### IntelliJ IDEA

Abra a pasta do repositório e importe como projeto **Maven** (o IntelliJ reconhece o `pom.xml`). A pasta `.idea/` não é versionada: cada integrante gera a sua.

## Estrutura do projeto

```text
.
├── .github/workflows/   CI: compila e roda os testes em cada PR
├── database/            Scripts e migrations do banco
├── docs/                Documentação do projeto
├── src/
│   ├── backend/java/    Código Java (pacote base br.com.kyoto)
│   └── frontend/        Interface (ainda não iniciada)
├── tests/java/          Testes automatizados (JUnit 5)
└── pom.xml              Configuração do Maven
```

Pacotes do backend:

| Pacote | Responsabilidade |
|---|---|
| `br.com.kyoto.whatsapp` | Integração com o WhatsApp: webhook de recebimento e envio de mensagens |
| `br.com.kyoto.odrcon` | Acesso ao ODRCon: moradores, condomínios, regras e chamados |
| `br.com.kyoto.conversa` | Controle da conversa com o morador: sessão, etapas e prazos |
| `br.com.kyoto.chamado` | Regras de negócio dos chamados: abertura, termos e acompanhamento |

Os testes ficam em `tests/java`, no mesmo pacote da classe testada (ex.: `tests/java/br/com/kyoto/conversa/TimerTest.java`).

## Configuração do repositório

| Arquivo | Para que serve |
|---|---|
| `pom.xml` | Configura o Maven para usar as pastas da squad (`src/backend/java` e `tests/java`) no lugar do padrão `src/main/java` e `src/test/java` |
| `.gitignore` | Deixa fora do Git os arquivos de build (`target/`, `build/`, `out/`), de IDE (`.idea/`, `*.iml`, `.vscode/`), de segredos (`.env`, exceto o `.env.example`) e do sistema operacional |
| `.gitattributes` | Mantém o final de linha LF no `mvnw` e CRLF no `mvnw.cmd`, para o wrapper funcionar tanto no CI (Linux) quanto no Windows |
| `mvnw`, `mvnw.cmd` e `.mvn/` | Maven Wrapper, gerado pelo próprio Maven. Não editar à mão |
