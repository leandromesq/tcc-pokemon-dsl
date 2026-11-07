// Adaptador entre a DSL (Java) e o @smogon/calc.
//
// Lê um pedido JSON da entrada padrão, calcula o dano no modo Champions e
// escreve uma resposta JSON na saída padrão. Todos os parâmetros chegam
// explícitos do Java: o adaptador não escolhe valores padrão.
//
// Pedido:
//   { "attacker": { "species", "ability", "nature", "sp": { hp, atk, def, spa, spd, spe } },
//     "defender": { ...mesmos campos },
//     "move": { "name", "hits" } }
// Resposta (sucesso, código 0):
//   { "description", "damage": [[16 rolagens], ...uma lista por acerto], "defenderHp" }
// Resposta (falha, código 1):
//   { "error": "mensagem" }
'use strict';

const { calculate, Generations, Pokemon, Move, Field } = require('@smogon/calc');

const gen = Generations.get(0); // 0 = Champions

function criarPokemon(dados) {
  return new Pokemon(gen, dados.species, {
    ability: dados.ability,
    nature: dados.nature,
    evs: dados.sp, // no Champions, o campo evs do motor guarda os SP
  });
}

// O motor devolve o dano em três formatos: um número (golpe sem efeito),
// uma lista de 16 rolagens (um acerto) ou uma lista de listas (vários acertos).
// A resposta usa sempre o último formato.
function normalizarDano(dano) {
  if (typeof dano === 'number') {
    return [[dano]];
  }
  if (Array.isArray(dano[0])) {
    return dano;
  }
  return [dano];
}

function calcular(pedido) {
  const atacante = criarPokemon(pedido.attacker);
  const defensor = criarPokemon(pedido.defender);
  const golpe = new Move(gen, pedido.move.name, {
    ability: pedido.attacker.ability,
    hits: pedido.move.hits,
  });

  const resultado = calculate(gen, atacante, defensor, golpe, new Field());
  const dano = normalizarDano(resultado.damage);
  const semDano = dano.every((acerto) => acerto.every((valor) => valor === 0));

  // desc() lança exceção quando não há dano (imunidade ou golpe que falha);
  // nesse caso a descrição é montada no mesmo formato do motor.
  const descricao = semDano
    ? `${atacante.name} ${golpe.name} vs. ${defensor.name}: 0-0 (0 - 0%)`
    : resultado.desc();

  return { description: descricao, damage: dano, defenderHp: defensor.maxHP() };
}

function lerEntrada() {
  return require('node:fs').readFileSync(0, 'utf8');
}

try {
  const resposta = calcular(JSON.parse(lerEntrada()));
  process.stdout.write(JSON.stringify(resposta) + '\n');
} catch (erro) {
  process.stdout.write(JSON.stringify({ error: String(erro && erro.message || erro) }) + '\n');
  process.exitCode = 1;
}
