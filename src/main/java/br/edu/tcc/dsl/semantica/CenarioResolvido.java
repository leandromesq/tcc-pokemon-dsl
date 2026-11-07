package br.edu.tcc.dsl.semantica;

import br.edu.tcc.dsl.catalogo.Golpe;

/** Cenário validado, com entidades do catálogo e parâmetros explícitos. */
public record CenarioResolvido(Combatente atacante, Golpe golpe, int acertos, Combatente defensor) {
}
