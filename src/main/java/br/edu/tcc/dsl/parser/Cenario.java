package br.edu.tcc.dsl.parser;

import br.edu.tcc.dsl.lexer.Token;

public record Cenario(Token atacante, Token golpe, Token defensor) {
}
