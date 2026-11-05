# Progresso e continuidade da implementação

Este arquivo é o ponto de entrada para retomar o trabalho ou transferi-lo a outro agente. O [README](README.md) contém a especificação da linguagem; este documento registra andamento, próxima tarefa e critérios de conclusão.

## Como colaborar

- O projeto também é uma experiência de aprendizado de Java para o autor.
- **Por padrão, orientar a implementação em pequenos incrementos, explicando código e testes; não implementar o código pelo autor sem solicitação explícita.**
- Alterações de documentação podem ser realizadas quando solicitadas.
- Ler o código atual antes de propor o próximo incremento; não presumir que esta checklist substitui a inspeção do repositório.
- Não ampliar a sintaxe nem escolher valores padrão silenciosamente. Registrar decisões provisórias e pendências.
- Escrever documentação em português e preservar os identificadores existentes.
- Rodar `mvn verify` após mudanças e informar falhas ou impedimentos. Não apresentar testes como aprovados sem executá-los.
- Não fazer commit ou push sem solicitação.

## Estado atual confirmado

**Último commit remoto inspecionado:** `0da1a41` — `feat: implementa analisador sintático inicial`.

- Lexer manual em Java 21 com nomes ASCII, `!`, `->`, espaços e `EOF`.
- Tokens preservam lexema e intervalo `[startOffset, endOffset)` em índices UTF-16.
- Parser manual consome `NAME BANG NAME ARROW NAME EOF`.
- `Cenario` é a AST mínima: um record com os tokens de atacante, golpe e defensor, preservando suas posições.
- Erros léxicos e sintáticos usam provisoriamente `IllegalArgumentException`.
- O parser não consulta dados do domínio: nomes inexistentes podem ser sintaticamente válidos.
- Dependência Smogon preparada no projeto Node.js; adaptador ainda não implementado.
- Ainda não há validação semântica, cálculo integrado ou CLI.

### Arquivos de referência

```txt
src/main/java/br/edu/tcc/dsl/lexer/
  TokenType.java
  Token.java
  Lexer.java
src/main/java/br/edu/tcc/dsl/parser/
  Cenario.java
  Parser.java
src/test/java/br/edu/tcc/dsl/
  AmbienteTest.java
  lexer/TokenTest.java
  lexer/LexerTest.java
  parser/ParserTest.java
motor-smogon/package.json
motor-smogon/package-lock.json
```

### Verificação

Comando de referência, a partir da raiz deste repositório:

```bash
mvn verify
```

Há 25 métodos de teste previstos no estado inspecionado: 13 do lexer, 1 de token, 1 de ambiente e 10 do parser. A contagem de métodos não substitui o resultado de execução. Registrar abaixo o resultado mais recente efetivamente obtido.

## Checklist das etapas

- [x] **1. Lexer:** primeiro incremento com tokens, localização e testes.
- [x] **2. Parser e AST:** estrutura mínima, rejeição de entradas malformadas e testes.
- [ ] **3. Modelo de domínio e acesso aos dados:** geração, catálogo, parâmetros e padrões explícitos.
- [ ] **4. Análise semântica:** resolução de entidades e validação do subconjunto suportado.
- [ ] **5. Integração Smogon:** contrato JSON, adaptador Node.js e tratamento de falhas.
- [ ] **6. Resultado e CLI:** fluxo textual completo com resultado e premissas.
- [ ] **7. Extensões controladas:** apenas funcionalidades acordadas para o escopo final.
- [ ] **8. Avaliação e consolidação:** cenários, evidências, limitações e reprodução.

Testes, documentação e diagnósticos acompanham todas as etapas. A checklist indica marcos iniciais, não componentes que nunca mais precisarão de revisão. O primeiro grande marco será concluir as etapas 1 a 6 para a sintaxe mínima.

## Próxima tarefa: delimitar o modelo semântico e o acesso ao catálogo

**Objetivo:** preparar a etapa 3 sem misturar consulta de dados com o parser nem desenvolver uma calculadora própria.

### Antes de escrever código

- [ ] Confirmar com o autor a geração: o texto acadêmico anterior prevê a 9ª geração; harmonizar essa escolha com o README, que ainda a lista como pendência.
- [ ] Definir a estratégia de consulta ao catálogo: dados distribuídos com o motor ou outra fonte explicitamente versionada. Evitar manter cópias manuais divergentes.
- [ ] Definir como nomes da DSL serão associados aos nomes/identificadores do catálogo, incluindo hífens e maiúsculas/minúsculas.
- [ ] Enumerar os parâmetros necessários e decidir quais serão fixos ou terão padrões documentados no cenário mínimo.
- [ ] Verificar efeitos implícitos do motor, especialmente habilidades, antes de declarar que determinado modificador está ausente.

### Próximo incremento de implementação a propor ao autor

Depois dessas decisões, definir um contrato mínimo de acesso a espécies e golpes, independente da sintaxe, com testes de entidade encontrada e desconhecida. A forma concreta desse contrato deve ser discutida antes de criar classes ou dependências.

Uma prova pequena de acesso ao catálogo no Node.js pode ser antecipada, se necessária para validar a estratégia. Isso não significa implementar todo o adaptador ou a calculadora nessa etapa.

### Critérios de conclusão da etapa 3

- Geração e fonte de dados explicitadas e versionadas.
- Política de resolução de nomes documentada.
- Parâmetros e padrões do cenário mínimo documentados.
- Contrato de consulta demonstrado por testes, sem consultas ao catálogo no lexer ou parser.
- README e este arquivo atualizados, com verificação executada.

## Limites que o próximo agente deve preservar

- Sintaxe atual: `charmander !ember -> caterpie`.
- Manter `!` e `->`; outras decisões de linguagem poderão ser revistas após um protótipo mais concreto.
- Não adicionar itens, clima, habilidades ou atributos personalizados antes de acordar o incremento.
- O motor é `@smogon/calc`, com versão exata no `package.json` e lockfile versionado.
- Não prometer explicação aritmética integral sem verificar o que o motor permite recuperar.
- Comparar resultados com o próprio motor verifica a tradução e a integração, não valida independentemente sua fórmula.

## Pendências de alinhamento acadêmico

- Atualizar referências de implementação com JFlex para a estratégia manual adotada.
- Distinguir uso do Smogon como motor de execução de seu uso anterior como referência independente.
- Harmonizar AST e eventual cenário normalizado, sem criar estruturas adicionais sem necessidade.
- Confirmar escopo final e avaliação com o orientador; não presumir estudo com participantes nem superioridade de compreensão.

## Rotina de atualização

Ao concluir um incremento:

1. Conferir o código e marcar apenas entregas efetivamente realizadas.
2. Atualizar o estado atual e a próxima tarefa com critérios objetivos.
3. Registrar comandos e resultados da verificação, incluindo impedimentos.
4. Alterar o README se a especificação mudou, evitando duplicar aqui suas regras.
5. Atualizar o commit de referência quando existir; não confundir mudanças locais com mudanças publicadas.

### Última verificação

- Em 05/11/2026, `mvn -o verify`: **BUILD SUCCESS**, 25 testes executados, sem falhas, erros ou testes ignorados.
- A tentativa online de `mvn verify` sofreu timeout de rede durante acesso ao repositório Apache; o modo offline utilizou as dependências já presentes no ambiente.
- `git diff --check`: sem problemas de whitespace nas alterações rastreadas.
- Atualização documental local em `README.md` e `PROGRESSO.md`, ainda sem commit ou push.
