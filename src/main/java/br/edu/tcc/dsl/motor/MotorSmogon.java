package br.edu.tcc.dsl.motor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.edu.tcc.dsl.semantica.CenarioResolvido;
import br.edu.tcc.dsl.semantica.Combatente;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Executa o cálculo no @smogon/calc por meio do adaptador
 * {@code motor-smogon/calcular.js}: um processo Node.js por cálculo, com o
 * pedido em JSON na entrada padrão e a resposta em JSON na saída padrão.
 */
public final class MotorSmogon {

    /** Propriedade de sistema que permite indicar outro caminho para o adaptador. */
    public static final String PROPRIEDADE_ADAPTADOR = "pokedsl.adaptador";
    static final Path ADAPTADOR_PADRAO = Path.of("motor-smogon", "calcular.js");
    private static final long TEMPO_LIMITE_SEGUNDOS = 30;

    /** SP enviados explicitamente: no cenário mínimo, todos são 0. */
    static final Map<String, Integer> SP_ZERADOS = Map.of(
            "hp", 0, "atk", 0, "def", 0, "spa", 0, "spd", 0, "spe", 0);

    private final Path adaptador;
    private final JsonMapper json = JsonMapper.builder().build();

    public MotorSmogon(Path adaptador) {
        this.adaptador = adaptador;
    }

    /** Usa o adaptador indicado em {@value #PROPRIEDADE_ADAPTADOR} ou o caminho padrão. */
    public static MotorSmogon padrao() {
        String configurado = System.getProperty(PROPRIEDADE_ADAPTADOR);
        return new MotorSmogon(configurado != null ? Path.of(configurado) : ADAPTADOR_PADRAO);
    }

    public ResultadoCalculo calcular(CenarioResolvido cenario) {
        if (!Files.isRegularFile(adaptador)) {
            throw new FalhaNoMotor("Adaptador não encontrado: " + adaptador.toAbsolutePath()
                    + " (execute a partir da raiz do projeto ou defina -D"
                    + PROPRIEDADE_ADAPTADOR + ")");
        }

        byte[] pedido = json.writeValueAsBytes(montarPedido(cenario));
        String saida = executarNode(pedido);
        return interpretarResposta(saida);
    }

    static Pedido montarPedido(CenarioResolvido cenario) {
        return new Pedido(
                pokemon(cenario.atacante()),
                pokemon(cenario.defensor()),
                new PedidoGolpe(cenario.golpe().nome(), cenario.acertos())
        );
    }

    private static PedidoPokemon pokemon(Combatente combatente) {
        return new PedidoPokemon(
                combatente.especie().nome(),
                combatente.habilidade(),
                combatente.natureza(),
                SP_ZERADOS
        );
    }

    private String executarNode(byte[] pedido) {
        Process processo;
        try {
            processo = new ProcessBuilder("node", adaptador.toString())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
        } catch (IOException e) {
            throw new FalhaNoMotor("Não foi possível executar o Node.js (comando 'node')", e);
        }

        try {
            try (OutputStream entrada = processo.getOutputStream()) {
                entrada.write(pedido);
            }
            String saida = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!processo.waitFor(TEMPO_LIMITE_SEGUNDOS, TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                throw new FalhaNoMotor("O adaptador excedeu " + TEMPO_LIMITE_SEGUNDOS + " s");
            }
            return saida;
        } catch (IOException e) {
            processo.destroyForcibly();
            throw new FalhaNoMotor("Falha na comunicação com o adaptador", e);
        } catch (InterruptedException e) {
            processo.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new FalhaNoMotor("Cálculo interrompido", e);
        }
    }

    ResultadoCalculo interpretarResposta(String saida) {
        Resposta resposta;
        try {
            resposta = json.readValue(saida, Resposta.class);
        } catch (JacksonException e) {
            throw new FalhaNoMotor("Resposta inválida do adaptador: " + saida.strip(), e);
        }

        if (resposta.erro() != null) {
            throw new FalhaNoMotor("O motor recusou o cálculo: " + resposta.erro());
        }
        if (resposta.descricao() == null || resposta.dano() == null) {
            throw new FalhaNoMotor("Resposta incompleta do adaptador: " + saida.strip());
        }
        return new ResultadoCalculo(resposta.descricao(), resposta.dano(), resposta.hpDefensor());
    }

    // Contrato JSON com calcular.js (nomes dos campos em inglês, como no catálogo).

    record Pedido(
            @JsonProperty("attacker") PedidoPokemon atacante,
            @JsonProperty("defender") PedidoPokemon defensor,
            @JsonProperty("move") PedidoGolpe golpe) {
    }

    record PedidoPokemon(
            @JsonProperty("species") String especie,
            @JsonProperty("ability") String habilidade,
            @JsonProperty("nature") String natureza,
            @JsonProperty("sp") Map<String, Integer> sp) {
    }

    record PedidoGolpe(
            @JsonProperty("name") String nome,
            @JsonProperty("hits") int acertos) {
    }

    record Resposta(
            @JsonProperty("description") String descricao,
            @JsonProperty("damage") List<List<Integer>> dano,
            @JsonProperty("defenderHp") int hpDefensor,
            @JsonProperty("error") String erro) {
    }
}
