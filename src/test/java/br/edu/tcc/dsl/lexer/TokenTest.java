package br.edu.tcc.dsl.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenTest {

    @Test
    void devePreservarOsDadosDoToken() {
        Token token = new Token(
                TokenType.NAME,
                "charmander",
                0,
                10
        );

        assertEquals(TokenType.NAME, token.type());
        assertEquals("charmander", token.lexeme());
        assertEquals(0, token.startOffset());
        assertEquals(10, token.endOffset());
    }
}
