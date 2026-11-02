package br.edu.tcc.dsl.parser;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.tcc.dsl.lexer.Lexer;
import br.edu.tcc.dsl.lexer.Token;
import br.edu.tcc.dsl.lexer.TokenType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    @Test
    void deveConstruirCenarioComTokensEPosicoes() {
        Cenario cenario = Parser.parse(
                Lexer.tokenize("charmander !ember -> caterpie")
        );

        assertEquals(new Cenario(
                new Token(TokenType.NAME, "charmander", 0, 10),
                new Token(TokenType.NAME, "ember", 12, 17),
                new Token(TokenType.NAME, "caterpie", 21, 29)
        ), cenario);
    }

    @Test
    void deveAceitarCenarioSemEspacos() {
        Cenario cenario = Parser.parse(
                Lexer.tokenize("Charmander!golpeinexistente->mr-mime")
        );

        assertEquals("Charmander", cenario.atacante().lexeme());
        assertEquals("golpeinexistente", cenario.golpe().lexeme());
        assertEquals("mr-mime", cenario.defensor().lexeme());
    }

    @Test
    void deveRejeitarEntradaVazia() {
        assertErro("", "NAME", 0);
    }

    @Test
    void deveRejeitarMarcadorDeGolpeAusente() {
        assertErro("charmander ember -> caterpie", "BANG", 11);
    }

    @Test
    void deveRejeitarGolpeAusente() {
        assertErro("charmander ! -> caterpie", "NAME", 13);
    }

    @Test
    void deveRejeitarSetaAusente() {
        assertErro("charmander !ember caterpie", "ARROW", 18);
    }

    @Test
    void deveRejeitarDefensorAusente() {
        assertErro("charmander !ember ->", "NAME", 20);
    }

    @Test
    void deveRejeitarConteudoAdicional() {
        assertErro("charmander !ember -> caterpie extra", "EOF", 30);
    }

    @Test
    void deveRejeitarListaSemEofFinal() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Parser.parse(List.of())
        );
    }

    @Test
    void deveRejeitarTokensAposEof() {
        List<Token> tokens = List.of(
                new Token(TokenType.NAME, "a", 0, 1),
                new Token(TokenType.BANG, "!", 1, 2),
                new Token(TokenType.NAME, "b", 2, 3),
                new Token(TokenType.ARROW, "->", 3, 5),
                new Token(TokenType.NAME, "c", 5, 6),
                new Token(TokenType.EOF, "", 6, 6),
                new Token(TokenType.EOF, "", 6, 6)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Parser.parse(tokens)
        );
    }

    private void assertErro(String source, String expected, int position) {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> Parser.parse(Lexer.tokenize(source))
        );

        assertTrue(error.getMessage().contains("Esperado " + expected));
        assertTrue(error.getMessage().contains("posição " + position));
    }
}
