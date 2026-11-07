package br.edu.tcc.dsl.catalogo;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Entrada do motor que ficou fora do catálogo, com o motivo. */
public record Exclusao(
        @JsonProperty("kind") String categoria,
        @JsonProperty("name") String nome,
        @JsonProperty("reason") String motivo
) {
}
