package br.edu.tcc.dsl.lexer;

public record Token(
    TokenType type,
    String lexeme,
    int startOffset,
    int endOffset
    ) {
}
