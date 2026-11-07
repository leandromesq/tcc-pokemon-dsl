package br.edu.tcc.dsl.catalogo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.json.JsonMapper;

/**
 * Catálogo de espécies e golpes do Champions, gerado a partir do
 * {@code @smogon/calc} por {@code motor-smogon/exportar-catalogo.js}.
 * As buscas normalizam o nome pela regra {@link #toId(String)}.
 */
public final class Catalogo {

    static final String RECURSO = "/catalogo-champions.json";

    private final Metadados metadados;
    private final Map<String, Especie> especies;
    private final Map<String, Golpe> golpes;

    private Catalogo(Metadados metadados, List<Especie> especies, List<Golpe> golpes) {
        this.metadados = metadados;
        this.especies = indexarPorId(especies, Especie::id);
        this.golpes = indexarPorId(golpes, Golpe::id);
    }

    /** Lê o catálogo versionado do classpath. */
    public static Catalogo carregar() {
        try (InputStream entrada = Catalogo.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("Recurso não encontrado: " + RECURSO);
            }
            return ler(entrada);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + RECURSO, e);
        }
    }

    static Catalogo ler(InputStream entrada) {
        Arquivo arquivo = JsonMapper.builder().build().readValue(entrada, Arquivo.class);
        return new Catalogo(arquivo.metadados(), arquivo.especies(), arquivo.golpes());
    }

    /**
     * Normaliza um nome como o {@code toID} do Smogon: minúsculas, mantendo
     * apenas {@code a-z} e {@code 0-9}. Ex.: "Mr-Mime" -> "mrmime".
     */
    public static String toId(String nome) {
        return nome.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public Optional<Especie> buscarEspecie(String nome) {
        return Optional.ofNullable(especies.get(toId(nome)));
    }

    public Optional<Golpe> buscarGolpe(String nome) {
        return Optional.ofNullable(golpes.get(toId(nome)));
    }

    public Metadados metadados() {
        return metadados;
    }

    public int quantidadeEspecies() {
        return especies.size();
    }

    public int quantidadeGolpes() {
        return golpes.size();
    }

    private static <T> Map<String, T> indexarPorId(List<T> itens, Function<T, String> id) {
        // Collectors.toMap lança exceção se houver ids repetidos.
        return itens.stream().collect(Collectors.toUnmodifiableMap(id, Function.identity()));
    }

    /** Estrutura do arquivo JSON, usada apenas na leitura. */
    private record Arquivo(
            @JsonProperty("metadata") Metadados metadados,
            @JsonProperty("species") List<Especie> especies,
            @JsonProperty("moves") List<Golpe> golpes
    ) {
    }
}
