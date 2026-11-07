package br.edu.tcc.dsl.catalogo;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoTest {

    private static Catalogo catalogo;

    @BeforeAll
    static void carregarCatalogo() {
        catalogo = Catalogo.carregar();
    }

    @Test
    void deveCarregarMetadadosETotais() {
        Metadados metadados = catalogo.metadados();

        assertEquals("@smogon/calc", metadados.fonte());
        assertEquals("0.12.0", metadados.versao());
        assertEquals("champions", metadados.geracao());
        assertEquals(11, metadados.excluidos().size());
        assertEquals(359, catalogo.quantidadeEspecies());
        assertEquals(514, catalogo.quantidadeGolpes());
    }

    @Test
    void deveEncontrarEspecieComTodosOsCampos() {
        Especie charizard = catalogo.buscarEspecie("charizard").orElseThrow();

        assertEquals(new Especie(
                "charizard",
                "Charizard",
                List.of("Fire", "Flying"),
                new EstatisticasBase(78, 84, 78, 109, 85, 100),
                "Blaze"
        ), charizard);
    }

    @Test
    void deveEncontrarGolpeComUmAcerto() {
        assertEquals(
                new Golpe("flamethrower", "Flamethrower", "Fire", "Special", 90, 1, 1),
                catalogo.buscarGolpe("flamethrower").orElseThrow()
        );
    }

    @Test
    void deveEncontrarGolpeComVariosAcertos() {
        Golpe bulletSeed = catalogo.buscarGolpe("bullet-seed").orElseThrow();

        assertEquals(2, bulletSeed.minAcertos());
        assertEquals(5, bulletSeed.maxAcertos());
    }

    @Test
    void deveResolverCategoriaDeGolpeDeStatusSemCategoriaNosDadosBrutos() {
        assertEquals("Status", catalogo.buscarGolpe("nasty-plot").orElseThrow().categoria());
    }

    @Test
    void deveNormalizarNomesComoToId() {
        assertEquals("Charizard", catalogo.buscarEspecie("CHARIZARD").orElseThrow().nome());
        assertEquals("Mr. Mime", catalogo.buscarEspecie("mr-mime").orElseThrow().nome());
        assertEquals("Charizard-Mega-X", catalogo.buscarEspecie("charizard-mega-x").orElseThrow().nome());
        assertEquals("U-turn", catalogo.buscarGolpe("u-turn").orElseThrow().nome());
        assertEquals("U-turn", catalogo.buscarGolpe("uturn").orElseThrow().nome());
    }

    @Test
    void toIdDeveManterApenasLetrasMinusculasEDigitos() {
        assertEquals("mrmime", Catalogo.toId("Mr-Mime"));
        assertEquals("kingsshield", Catalogo.toId("King's Shield"));
        assertEquals("porygon2", Catalogo.toId("Porygon2"));
    }

    @Test
    void naoDeveEncontrarEntidadeDesconhecida() {
        assertTrue(catalogo.buscarEspecie("charmander").isEmpty());
        assertTrue(catalogo.buscarGolpe("ember").isEmpty());
    }

    @Test
    void naoDeveEncontrarGolpeExcluidoPorEstarIncompleto() {
        assertTrue(catalogo.buscarGolpe("metal-claw").isEmpty());
        assertTrue(catalogo.metadados().excluidos().stream()
                .anyMatch(e -> e.nome().equals("Metal Claw")));
    }

    @Test
    void buscaDeveSerSeparadaPorCategoria() {
        assertTrue(catalogo.buscarGolpe("charizard").isEmpty());
        assertTrue(catalogo.buscarEspecie("flamethrower").isEmpty());
    }
}
