# DSL para cenários de cálculo de dano Pokémon

Implementação do artefato do TCC: uma linguagem específica de domínio (DSL) textual para descrever cenários de cálculo de dano Pokémon, acompanhada de lexer, parser, análise semântica e integração ao motor `@smogon/calc`.

O foco do trabalho é o processamento da linguagem. A fórmula de dano é executada pelo motor existente, não por uma calculadora própria.

## Continuidade do desenvolvimento

Consulte [PROGRESSO.md](PROGRESSO.md) para o estado atual, checklist das etapas, próxima tarefa e instruções de colaboração para outros agentes. Esse arquivo deve ser atualizado a cada incremento.

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

**Estado: sintaxe mínima implementada de ponta a ponta (lexer, parser, análise semântica, integração com o motor e CLI).** A especificação continua provisória; as decisões pendentes abaixo precisam ser resolvidas antes de considerá-la completa.

### Objetivo

Descrever um cenário isolado com um atacante, um golpe e um defensor. A DSL descreve entradas para o cálculo; não permite definir novas fórmulas ou regras de batalha e não simula uma batalha com múltiplos turnos.

### Exemplo mínimo

```txt
charizard !flamethrower -> venusaur
```

| Elemento | Significado |
| --- | --- |
| `charizard` | Pokémon atacante |
| `!` | Marcador do golpe |
| `flamethrower` | Nome do golpe |
| `->` | Separador direcionado do atacante para o defensor |
| `venusaur` | Pokémon defensor |

A geração adotada é **Pokémon Champions** (`@smogon/calc` 0.12.0, `Generations.get(0)`). A fórmula de dano correspondente está em [docs/formula-dano-champions.md](docs/formula-dano-champions.md).

### Uso da CLI

```bash
mvn package                 # gera target/pokemon-dsl-0.1.0-SNAPSHOT.jar
(cd motor-smogon && npm ci) # instala o @smogon/calc, se ainda não instalado
./pokedsl "charizard !flamethrower -> venusaur"
```

```txt
0 SpA Charizard Flamethrower vs. 0 HP / 0 SpD Venusaur: 110-132 (70.9 - 85.1%) -- guaranteed 2HKO
(110, 110, 114, 114, 116, 116, 120, 120, 120, 122, 122, 126, 126, 128, 128, 132)

Premissas: geração Champions, nível 50, @smogon/calc 0.12.0
  Charizard: Blaze (habilidade padrão), natureza Serious, SP 0, sem item
  Venusaur: Overgrow (habilidade padrão), natureza Serious, SP 0, sem item
  campo: sem clima
```

- A primeira linha é a descrição produzida pelo motor, no formato usual das calculadoras da comunidade. Em seguida vêm as 16 rolagens de dano (uma linha por acerto, nos golpes de vários acertos) e as premissas aplicadas.
- Erros são escritos na saída de erros. Os erros semânticos sublinham o nome que os causou:

  ```txt
  Erro semântico: Espécie desconhecida no catálogo Champions: 'caterpie'
    charizard !flamethrower -> caterpie
                               ^^^^^^^^
  ```
- Códigos de saída: `0` sucesso, `1` cenário inválido (léxico, sintático ou semântico), `2` uso incorreto, `3` falha no motor.
- O script `./pokedsl` executa o jar com o caminho do adaptador (`motor-smogon/calcular.js`). Sem o script, use `java -Dpokedsl.adaptador=motor-smogon/calcular.js -jar target/pokemon-dsl-0.1.0-SNAPSHOT.jar "<cenário>"`. O comando `node` precisa estar no PATH.

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
- Preservam o texto original, incluindo maiúsculas e minúsculas. Na análise semântica, o nome é normalizado como o `toID` do Smogon (minúsculas, mantendo só letras e dígitos) e comparado ao identificador do catálogo. Portanto `Life-Orb`, `life-orb` e `lifeorb` são equivalentes. A forma recomendada é a minúscula com hífen no lugar de espaços, como `u-turn`, `mr-mime` e `life-orb`.

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

#### Regras semânticas do cenário mínimo

- Atacante e defensor devem existir entre as espécies do catálogo, e o golpe entre os golpes, com a resolução de nomes descrita em "Nomes".
- Golpes de status são rejeitados, porque não causam dano.
- Golpes cujo dano depende de outros fatores são rejeitados provisoriamente, com a mensagem "ainda não suportado": todos os golpes de dano com poder 0 (como `seismic-toss`, `low-kick` e `gyro-ball`) e 14 golpes cujo poder o motor recalcula (`acrobatics`, `assurance`, `barb-barrage`, `eruption`, `hex`, `infernal-parade`, `payback`, `power-trip`, `rising-voltage`, `stored-power`, `terrain-pulse`, `triple-axel`, `water-spout` e `weather-ball`).
- Padrões aplicados e exibidos nas premissas: habilidade padrão da espécie, natureza `Serious`, SP 0, sem item e sem clima. Golpes de vários acertos usam a contagem fixa (por exemplo, Dual Wingbeat 2). Quando o número de acertos varia, usa-se o mínimo + 1 (Bullet Seed, de 2 a 5, usa 3). Essa última regra é provisória, até existir sintaxe para a contagem.

A existência de uma entidade e a legalidade competitiva de uma combinação são verificações distintas. A checagem de que um Pokémon aprende determinado golpe não faz parte do marco mínimo proposto.

### Exemplos para orientar os testes

