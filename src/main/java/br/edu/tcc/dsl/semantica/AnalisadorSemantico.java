package br.edu.tcc.dsl.semantica;

import java.util.Set;

import br.edu.tcc.dsl.catalogo.Catalogo;
import br.edu.tcc.dsl.catalogo.Especie;
import br.edu.tcc.dsl.catalogo.Golpe;
import br.edu.tcc.dsl.lexer.Token;
import br.edu.tcc.dsl.parser.Cenario;

/**
 * Resolve os nomes do cenário no catálogo, valida o subconjunto suportado e
 * aplica os padrões do cenário mínimo, que ficam explícitos no resultado.
 */
public final class AnalisadorSemantico {

    /** Natureza neutra enviada explicitamente ao motor. */
    public static final String NATUREZA_PADRAO = "Serious";

    /**
     * Golpes com poder-base maior que 0, mas que o motor recalcula a partir de
     * outros fatores (HP, status, item, campo, ordem dos turnos...). Os golpes
     * de poder 0 são rejeitados por regra geral. Levantado em
     * {@code champions.ts} (calculateBasePowerChampions) do @smogon/calc 0.12.0.
     */
    static final Set<String> GOLPES_PODER_VARIAVEL = Set.of(
            "acrobatics", "assurance", "barbbarrage", "eruption", "hex",
            "infernalparade", "payback", "powertrip", "risingvoltage",
            "storedpower", "terrainpulse", "tripleaxel", "waterspout",
            "weatherball"
    );

    private final Catalogo catalogo;

    public AnalisadorSemantico(Catalogo catalogo) {
        this.catalogo = catalogo;
    }

    public CenarioResolvido analisar(Cenario cenario) {
        Especie atacante = resolverEspecie(cenario.atacante());
        Golpe golpe = resolverGolpe(cenario.golpe());
        Especie defensor = resolverEspecie(cenario.defensor());

        return new CenarioResolvido(
                combatentePadrao(atacante),
                golpe,
                acertosPadrao(golpe),
                combatentePadrao(defensor)
        );
    }

    private Especie resolverEspecie(Token token) {
        return catalogo.buscarEspecie(token.lexeme()).orElseThrow(() -> new ErroSemantico(
                "Espécie desconhecida no catálogo Champions: '" + token.lexeme() + "'", token));
    }

    private Golpe resolverGolpe(Token token) {
        Golpe golpe = catalogo.buscarGolpe(token.lexeme()).orElseThrow(() -> new ErroSemantico(
                "Golpe desconhecido no catálogo Champions: '" + token.lexeme() + "'", token));

        if (golpe.categoria().equals("Status")) {
            throw new ErroSemantico(
                    "Golpe de status não causa dano: '" + golpe.nome() + "'", token);
        }
        if (golpe.poderBase() == 0 || GOLPES_PODER_VARIAVEL.contains(golpe.id())) {
            throw new ErroSemantico(
                    "Golpe ainda não suportado (dano depende de outros fatores): '"
                            + golpe.nome() + "'", token);
        }
        return golpe;
    }

    private static Combatente combatentePadrao(Especie especie) {
        return new Combatente(especie, especie.habilidadePadrao(), NATUREZA_PADRAO);
    }

    /**
     * Número de acertos enviado ao motor. Contagem fixa: o próprio número.
     * Intervalo sem contagem informada: mínimo + 1 (decisão provisória do
     * autor, igual ao padrão implícito do motor; ex.: Bullet Seed 2-5 -> 3).
     */
    static int acertosPadrao(Golpe golpe) {
        if (golpe.minAcertos() == golpe.maxAcertos()) {
            return golpe.minAcertos();
        }
        return golpe.minAcertos() + 1;
    }
}
