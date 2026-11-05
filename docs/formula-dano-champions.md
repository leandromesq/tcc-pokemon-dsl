# Fórmula de dano adotada: Pokémon Champions no `@smogon/calc` 0.12.0

Este documento descreve em forma matemática o cálculo que o motor executa para um golpe de dano padrão no modo Champions (`Generations.get(0)`). Ele foi extraído do código-fonte da versão fixada (`motor-smogon/node_modules/@smogon/calc/src`) e conferido com execuções do próprio motor.

> **Fonte e independência.** As expressões abaixo descrevem a implementação, não uma especificação oficial do jogo. O núcleo (dano-base, rolagens, STAB, efetividade e modificadores) coincide com a descrição comunitária da 5ª geração em diante (Bulbapedia, *Damage*), o que dá uma fonte parcialmente independente para os casos-base. As regras próprias do Champions (estatísticas e nível) só puderam ser conferidas no código do motor.

## Notação

- $\lfloor x \rfloor$: piso (truncamento para baixo).
- $\operatorname{pr}(x)$: arredondamento do motor (`pokeRound`). Arredonda para o inteiro mais próximo; exatamente $,5$ vai **para baixo**:
  $$\operatorname{pr}(x) = \begin{cases} \lceil x \rceil & \text{se } x - \lfloor x \rfloor > 0{,}5 \\ \lfloor x \rfloor & \text{caso contrário} \end{cases}$$
- Os modificadores são inteiros na escala de 4096, ou seja, $m/4096$. Por exemplo, $6144 = 1{,}5\times$, $2048 = 0{,}5\times$, $4915 \approx 1{,}2\times$, $5324 \approx 1{,}3\times$.
- Os *overflows* de 16 e 32 bits do motor (`OF16`, `OF32`) foram omitidos porque não ocorrem no recorte com nível 50.

## 1. Estatísticas (próprio do Champions)

Fonte: `stats.ts`, `calcStatChampions`. O nível é sempre $L = 50$ (`pokemon.ts:59` ignora outro valor), não há IVs, e os EVs são substituídos por pontos de estatística $SP$:

$$HP = B_{hp} + SP_{hp} + 75 \qquad (\text{exceto } B_{hp} = 1 \Rightarrow HP = 1)$$

$$X = \left\lfloor N_X \cdot (B_X + SP_X + 20) \right\rfloor, \quad X \in \{atk, def, spa, spd, spe\},\; N_X \in \{0{,}9;\ 1;\ 1{,}1\}$$

em que $B$ é a estatística-base da espécie e $N_X$ é o fator da natureza.

**Relação com a 9ª geração.** Com $L = 50$, $IV = 31$ e $EV = 8\,SP$, a fórmula da 3ª geração em diante se reduz exatamente a essas expressões. Por exemplo, para $X \ne hp$: $\lfloor (2B + 31 + 2SP)\cdot 50/100 \rfloor + 5 = B + SP + 20$, já que $2B + 2SP + 31$ é ímpar. Isso permite justificar a regra do Champions a partir da fórmula documentada da 9ª geração.

**Limites de SP** (informados pelo autor): $0 \le SP_X \le 32$ para cada estatística, incluindo HP, e $\sum_X SP_X \le 66$.

> O motor **não valida** esses limites: aceitou $SP = 40$ sem erro. A validação cabe à análise semântica da DSL.

## 2. Poder, ataque e defesa efetivos

Fonte: `champions.ts:652`, `:887`, `:1013`. Cada um aplica a cadeia dos modificadores pertinentes (item, habilidade e outros):

$$P = \max\!\left(1,\ \operatorname{pr}\!\left(\frac{P_0 \cdot M_P}{4096}\right)\right),\quad A = \max\!\left(1,\ \operatorname{pr}\!\left(\frac{A_0 \cdot M_A}{4096}\right)\right),\quad D = \max\!\left(1,\ \operatorname{pr}\!\left(\frac{D_0 \cdot M_D}{4096}\right)\right)$$

$A_0$ e $D_0$ são as estatísticas da seção 1, já com os estágios de alteração aplicados. Golpes físicos usam $atk/def$; especiais usam $spa/spd$.

**Encadeamento de modificadores** (`chainMods`, `util.ts:471`): começando em $M = 4096$, para cada modificador $m \ne 4096$,

$$M \leftarrow \left\lfloor \frac{M \cdot m + 2048}{4096} \right\rfloor$$

limitado a um intervalo: $[41,\ 2097152]$ para o poder e $[410,\ 131072]$ para ataque e defesa.

## 3. Dano-base

Fonte: `util.ts:481`, `getBaseDamage`. É a mesma expressão do projeto de pesquisa, com $L = 50$, de modo que $\lfloor 2L/5 + 2 \rfloor = 22$:

$$D_b = \left\lfloor \frac{\left\lfloor \dfrac{\lfloor 2L/5 + 2 \rfloor \cdot P \cdot A}{D} \right\rfloor}{50} \right\rfloor + 2$$

Em seguida, nesta ordem (`champions.ts:1045`), quando aplicáveis:

1. Alvo múltiplo (apenas em duplas): $D_b \leftarrow \operatorname{pr}(D_b \cdot 3072/4096)$.
2. **Clima**: sol com golpe de Fogo ou chuva com golpe de Água dá $D_b \leftarrow \operatorname{pr}(D_b \cdot 6144/4096)$. Sol com Água ou chuva com Fogo dá $D_b \leftarrow \operatorname{pr}(D_b \cdot 2048/4096)$.
3. Acerto crítico: $D_b \leftarrow \lfloor 1{,}5\, D_b \rfloor$. Fica fora do recorte.

