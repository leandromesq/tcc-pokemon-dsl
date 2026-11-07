package br.edu.tcc.dsl.catalogo;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Estatísticas-base de uma espécie, como aparecem no catálogo. */
public record EstatisticasBase(
        @JsonProperty("hp") int hp,
        @JsonProperty("atk") int ataque,
        @JsonProperty("def") int defesa,
        @JsonProperty("spa") int ataqueEspecial,
        @JsonProperty("spd") int defesaEspecial,
        @JsonProperty("spe") int velocidade
) {
}