| Entrada | Resultado esperado |
| --- | --- |
| `charizard !flamethrower -> venusaur` | Cenário válido: 110-132 (70.9 - 85.1%) |
| `venusaur !bullet-seed -> charizard` | Cenário válido, com 3 acertos (mínimo + 1) |
| `charizard !nasty-plot -> venusaur` | Erro semântico: golpe de status |
| `charizard !seismic-toss -> venusaur` | Erro semântico: golpe ainda não suportado |
| `charmander !ember -> caterpie` | Estrutura sintática válida; erro semântico, pois as entidades não existem no Champions |
| `charmander !golpeinexistente -> caterpie` | Estrutura sintática válida; erro semântico por golpe inexistente |
| `charmander ember -> caterpie` | Erro sintático: falta o marcador `!` |
| `charmander !ember caterpie` | Erro sintático: falta o separador `->` |
| `charmander !ember ->` | Erro sintático: falta o defensor |
| `charmander !ember -> caterpie extra` | Erro sintático: conteúdo adicional após o defensor |
| entrada vazia | Erro sintático: cenário ausente |

### Decisões pendentes

Antes da implementação completa, definir e registrar:

- revisão futura das regras léxicas provisórias, incluindo nomes com espaços e pontuação fora do subconjunto atual;
- valores padrão de condições relevantes. Já decididos: nível 50 (fixo no Champions), SP 0, natureza neutra (`Serious`) e, enquanto não houver sintaxe, a habilidade padrão da espécie;
- comportamento diante de efeitos implícitos ou padrões do motor que ultrapassem o escopo pretendido. Por exemplo, a habilidade padrão de `charizard-mega-y` é Drought, mas o motor **não** ativa o sol sozinho: o cálculo usa "sem clima", como declarado nas premissas;
- localização dos erros léxicos e sintáticos na CLI. Hoje só os erros semânticos sublinham o trecho; os demais informam a posição na mensagem.

Nenhum valor padrão deve depender apenas de uma decisão silenciosa da biblioteca. Os padrões utilizados precisam ser documentados e enviados explicitamente pelo adaptador quando a API permitir.

### Fora do primeiro marco

- Sintaxe para itens, habilidades, clima, terreno e atributos personalizados.
- Múltiplos atacantes, defensores ou golpes por entrada.
- Simulação de turnos e batalhas completas.
- Definição de novas regras ou fórmulas pela DSL.
- Interface gráfica e sugestões inteligentes.
- Explicação integral dos passos aritméticos internos do motor.

A ausência de sintaxe para um modificador não garante sua ausência no cálculo: os efeitos e padrões do motor deverão ser conferidos ao definir a semântica.

## Catálogo de dados

A análise semântica consulta um catálogo em JSON (`src/main/resources/catalogo-champions.json`), extraído do `@smogon/calc` pelo script `motor-smogon/exportar-catalogo.js` e versionado no repositório. Formato:

```json
{
  "metadata": {
    "source": "@smogon/calc",
    "version": "0.12.0",
    "generation": "champions",
    "excluded": [
      { "kind": "move", "name": "Metal Claw", "reason": "entrada incompleta: sem type e basePower" }
    ]
  },
  "species": [
    {
      "id": "charizard",
      "name": "Charizard",
      "types": ["Fire", "Flying"],
      "baseStats": { "hp": 78, "atk": 84, "def": 78, "spa": 109, "spd": 85, "spe": 100 },
      "defaultAbility": "Blaze"
    }
  ],
  "moves": [
    { "id": "flamethrower", "name": "Flamethrower", "type": "Fire", "category": "Special", "basePower": 90, "minHits": 1, "maxHits": 1 },
    { "id": "bulletseed", "name": "Bullet Seed", "type": "Grass", "category": "Physical", "basePower": 25, "minHits": 2, "maxHits": 5 }
  ]
}
```

- `id` é o identificador normalizado do motor (`toID`); `name` é o nome canônico exibido na saída.
- `category` vem da classe `Move` do motor, e não do dado bruto, que omite a categoria dos golpes de status no Champions.
- `defaultAbility` é a primeira habilidade da espécie no catálogo.
- Golpes de um acerto têm `minHits` e `maxHits` iguais a 1.
- Golpes sem `type` ou sem `basePower` nos dados do motor são considerados incompletos: ficam fora do catálogo e são listados em `metadata.excluded`, com o motivo. A falta de `category`, sozinha, não torna o golpe incompleto, porque a classe `Move` a resolve.
- O marcador interno `(No Move)` do motor não é exportado.
- As listas são ordenadas por `id` (e `excluded` por `name`) e o arquivo não contém data de geração. Assim, a mesma versão do motor sempre produz o mesmo arquivo.

Para gerar o catálogo novamente, ou conferir se a cópia versionada está atualizada (por exemplo, depois de mudar a versão do motor):

```bash
cd motor-smogon
npm run exportar-catalogo    # grava o arquivo
npm run verificar-catalogo   # não grava; termina com erro se o arquivo estiver desatualizado
```

Na versão 0.12.0, o catálogo tem 359 espécies e 514 golpes, com 11 golpes excluídos.

## Reprodutibilidade e limitações

A integração deverá registrar a geração adotada, a versão do motor e a fonte dos dados efetivamente utilizados. Caso seja utilizado o catálogo distribuído com `@smogon/calc`, isso deverá ser declarado explicitamente. A versão do pacote, sozinha, não substitui a definição do subconjunto suportado.

O motor será tratado como implementação de referência comunitária, não como especificação oficial completa dos jogos. A avaliação deverá separar a correção do parser, da validação semântica e da tradução para o motor da correção interna do cálculo fornecido pela dependência.

## Verificação do ambiente

Instale primeiro as dependências Node.js a partir do lockfile, porque os testes de integração executam o adaptador real:

```bash
cd motor-smogon
npm ci
```

Depois, na raiz do repositório:

```bash
mvn verify
```

A suíte Java cobre tokens, lexer, parser, catálogo, análise semântica, integração com o motor (reproduz o exemplo verificado da fórmula) e a CLI de ponta a ponta.
