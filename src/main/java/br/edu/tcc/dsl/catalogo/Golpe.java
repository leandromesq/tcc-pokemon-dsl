package br.edu.tcc.dsl.catalogo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Golpe do catálogo. {@code categoria} é "Physical", "Special" ou "Status";
 * golpes de um acerto têm {@code minAcertos} e {@code maxAcertos} iguais a 1.
 */
public record Golpe(
        @JsonProperty("id") String id,
        @JsonProperty("name") String nome,
        @JsonProperty("type") String tipo,
        @JsonProperty("category") String categoria,
        @JsonProperty("basePower") int poderBase,
        @JsonProperty("minHits") int minAcertos,
        @JsonProperty("maxHits") int maxAcertos
) {
}
