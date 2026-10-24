# DSL para cenários de cálculo de dano Pokémon

Implementação do artefato do TCC: uma linguagem específica de domínio (DSL) textual para descrever cenários de cálculo de dano Pokémon, acompanhada de lexer, parser, análise semântica e integração ao motor `@smogon/calc`.

O foco do trabalho é o processamento da linguagem. A fórmula de dano é executada pelo motor existente, não por uma calculadora própria.

## Tecnologias e arquitetura

- Java 21 para processamento da DSL.
- Maven para compilação e gerenciamento de dependências.
- JUnit 5 para testes.
- Parser descendente recursivo manual.
- Node.js para o adaptador do motor.
- `@smogon/calc` com versão exata registrada em `motor-smogon/package.json` e dependências resolvidas em `motor-smogon/package-lock.json`.

```txt
Texto da DSL
    ↓
Lexer → tokens com localização
    ↓
Parser → AST
    ↓
Resolução de nomes e validação semântica
    ↓
Cenário normalizado, independente do motor
    ↓ JSON
Adaptador Node.js → @smogon/calc
    ↓ JSON
Resultado
```

A interpretação da DSL pertence ao código Java. O adaptador não deve interpretar a sintaxe nem decidir silenciosamente valores padrão do cenário.

## Especificação inicial da DSL

**Estado: proposta mínima em definição, ainda não implementada.** As decisões pendentes abaixo precisam ser resolvidas antes de considerar esta especificação completa.

### Objetivo

Descrever um cenário isolado com um atacante, um golpe e um defensor. A DSL descreve entradas para o cálculo; não permite definir novas fórmulas ou regras de batalha e não simula uma batalha com múltiplos turnos.

### Exemplo mínimo

```txt
charmander !ember -> caterpie
```

| Elemento | Significado |
| --- | --- |
| `charmander` | Pokémon atacante |
| `!` | Marcador do golpe |
| `ember` | Nome do golpe |
| `->` | Separador direcionado do atacante para o defensor |
| `caterpie` | Pokémon defensor |

### Tokens previstos

| Token conceitual | Função |
| --- | --- |
| `NOME` | Identificar um Pokémon ou golpe |
| `!` | Introduzir o golpe |
| `->` | Separar atacante e defensor |
| `EOF` | Indicar o fim da entrada |

Os tipos de token adotados no protótipo são `NAME`, `BANG`, `ARROW` e `EOF`, respectivamente. A seta `->` deve ser reconhecida como um único token, composto por dois caracteres consecutivos.

### Regras léxicas do protótipo

Estas regras são provisórias e delimitam o primeiro incremento. Não representam suporte completo aos nomes das entidades Pokémon.

#### Nomes

- Começam obrigatoriamente com uma letra ASCII (`a-z` ou `A-Z`).
- Depois da primeira letra, aceitam letras ASCII e dígitos (`0-9`).
- Aceitam hífens entre partes alfanuméricas, como em `mr-mime` e `porygon-z`.
- Não aceitam hífens iniciais, finais ou consecutivos.
- Não incluem espaços, acentos ou outros sinais de pontuação.
- Preservam o texto original, incluindo maiúsculas e minúsculas. A política de comparação e resolução semântica dos nomes permanece pendente.

Uma expressão regular equivalente para a formação dos nomes é:

```regex
[A-Za-z][A-Za-z0-9]*(?:-[A-Za-z0-9]+)*
```

`charmander`, `Charmander`, `mr-mime` e `porygon2` são nomes lexicalmente válidos. `2porygon`, `mr-` e `mr--mime` devem causar erro léxico.

O lexer não consulta o catálogo: um nome bem formado, mas inexistente no domínio, continua sendo um token `NAME`. A existência e a disponibilidade na geração adotada são verificadas pela análise semântica.

#### Espaços e marcadores

- Caracteres reconhecidos por `Character.isWhitespace(char)` do Java são ignorados entre tokens e no início e no fim da entrada, incluindo espaços comuns, tabulações e quebras de linha. Isso não equivale a aceitar todo caractere Unicode visualmente semelhante a um espaço.
- Espaços separam nomes: `mr mime` produz dois tokens `NAME`, não um nome composto.
- `!` produz um token `BANG`.
- `->` produz um token `ARROW`; seus dois caracteres devem ser consecutivos. `- >` é inválido.
- Não são obrigatórios espaços em torno dos marcadores: `charmander!ember->caterpie` pode ser tokenizado.
- Um hífen só integra um nome quando seguido de letra ASCII ou dígito. Em `ember->caterpie`, o nome termina antes do hífen, permitindo reconhecer a seta na sequência.
- Caracteres não reconhecidos causam erro léxico; no protótipo, esse erro é comunicado por `IllegalArgumentException` com a posição do caractere.

