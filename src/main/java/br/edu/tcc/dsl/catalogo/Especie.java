package br.edu.tcc.dsl.catalogo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Espécie do catálogo. {@code nome} é o nome canônico exibido na saída. */
public record Especie(
        @JsonProperty("id") String id,
        @JsonProperty("name") String nome,
        @JsonProperty("types") List<String> tipos,
        @JsonProperty("baseStats") EstatisticasBase estatisticasBase,
        @JsonProperty("defaultAbility") String habilidadePadrao
) {

    public Especie {
        tipos = List.copyOf(tipos);
    }
}
