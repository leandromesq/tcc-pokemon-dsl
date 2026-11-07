package br.edu.tcc.dsl.semantica;

import br.edu.tcc.dsl.lexer.Token;

/** Cenário sintaticamente válido, mas sem significado no catálogo ou fora do escopo suportado. */
public final class ErroSemantico extends RuntimeException {

    private final Token token;

    public ErroSemantico(String mensagem, Token token) {
        super(mensagem);
        this.token = token;
    }

    /** Token que causou o erro, com sua posição na entrada. */
    public Token token() {
        return token;
    }
}
