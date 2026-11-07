package br.edu.tcc.dsl.motor;

import java.util.List;

/**
 * Resultado devolvido pelo motor.
 *
 * @param descricao  linha descritiva no formato do @smogon/calc
 * @param rolagens   as 16 rolagens de dano, uma lista por acerto
 * @param hpDefensor HP máximo do defensor
 */
public record ResultadoCalculo(String descricao, List<List<Integer>> rolagens, int hpDefensor) {

    public ResultadoCalculo {
        rolagens = rolagens.stream().map(List::copyOf).toList();
    }
}
