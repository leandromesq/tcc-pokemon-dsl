package br.edu.tcc.dsl.semantica;

import br.edu.tcc.dsl.catalogo.Especie;

/**
 * Pokémon do cenário com todos os parâmetros resolvidos. No cenário mínimo,
 * a habilidade é a padrão da espécie, a natureza é neutra e todos os SP são 0.
 */
public record Combatente(Especie especie, String habilidade, String natureza) {
}
