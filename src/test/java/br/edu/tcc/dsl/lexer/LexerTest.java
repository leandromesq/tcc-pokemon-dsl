package br.edu.tcc.dsl.lexer;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LexerTest {

    @Test
    void deveReconhecerMarcadoresComSuasPosicoes() {
        List<Token> tokens = Lexer.tokenize("! ->");

        assertEquals(List.of(
                new Token(TokenType.BANG, "!", 0, 1),
                new Token(TokenType.ARROW, "->", 2, 4),
                new Token(TokenType.EOF, "", 4, 4)
        ), tokens);
    }

    @Test
    void deveReconhecerMarcadoresSemEspacos() {
        assertEquals(List.of(
                new Token(TokenType.BANG, "!", 0, 1),
                new Token(TokenType.ARROW, "->", 1, 3),
                new Token(TokenType.EOF, "", 3, 3)
        ), Lexer.tokenize("!->"));
    }

    @Test
    void deveProduzirEofParaEntradaVazia() {
        assertEquals(
                List.of(new Token(TokenType.EOF, "", 0, 0)),
                Lexer.tokenize("")
        );
    }

    @Test
    void deveIgnorarEspacosTabulacoesEQuebrasDeLinha() {
        assertEquals(
                List.of(new Token(TokenType.EOF, "", 3, 3)),
                Lexer.tokenize(" \t\n")
        );
    }

    @Test
    void deveRejeitarHifenIsolado() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Lexer.tokenize("-")
        );
    }

    @Test
    void deveRejeitarSetaSeparadaPorEspaco() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Lexer.tokenize("- >")
        );
    }

    @Test
    void deveRejeitarCaractereDesconhecido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Lexer.tokenize("@")
        );
    }

    @Test
    void deveReconhecerCenarioMinimo() {
        assertEquals(List.of(
                new Token(TokenType.NAME, "charmander", 0, 10),
                new Token(TokenType.BANG, "!", 11, 12),
                new Token(TokenType.NAME, "ember", 12, 17),
                new Token(TokenType.ARROW, "->", 18, 20),
                new Token(TokenType.NAME, "caterpie", 21, 29),
                new Token(TokenType.EOF, "", 29, 29)
        ), Lexer.tokenize("charmander !ember -> caterpie"));
    }

    @Test
    void deveReconhecerNomesComHifenENumeros() {
        assertEquals(List.of(
                new Token(TokenType.NAME, "mr-mime", 0, 7),
                new Token(TokenType.NAME, "porygon2", 8, 16),
                new Token(TokenType.EOF, "", 16, 16)
        ), Lexer.tokenize("mr-mime porygon2"));
    }

    @Test
    void deveDistinguirSetaDeHifenNoNome() {
        assertEquals(List.of(
                new Token(TokenType.NAME, "ember", 0, 5),
                new Token(TokenType.ARROW, "->", 5, 7),
                new Token(TokenType.NAME, "mr-mime", 7, 14),
                new Token(TokenType.EOF, "", 14, 14)
        ), Lexer.tokenize("ember->mr-mime"));
    }

    @Test
    void devePreservarMaiusculasEMinusculas() {
        assertEquals(List.of(
                new Token(TokenType.NAME, "Charmander", 0, 10),
                new Token(TokenType.EOF, "", 10, 10)
        ), Lexer.tokenize("Charmander"));
    }

    @Test
    void deveRejeitarNomeIniciadoPorNumero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Lexer.tokenize("2porygon")
        );
    }

    @Test
    void deveRejeitarHifenNoFinalDoNome() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Lexer.tokenize("mr-")
        );
    }
}
