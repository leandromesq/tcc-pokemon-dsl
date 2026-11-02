package br.edu.tcc.dsl.parser;

import java.util.List;

import br.edu.tcc.dsl.lexer.Token;
import br.edu.tcc.dsl.lexer.TokenType;

public final class Parser {

    private final List<Token> tokens;
    private int position;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static Cenario parse(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()
                || tokens.get(tokens.size() - 1).type() != TokenType.EOF) {
            throw new IllegalArgumentException(
                    "A lista de tokens deve terminar com EOF"
            );
        }

        Parser parser = new Parser(List.copyOf(tokens));
        Token atacante = parser.expect(TokenType.NAME);
        parser.expect(TokenType.BANG);
        Token golpe = parser.expect(TokenType.NAME);
        parser.expect(TokenType.ARROW);
        Token defensor = parser.expect(TokenType.NAME);
        parser.expect(TokenType.EOF);

        if (parser.position != parser.tokens.size()) {
            throw new IllegalArgumentException("Tokens adicionais após EOF");
        }

        return new Cenario(atacante, golpe, defensor);
    }

    private Token expect(TokenType expected) {
        Token actual = tokens.get(position);

        if (actual.type() != expected) {
            throw new IllegalArgumentException(
                    "Esperado " + expected + " na posição "
                            + actual.startOffset() + ", encontrado " + actual.type()
            );
        }

        position++;
        return actual;
    }
}
