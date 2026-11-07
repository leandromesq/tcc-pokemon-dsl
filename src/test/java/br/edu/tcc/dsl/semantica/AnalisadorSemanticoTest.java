package br.edu.tcc.dsl.semantica;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.edu.tcc.dsl.catalogo.Catalogo;
import br.edu.tcc.dsl.lexer.Lexer;
import br.edu.tcc.dsl.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalisadorSemanticoTest {

    private static Catalogo catalogo;
    private static AnalisadorSemantico analisador;

    @BeforeAll
    static void preparar() {
        catalogo = Catalogo.carregar();
        analisador = new AnalisadorSemantico(catalogo);
    }

    private static CenarioResolvido analisar(String fonte) {
        return analisador.analisar(Parser.parse(Lexer.tokenize(fonte)));
    }

    @Test
    void deveResolverCenarioMinimoComPadroesExplicitos() {
        CenarioResolvido cenario = analisar("charizard !flamethrower -> venusaur");

        assertEquals("Charizard", cenario.atacante().especie().nome());
        assertEquals("Blaze", cenario.atacante().habilidade());
        assertEquals("Serious", cenario.atacante().natureza());
        assertEquals("Flamethrower", cenario.golpe().nome());
        assertEquals(1, cenario.acertos());
        assertEquals("Venusaur", cenario.defensor().especie().nome());
        assertEquals("Overgrow", cenario.defensor().habilidade());
    }

    @Test
    void deveAceitarNomesComMaiusculasEHifens() {
        CenarioResolvido cenario = analisar("Mr-Mime !U-Turn -> charizard-mega-x");

        assertEquals("Mr. Mime", cenario.atacante().especie().nome());
        assertEquals("U-turn", cenario.golpe().nome());
        assertEquals("Charizard-Mega-X", cenario.defensor().especie().nome());
    }

    @Test
    void deveRejeitarEspecieDesconhecidaApontandoOToken() {
        ErroSemantico erro = assertThrows(ErroSemantico.class,
                () -> analisar("charmander !flamethrower -> venusaur"));

        assertTrue(erro.getMessage().contains("Espécie desconhecida"));
        assertEquals("charmander", erro.token().lexeme());
        assertEquals(0, erro.token().startOffset());
    }

    @Test
    void deveRejeitarDefensorDesconhecido() {
        ErroSemantico erro = assertThrows(ErroSemantico.class,
                () -> analisar("charizard !flamethrower -> caterpie"));

        assertEquals("caterpie", erro.token().lexeme());
        assertEquals(27, erro.token().startOffset());
    }

    @Test
    void deveRejeitarGolpeDesconhecido() {
        ErroSemantico erro = assertThrows(ErroSemantico.class,
                () -> analisar("charizard !ember -> venusaur"));

        assertTrue(erro.getMessage().contains("Golpe desconhecido"));
        assertEquals("ember", erro.token().lexeme());
    }

    @Test
    void deveRejeitarGolpeExcluidoDoCatalogo() {
        assertThrows(ErroSemantico.class, () -> analisar("charizard !metal-claw -> venusaur"));
    }

    @Test
    void deveRejeitarGolpeDeStatus() {
        ErroSemantico erro = assertThrows(ErroSemantico.class,
                () -> analisar("charizard !nasty-plot -> venusaur"));

        assertTrue(erro.getMessage().contains("status"));
    }

    @Test
    void deveRejeitarGolpeDePoderZero() {
        ErroSemantico erro = assertThrows(ErroSemantico.class,
                () -> analisar("charizard !seismic-toss -> venusaur"));

        assertTrue(erro.getMessage().contains("ainda não suportado"));
    }

    @Test
    void deveRejeitarGolpeDePoderVariavel() {
        ErroSemantico erro = assertThrows(ErroSemantico.class,
                () -> analisar("charizard !weather-ball -> venusaur"));

        assertTrue(erro.getMessage().contains("ainda não suportado"));
    }

    @Test
    void listaDePoderVariavelDeveConterApenasGolpesDoCatalogoComPoderPositivo() {
        for (String id : AnalisadorSemantico.GOLPES_PODER_VARIAVEL) {
            int poder = catalogo.buscarGolpe(id).orElseThrow(
                    () -> new AssertionError("Fora do catálogo: " + id)).poderBase();
            assertTrue(poder > 0, id);
        }
    }

    @Test
    void deveUsarMinimoMaisUmEmGolpeDeIntervalo() {
        assertEquals(3, analisar("venusaur !bullet-seed -> charizard").acertos());
    }

    @Test
    void deveUsarContagemFixaEmGolpeDeAcertosFixos() {
        assertEquals(2, analisar("charizard !dual-wingbeat -> venusaur").acertos());
    }
}
