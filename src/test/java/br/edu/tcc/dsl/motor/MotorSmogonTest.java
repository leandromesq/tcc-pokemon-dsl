package br.edu.tcc.dsl.motor;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.edu.tcc.dsl.catalogo.Catalogo;
import br.edu.tcc.dsl.lexer.Lexer;
import br.edu.tcc.dsl.parser.Parser;
import br.edu.tcc.dsl.semantica.AnalisadorSemantico;
import br.edu.tcc.dsl.semantica.CenarioResolvido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de integração: executam o adaptador Node.js real (requer `npm ci`). */
class MotorSmogonTest {

    private static AnalisadorSemantico analisador;
    private static final MotorSmogon motor = new MotorSmogon(MotorSmogon.ADAPTADOR_PADRAO);

    @BeforeAll
    static void preparar() {
        analisador = new AnalisadorSemantico(Catalogo.carregar());
    }

    private static CenarioResolvido resolver(String fonte) {
        return analisador.analisar(Parser.parse(Lexer.tokenize(fonte)));
    }

    @Test
    void deveReproduzirOExemploVerificadoDaFormula() {
        // Ver docs/formula-dano-champions.md, seção 5.
        ResultadoCalculo resultado = motor.calcular(resolver("charizard !flamethrower -> venusaur"));

        assertEquals(
                "0 SpA Charizard Flamethrower vs. 0 HP / 0 SpD Venusaur: "
                        + "110-132 (70.9 - 85.1%) -- guaranteed 2HKO",
                resultado.descricao());
        assertEquals(List.of(List.of(110, 110, 114, 114, 116, 116, 120, 120,
                120, 122, 122, 126, 126, 128, 128, 132)), resultado.rolagens());
        assertEquals(155, resultado.hpDefensor());
    }

    @Test
    void deveEnviarONumeroDeAcertosDeGolpeDeIntervalo() {
        ResultadoCalculo resultado = motor.calcular(resolver("venusaur !bullet-seed -> charizard"));

        assertTrue(resultado.descricao().contains("(3 hits)"));
        assertEquals(3, resultado.rolagens().size());
    }

    @Test
    void deveDescreverGolpeSemEfeito() {
        ResultadoCalculo resultado = motor.calcular(resolver("venusaur !body-slam -> gengar"));

        assertEquals("Venusaur Body Slam vs. Gengar: 0-0 (0 - 0%)", resultado.descricao());
        assertEquals(List.of(List.of(0)), resultado.rolagens());
    }

    @Test
    void deveMontarPedidoComParametrosExplicitos() {
        MotorSmogon.Pedido pedido = MotorSmogon.montarPedido(resolver("charizard !flamethrower -> venusaur"));

        assertEquals("Blaze", pedido.atacante().habilidade());
        assertEquals("Serious", pedido.atacante().natureza());
        assertEquals(MotorSmogon.SP_ZERADOS, pedido.atacante().sp());
        assertEquals("Overgrow", pedido.defensor().habilidade());
        assertEquals(1, pedido.golpe().acertos());
    }

    @Test
    void deveFalharComAdaptadorInexistente() {
        MotorSmogon semAdaptador = new MotorSmogon(Path.of("nao-existe.js"));

        FalhaNoMotor falha = assertThrows(FalhaNoMotor.class,
                () -> semAdaptador.calcular(resolver("charizard !flamethrower -> venusaur")));
        assertTrue(falha.getMessage().contains("Adaptador não encontrado"));
    }

    @Test
    void deveRepassarErroInformadoPeloAdaptador() {
        FalhaNoMotor falha = assertThrows(FalhaNoMotor.class,
                () -> motor.interpretarResposta("{\"error\":\"falhou\"}"));
        assertTrue(falha.getMessage().contains("falhou"));
    }

    @Test
    void deveRejeitarRespostaQueNaoEJson() {
        assertThrows(FalhaNoMotor.class, () -> motor.interpretarResposta("isto não é json"));
    }
}
