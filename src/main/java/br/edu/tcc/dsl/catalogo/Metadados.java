package br.edu.tcc.dsl.catalogo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Origem do catálogo: motor, versão, geração e entradas excluídas. */
public record Metadados(
        @JsonProperty("source") String fonte,
        @JsonProperty("version") String versao,
        @JsonProperty("generation") String geracao,
        @JsonProperty("excluded") List<Exclusao> excluidos
) {

    public Metadados {
        excluidos = List.copyOf(excluidos);
    }
}