## 4. Rolagens e modificadores finais

Fonte: `util.ts:541`, `getFinalDamage`. Para cada $i = 0, \dots, 15$:

$$R_i = \left\lfloor \frac{D_b\,(85 + i)}{100} \right\rfloor$$

$$S_i = \operatorname{pr}\!\left(\frac{R_i \cdot \text{STAB}}{4096}\right), \qquad \text{STAB} = \begin{cases} 6144 & \text{se o tipo do golpe é um dos tipos do atacante} \\ 4096 & \text{caso contrário} \end{cases}$$

$$T_i = \lfloor S_i \cdot E \rfloor, \qquad E \in \{0;\ 0{,}25;\ 0{,}5;\ 1;\ 2;\ 4\}\ \text{(efetividade de tipo)}$$

Se o atacante estiver queimado e o golpe for físico, $T_i \leftarrow \lfloor T_i / 2 \rfloor$. Fica fora do recorte.

$$\text{Dano}_i = \max\!\left(1,\ \operatorname{pr}\!\left(\frac{T_i \cdot M_F}{4096}\right)\right)$$

$M_F$ é a cadeia dos modificadores finais (telas, Life Orb $= 5324$, Expert Belt $= 4915$, frutas de resistência, algumas habilidades), limitada a $[41,\ 131072]$. Se $E = 0$, o motor devolve dano 0 antes de chegar a este ponto.

**Percentual:** $\text{Dano}_i / HP_{\text{defensor}} \cdot 100$.

## 5. Exemplo verificado: `charizard !flamethrower -> venusaur`

Premissas explícitas: habilidade nenhuma (`No Ability`), natureza neutra (`Serious`), $SP = 0$, sem item, sem clima.

| Parâmetro | Origem | Valor |
| --- | --- | --- |
| $L$ | fixo no Champions | 50 |
| $P$ | Flamethrower, especial, Fogo | 90 |
| $A$ | spa do Charizard: $\lfloor 1 \cdot (109 + 0 + 20) \rfloor$ | 129 |
| $D$ | spd do Venusaur: $\lfloor 1 \cdot (100 + 0 + 20) \rfloor$ | 120 |
| $HP$ | Venusaur: $80 + 0 + 75$ | 155 |
| STAB | Charizard é Fogo/Voador | 6144 (1,5) |
| $E$ | Fogo contra Planta/Veneno | 2 |

$$D_b = \left\lfloor \frac{\lfloor 22 \cdot 90 \cdot 129 / 120 \rfloor}{50} \right\rfloor + 2 = \left\lfloor \frac{\lfloor 2128{,}5 \rfloor}{50} \right\rfloor + 2 = \lfloor 42{,}56 \rfloor + 2 = 44$$

- Rolagem mínima ($i = 0$): $R_0 = \lfloor 44 \cdot 0{,}85 \rfloor = 37$, $S_0 = \operatorname{pr}(55{,}5) = 55$, $T_0 = 110$.
- Rolagem máxima ($i = 15$): $R_{15} = 44$, $S_{15} = 66$, $T_{15} = 132$.

Resultado do motor: `110–132 (70.9 – 85.1%)`, com vetor
`[110,110,114,114,116,116,120,120,120,122,122,126,126,128,128,132]`. Os extremos conferem com a derivação manual.

Variações também conferidas à mão contra o motor:

- **Sol** (`~sun`): $D_b = \operatorname{pr}(44 \cdot 1{,}5) = 66$; mínimo $\lfloor 66 \cdot 0{,}85 \rfloor = 56 \to 84 \to 168$; máximo $66 \to 99 \to 198$. Motor: `168–198`.
- **Charcoal** (`@charcoal`, poder $\times 4915$): $P = \operatorname{pr}(90 \cdot 4915/4096) = \operatorname{pr}(107{,}99) = 108$; $D_b = \lfloor \lfloor 22 \cdot 108 \cdot 129/120 \rfloor / 50 \rfloor + 2 = 53$; mínimo $45 \to \operatorname{pr}(67{,}5) = 67 \to 134$; máximo $53 \to 79 \to 158$. Motor: `134–158`.

## Diferenças em relação ao modo da 9ª geração (`gen789.ts`)

Identificadas por `diff` entre `mechanics/gen789.ts` e `mechanics/champions.ts`:

- Estatísticas por $SP$ em vez de IV/EV, e nível fixo em 50 (seção 1).
- Não há Terastalização, Dynamax, golpes Z nem Stellar.
- Não existe Utility Umbrella, e os climas extremos não anulam golpes de Fogo ou Água.
- O conjunto de habilidades, itens e golpes é menor. Há algumas habilidades novas, como `Dragonize`, `Mega Sol` e `Aura Guard`.
- Dano-base, rolagens, STAB, efetividade e encadeamento de modificadores são **idênticos**.

## Implicações para a DSL

- Se nenhuma habilidade for informada, o motor coloca a habilidade padrão da espécie (o Charizard recebe `Blaze`). O adaptador deve sempre enviar a habilidade de forma explícita.
- O motor não valida $SP$. A análise semântica deve checar $0 \le SP \le 32$ por estatística e no máximo 66 no total.
- Catálogo do Champions na 0.12.0: 359 espécies, 526 golpes e 166 itens. `charmander`, `caterpie` e `ember` não existem nele.