Reconhecimento léxico não implica validade sintática: `!->` pode ser tokenizado, mas não constitui um cenário válido.

#### Localização e fim da entrada

Cada token registra o tipo, o texto reconhecido (`lexeme`) e o intervalo `[startOffset, endOffset)`: início inclusivo e fim exclusivo, com índices a partir de zero nas unidades UTF-16 de uma `String` Java.

O lexer acrescenta um token `EOF` de texto vazio, com ambos os offsets iguais a `source.length()`, inclusive para uma entrada vazia ou composta apenas por espaços.

### Gramática inicial

A gramática abaixo descreve a sequência de tokens, não as regras de formação dos nomes nem o tratamento de espaços:

```ebnf
cenario = nome, "!", nome, "->", nome, EOF ;
```

`nome` corresponde a um token `NOME`. `EOF` exige que não existam tokens adicionais após o defensor.

### Responsabilidades das etapas

- **Lexer:** reconhecer os tokens e registrar suas posições no texto.
- **Parser:** verificar a estrutura e construir a AST, sem consultar o catálogo de Pokémon ou golpes.
- **Análise semântica:** resolver nomes e verificar se as entidades e os valores são válidos no escopo adotado.
- **Adaptador:** converter o cenário validado para os objetos do motor e devolver o resultado.

A existência de uma entidade e a legalidade competitiva de uma combinação são verificações distintas. A checagem de que um Pokémon aprende determinado golpe não faz parte do marco mínimo proposto.

### Exemplos para orientar os testes

| Entrada | Resultado esperado |
| --- | --- |
| `charmander !ember -> caterpie` | Estrutura sintática válida; validação semântica depende da geração escolhida |
| `charmander !golpeinexistente -> caterpie` | Estrutura sintática válida; erro semântico por golpe inexistente |
| `charmander ember -> caterpie` | Erro sintático: falta o marcador `!` |
| `charmander !ember caterpie` | Erro sintático: falta o separador `->` |
| `charmander !ember ->` | Erro sintático: falta o defensor |
| `charmander !ember -> caterpie extra` | Erro sintático: conteúdo adicional após o defensor |
| entrada vazia | Erro sintático: cenário ausente |

### Decisões pendentes

Antes da implementação completa, definir e registrar:

- geração e conjunto de regras adotados;
- revisão futura das regras léxicas provisórias, incluindo nomes com espaços e pontuação fora do subconjunto atual;
- política de comparação e resolução semântica de nomes quanto a maiúsculas e minúsculas;
- valores padrão de nível, atributos, itens, habilidades e condições relevantes;
- comportamento diante de efeitos implícitos ou padrões do motor que ultrapassem o escopo pretendido;
- formato de localização dos erros e estrutura do resultado.

Nenhum valor padrão deve depender apenas de uma decisão silenciosa da biblioteca. Os padrões utilizados precisam ser documentados e enviados explicitamente pelo adaptador quando a API permitir.

### Fora do primeiro marco

- Sintaxe para itens, habilidades, clima, terreno e atributos personalizados.
- Múltiplos atacantes, defensores ou golpes por entrada.
- Simulação de turnos e batalhas completas.
- Definição de novas regras ou fórmulas pela DSL.
- Interface gráfica e sugestões inteligentes.
- Explicação integral dos passos aritméticos internos do motor.

A ausência de sintaxe para um modificador não garante sua ausência no cálculo: os efeitos e padrões do motor deverão ser conferidos ao definir a semântica.

## Reprodutibilidade e limitações

A integração deverá registrar a geração adotada, a versão do motor e a fonte dos dados efetivamente utilizados. Caso seja utilizado o catálogo distribuído com `@smogon/calc`, isso deverá ser declarado explicitamente. A versão do pacote, sozinha, não substitui a definição do subconjunto suportado.

O motor será tratado como implementação de referência comunitária, não como especificação oficial completa dos jogos. A avaliação deverá separar a correção do parser, da validação semântica e da tradução para o motor da correção interna do cálculo fornecido pela dependência.

## Verificação do ambiente

Na pasta `implementacao/`, execute:

```bash
mvn verify
```

Para instalar as dependências Node.js a partir do lockfile:

```bash
cd motor-smogon
npm ci
```

O teste Java inicial verifica apenas o funcionamento do ambiente, não a correção da DSL. Os testes do adaptador Node.js ainda precisam ser implementados.
