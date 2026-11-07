package br.edu.tcc.dsl.motor;

/** Falha ao executar o adaptador Node.js ou ao interpretar sua resposta. */
public final class FalhaNoMotor extends RuntimeException {

    public FalhaNoMotor(String mensagem) {
        super(mensagem);
    }

    public FalhaNoMotor(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
