package br.edu.tcc.dsl.cli;

import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

import br.edu.tcc.dsl.catalogo.Catalogo;
import br.edu.tcc.dsl.catalogo.Metadados;
import br.edu.tcc.dsl.lexer.Lexer;
import br.edu.tcc.dsl.lexer.Token;
import br.edu.tcc.dsl.motor.FalhaNoMotor;
import br.edu.tcc.dsl.motor.MotorSmogon;
import br.edu.tcc.dsl.motor.ResultadoCalculo;
import br.edu.tcc.dsl.parser.Cenario;
import br.edu.tcc.dsl.parser.Parser;
import br.edu.tcc.dsl.semantica.AnalisadorSemantico;
import br.edu.tcc.dsl.semantica.CenarioResolvido;
import br.edu.tcc.dsl.semantica.Combatente;
import br.edu.tcc.dsl.semantica.ErroSemantico;

/**
 * Interface de linha de comando: recebe um cenário como argumento, executa
 * análise léxica, sintática e semântica, calcula no motor e imprime o resultado.
 *
 * <p>Códigos de saída: 0 sucesso; 1 cenário inválido; 2 uso incorreto;
 * 3 falha no motor.
 */
public final class PokeDsl {

    static final int SUCESSO = 0;
    static final int CENARIO_INVALIDO = 1;
    static final int USO_INCORRETO = 2;
    static final int FALHA_NO_MOTOR = 3;

    private final Catalogo catalogo;
    private final MotorSmogon motor;
    private final PrintStream saida;
    private final PrintStream erros;

    PokeDsl(Catalogo catalogo, MotorSmogon motor, PrintStream saida, PrintStream erros) {
        this.catalogo = catalogo;
        this.motor = motor;
        this.saida = saida;
        this.erros = erros;
    }

    public static void main(String[] args) {
        PokeDsl cli = new PokeDsl(Catalogo.carregar(), MotorSmogon.padrao(), System.out, System.err);
        System.exit(cli.executar(args));
    }

    int executar(String[] args) {
        if (args.length != 1) {
            erros.println("Uso: pokedsl \"<atacante> !<golpe> -> <defensor>\"");
            erros.println("Exemplo: pokedsl \"charizard !flamethrower -> venusaur\"");
            return USO_INCORRETO;
        }
        String fonte = args[0];

        CenarioResolvido cenario;
        try {
            Cenario arvore = Parser.parse(Lexer.tokenize(fonte));
            cenario = new AnalisadorSemantico(catalogo).analisar(arvore);
        } catch (ErroSemantico e) {
            erros.println("Erro semântico: " + e.getMessage());
            erros.print(destacar(fonte, e.token()));
            return CENARIO_INVALIDO;
        } catch (IllegalArgumentException e) {
            // O lexer e o parser sinalizam erros com IllegalArgumentException.
            erros.println("Erro de sintaxe: " + e.getMessage());
            return CENARIO_INVALIDO;
        }

        ResultadoCalculo resultado;
        try {
            resultado = motor.calcular(cenario);
        } catch (FalhaNoMotor e) {
            erros.println("Falha no motor de cálculo: " + e.getMessage());
            return FALHA_NO_MOTOR;
        }

        saida.print(formatar(cenario, resultado, catalogo.metadados()));
        return SUCESSO;
    }

    /** Linha do cenário com o trecho do token sublinhado por '^'. */
    static String destacar(String fonte, Token token) {
        int largura = Math.max(1, token.endOffset() - token.startOffset());
        return "  " + fonte + System.lineSeparator()
                + "  " + " ".repeat(token.startOffset()) + "^".repeat(largura)
                + System.lineSeparator();
    }

    static String formatar(CenarioResolvido cenario, ResultadoCalculo resultado, Metadados metadados) {
        String nl = System.lineSeparator();
        StringBuilder texto = new StringBuilder();

        texto.append(resultado.descricao()).append(nl);
        for (List<Integer> acerto : resultado.rolagens()) {
            texto.append(acerto.stream().map(String::valueOf)
                    .collect(Collectors.joining(", ", "(", ")"))).append(nl);
        }

        texto.append(nl).append("Premissas: geração Champions, nível 50, ")
                .append(metadados.fonte()).append(' ').append(metadados.versao()).append(nl);
        texto.append("  ").append(descreverCombatente(cenario.atacante())).append(nl);
        texto.append("  ").append(descreverCombatente(cenario.defensor())).append(nl);
        if (cenario.golpe().maxAcertos() > 1) {
            texto.append("  ").append(descreverAcertos(cenario)).append(nl);
        }
        texto.append("  campo: sem clima").append(nl);
        return texto.toString();
    }

    private static String descreverCombatente(Combatente combatente) {
        return combatente.especie().nome() + ": " + combatente.habilidade()
                + " (habilidade padrão), natureza " + combatente.natureza()
                + ", SP 0, sem item";
    }

    private static String descreverAcertos(CenarioResolvido cenario) {
        var golpe = cenario.golpe();
        if (golpe.minAcertos() == golpe.maxAcertos()) {
            return golpe.nome() + ": " + cenario.acertos() + " acertos (fixo)";
        }
        return golpe.nome() + ": " + cenario.acertos() + " acertos (padrão: mínimo + 1; varia de "
                + golpe.minAcertos() + " a " + golpe.maxAcertos() + ")";
    }
}
