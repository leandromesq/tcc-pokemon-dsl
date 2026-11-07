package br.edu.tcc.dsl.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.edu.tcc.dsl.catalogo.Catalogo;
import br.edu.tcc.dsl.motor.MotorSmogon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes ponta a ponta da CLI (executam o adaptador Node.js real). */
class PokeDslTest {

    private static Catalogo catalogo;

    private final ByteArrayOutputStream saida = new ByteArrayOutputStream();
    private final ByteArrayOutputStream erros = new ByteArrayOutputStream();

    @BeforeAll
    static void carregarCatalogo() {
        catalogo = Catalogo.carregar();
    }

    private int executar(MotorSmogon motor, String... args) {
        PokeDsl cli = new PokeDsl(catalogo, motor,
                new PrintStream(saida, true, StandardCharsets.UTF_8),
                new PrintStream(erros, true, StandardCharsets.UTF_8));
        return cli.executar(args);
    }

    private int executar(String... args) {
        return executar(MotorSmogon.padrao(), args);
    }

    private String saida() {
        return saida.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }

    private String erros() {
        return erros.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }

    @Test
    void deveImprimirResultadoEPremissas() {
        assertEquals(PokeDsl.SUCESSO, executar("charizard !flamethrower -> venusaur"));

        assertEquals("""
                0 SpA Charizard Flamethrower vs. 0 HP / 0 SpD Venusaur: 110-132 (70.9 - 85.1%) -- guaranteed 2HKO
                (110, 110, 114, 114, 116, 116, 120, 120, 120, 122, 122, 126, 126, 128, 128, 132)

                Premissas: geração Champions, nível 50, @smogon/calc 0.12.0
                  Charizard: Blaze (habilidade padrão), natureza Serious, SP 0, sem item
                  Venusaur: Overgrow (habilidade padrão), natureza Serious, SP 0, sem item
                  campo: sem clima
                """, saida());
        assertEquals("", erros());
    }

    @Test
    void deveInformarAcertosDeGolpeMultihit() {
        assertEquals(PokeDsl.SUCESSO, executar("venusaur !bullet-seed -> charizard"));

        assertTrue(saida().contains("(3 hits)"));
        assertTrue(saida().contains("Bullet Seed: 3 acertos (padrão: mínimo + 1; varia de 2 a 5)"));
    }

    @Test
    void deveApontarErroSemanticoNoToken() {
        assertEquals(PokeDsl.CENARIO_INVALIDO, executar("charizard !flamethrower -> caterpie"));

        assertEquals("""
                Erro semântico: Espécie desconhecida no catálogo Champions: 'caterpie'
                  charizard !flamethrower -> caterpie
                                             ^^^^^^^^
                """, erros());
        assertEquals("", saida());
    }

    @Test
    void deveInformarErroDeSintaxe() {
        assertEquals(PokeDsl.CENARIO_INVALIDO, executar("charizard flamethrower -> venusaur"));

        assertTrue(erros().startsWith("Erro de sintaxe: Esperado BANG"));
    }

    @Test
    void deveExigirExatamenteUmArgumento() {
        assertEquals(PokeDsl.USO_INCORRETO, executar());
        assertEquals(PokeDsl.USO_INCORRETO, executar("charizard", "!flamethrower"));

        assertTrue(erros().startsWith("Uso: pokedsl"));
    }

    @Test
    void deveInformarFalhaNoMotor() {
        assertEquals(PokeDsl.FALHA_NO_MOTOR,
                executar(new MotorSmogon(Path.of("nao-existe.js")), "charizard !flamethrower -> venusaur"));

        assertTrue(erros().startsWith("Falha no motor de cálculo: Adaptador não encontrado"));
    }
}
