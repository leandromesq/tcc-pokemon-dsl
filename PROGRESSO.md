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

O ambiente (JDK 21, Maven e Node.js 24) está definido em `flake.nix`. A partir da raiz do repositório:

```bash
nix develop path:. -c mvn verify
```

`flake.nix` e `flake.lock` são locais e ficam no `.gitignore`. Por isso o prefixo `path:.` é necessário: sem ele, o Nix só enxerga arquivos rastreados pelo git.

Sem Nix, basta ter JDK 21 e Maven 3.9+ no PATH e rodar `mvn verify`.

Há 25 métodos de teste previstos no estado inspecionado: 13 do lexer, 1 de token, 1 de ambiente e 10 do parser. A contagem de métodos não substitui o resultado de execução. Registrar abaixo o resultado mais recente efetivamente obtido.

## Decisões confirmadas pelo autor (05/11/2026)

Estas decisões divergem do projeto de pesquisa ([leandromesq/Projeto-de-Pesquisa](https://github.com/leandromesq/Projeto-de-Pesquisa)), que ainda não foi atualizado:

1. **`@smogon/calc` é o motor de execução do cálculo**, decisão combinada com o orientador. Não haverá motor de cálculo próprio. O projeto de pesquisa ainda prevê um motor próprio (objetivo específico 4), testes diferenciais contra o Smogon e saída com etapas intermediárias; esses pontos precisam ser reescritos.
2. **Separador `->` em vez de `x`**, para evitar ambiguidade (um `x` também poderia fazer parte de um nome). Outros símbolos podem ser revistos para melhorar a clareza da linguagem.
3. **Item, clima e habilidade continuam no escopo final.** O marco atual (atacante, golpe e defensor) é uma versão mínima para chegar a um primeiro resultado pela CLI.
4. **Lexer e parser manuais**, sem JFlex. O texto acadêmico precisa justificar essa escolha.

5. **Geração: Pokémon Champions** (`Generations.get(0)` no `@smogon/calc` 0.12.0). A fórmula em forma matemática, extraída do motor e conferida com uma derivação manual, está em [docs/formula-dano-champions.md](docs/formula-dano-champions.md).

### Consequências da escolha do Champions

- Catálogo na 0.12.0: 359 espécies, 526 golpes, 166 itens. `charmander`, `caterpie` e `ember` **não existem**. O exemplo sintático `charmander !ember -> caterpie` continua válido para o parser, mas passa a ser semanticamente inválido. O exemplo de referência passa a ser `charizard !flamethrower -> venusaur`.
- Nível sempre 50 (o motor ignora outro valor). Não há IVs, e os EVs são substituídos por pontos de estatística (SP). Os únicos parâmetros por Pokémon com valor padrão a decidir são SP e natureza.
- Limites de SP (informados pelo autor): de 0 a 32 por estatística e no máximo 66 no total. O motor **não valida** esses limites (aceitou 40 sem erro), então a checagem cabe à análise semântica.
- Se nenhuma habilidade for informada, o motor coloca a habilidade padrão da espécie (Charizard recebe `Blaze`). **Decisão do autor para o cenário mínimo:** usar a habilidade padrão da espécie, isto é, a primeira habilidade do catálogo. O Java resolve essa habilidade e o adaptador a envia de forma explícita, sem depender do padrão implícito do motor. Ela aparece nas premissas da saída. A sintaxe para declarar a habilidade virá depois.
- **Política de nomes** (decisão do autor):
  - Forma escrita recomendada: minúsculas, com hífen no lugar de espaços e sem pontuação. Exemplos: `mr-mime`, `u-turn`, `kings-shield`, `life-orb`, `charizard-mega-x`.
  - Resolução: o nome do token é normalizado pela regra do `toID` do Smogon (minúsculas, mantendo só `a-z` e `0-9`) e comparado ao identificador do catálogo. A comparação não diferencia maiúsculas e minúsculas e é permissiva quanto aos hífens: `Life-Orb`, `life-orb` e `lifeorb` resolvem para `Life Orb`.
  - A busca é feita na categoria indicada pela posição sintática (espécie, golpe e, no futuro, item e habilidade).
  - Verificado no catálogo Champions da 0.12.0: nenhuma colisão de `toID` dentro de cada categoria.
  - A saída mostra o nome canônico resolvido, por exemplo `u-turn → U-turn`.
  - Consequência: a regra léxica atual não precisa mudar.
- **Valores padrão do cenário mínimo** (decididos pelo autor): SP 0 em todas as estatísticas e natureza neutra. Como as cinco naturezas neutras (Hardy, Docile, Serious, Bashful e Quirky) produzem as mesmas estatísticas, o adaptador enviará `Serious` de forma explícita, que também é o padrão do motor. A saída deve exibir esses valores como premissas.

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

- [x] Geração definida: Champions. Falta harmonizar o projeto de pesquisa, que ainda diz 9ª geração (Scarlet/Violet).
- [x] Estratégia do catálogo (decisão do autor): um script Node extrai do `@smogon/calc` 0.12.0 um catálogo Champions em **JSON**, com origem, versão e geração nos metadados. O arquivo gerado **é versionado no repositório** e é lido pelo Java na análise semântica. Um teste deve garantir que a cópia versionada coincide com uma nova geração, para evitar divergência.
- [x] Biblioteca JSON do Java (decisão do autor): **Jackson 3.2.4** (`tools.jackson.core:jackson-databind`). Ainda falta adicioná-la ao `pom.xml` com a versão exata. A mesma biblioteca servirá ao contrato JSON com o adaptador na etapa 5.
- [ ] Conferir a licença dos dados redistribuídos (`@smogon/calc` é MIT) e registrar a atribuição.
- [x] Conteúdo do catálogo (decisões do autor):
  - Exportar **todos** os golpes; os nomes dos campos do JSON ficam em **inglês**.
  - A análise semântica rejeita golpes de **status**, com diagnóstico de funcionalidade não suportada.
  - Golpes com **múltiplos acertos são aceitos**. O tratamento fica para depois: pode vir de uma sintaxe para o número de acertos, como `venusaur !bullet-seed(3)`, ou de outra forma a definir. O catálogo guarda `minHits`/`maxHits`, que valem 1/1 nos golpes de um acerto. O caso do Triple Axel (`multiaccuracy`) fica para quando esses golpes forem tratados.
  - A categoria vem da classe `Move` do motor, não do dado bruto. Isso resolve os 86 golpes de status sem `category`.
  - Entradas incompletas (como `Metal Claw`) ficam fora do catálogo e são listadas em `metadata.excluded`, com o motivo.
- [ ] **Golpes cujo dano depende de outros fatores** (tratamento adiado pelo autor; serão tratados separadamente). Exemplos:
  - com poder 0: `Seismic Toss` e `Night Shade` (nível), `Gyro Ball` e `Electro Ball` (velocidades), `Low Kick`, `Grass Knot` e `Heavy Slam` (peso), `Flail` e `Reversal` (HP atual), `Super Fang` (HP do alvo);
  - com poder diferente de 0, mas variável: `Eruption`, `Hex`, `Facade`, `Weather Ball` e `Stored Power`.

  **Decisão provisória do autor:** enquanto não houver tratamento próprio, esses golpes são **rejeitados** pela análise semântica, com diagnóstico de golpe ainda não suportado. A lista explícita será montada na etapa 4; o critério "poder 0" sozinho não basta.

### Achados nos dados do Champions (`@smogon/calc` 0.12.0)

Verificados em 05/11/2026 sobre `Generations.get(0).moves`:

- **86 golpes não têm `category` nos dados brutos.** Todos são de status, com `basePower` 0. A classe `Move` do motor os resolve como `Status`. A regra que define essa categoria padrão (`src/data/moves.ts:5594`, `gen >= 4`) não se aplica ao Champions, cujo número de geração é 0. O script de exportação deve tratar a ausência de categoria explicitamente, e não deixar o campo vazio.
- **`Metal Claw` está incompleto:** só tem `flags`, sem tipo, poder nem categoria. Parece um defeito dos dados da referência (um *patch* do Champions aplicado a um golpe ausente da lista base). O script deve detectar entradas incompletas e decidir de forma documentada como tratá-las, em vez de exportá-las silenciosamente.
- **14 golpes com múltiplos acertos** (campo `multihit`): Bone Rush, Bullet Seed, Double Hit, Dragon Darts, Dual Wingbeat, Icicle Spear, Pin Missile, Population Bomb, Rock Blast, Scale Shot, Tail Slap, Triple Axel, Twin Beam e Water Shuriken.
- Vários golpes de dano fixo ou poder variável têm `basePower` 0 e categoria de dano, como `Seismic Toss`, `Night Shade`, `Super Fang`, `Low Kick`, `Grass Knot`, `Heavy Slam`, `Gyro Ball`, `Flail`, `Reversal` e os golpes de nocaute em um golpe. O dano desses golpes depende de outros fatores (ver pendência acima).
- [x] Política de nomes definida: `toID` permissivo, sem diferenciar maiúsculas e minúsculas (ver "Consequências da escolha do Champions").
- [x] Parâmetros do cenário mínimo: nível fixo em 50 (Champions), SP 0, natureza neutra (`Serious`), sem item, sem clima e habilidade padrão da espécie enviada de forma explícita (ver "Consequências da escolha do Champions").
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

- Sintaxe atual: `charizard !flamethrower -> venusaur` (os testes do lexer e do parser usam `charmander !ember -> caterpie`, que é só um exemplo sintático).
- Manter `!` e `->`; outras decisões de linguagem poderão ser revistas após um protótipo mais concreto.
- Não adicionar itens, clima, habilidades ou atributos personalizados antes de acordar o incremento.
- O motor é `@smogon/calc`, com versão exata no `package.json` e lockfile versionado.
- Não prometer explicação aritmética integral sem verificar o que o motor permite recuperar.
- Comparar resultados com o próprio motor verifica a tradução e a integração, não valida independentemente sua fórmula.

## Pendências de alinhamento acadêmico

- Atualizar referências de implementação com JFlex para a estratégia manual adotada.
- Reescrever objetivos, metodologia, resultados esperados e cronograma do projeto de pesquisa: o Smogon passou de referência independente a motor de execução, então não há mais motor próprio nem testes diferenciais motor-contra-motor. A verificação passa a cobrir a tradução DSL → motor, e não a fórmula em si.
- Trocar o separador `x` por `->` no esboço de sintaxe do projeto de pesquisa.
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

- Em 05/11/2026, `nix develop -c mvn verify` (OpenJDK 21.0.12, Maven 3.10.0, Node.js 24.21.0, nixpkgs fixado em `flake.lock`): **BUILD SUCCESS**, 25 testes executados, sem falhas, erros ou testes ignorados.
- `npm ci` em `motor-smogon/` instalou `@smogon/calc` 0.12.0 sem vulnerabilidades reportadas.
- Atualização local de `PROGRESSO.md`, `README.md` e `.gitignore`, mais criação de `docs/formula-dano-champions.md`, ainda sem commit ou push.
