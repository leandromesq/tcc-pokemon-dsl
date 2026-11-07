// Exporta o catálogo do Pokémon Champions do @smogon/calc para um JSON
// versionado, lido pela implementação Java da DSL.
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { Generations, Move } = require('@smogon/calc');
const { version } = require('@smogon/calc/package.json');

const gen = Generations.get(0); // 0 = Champions

// Converte uma espécie do motor para o formato do catálogo.
function exportarEspecie(s) {
  return {
    id: s.id,
    name: s.name,
    types: [...s.types],
    baseStats: {
      hp: s.baseStats.hp,
      atk: s.baseStats.atk,
      def: s.baseStats.def,
      spa: s.baseStats.spa,
      spd: s.baseStats.spd,
      spe: s.baseStats.spe,
    },
    defaultAbility: s.abilities[0],
  };
}

// Converte o campo multihit do motor, que pode ser ausente (um acerto),
// um número fixo (ex.: 2) ou um intervalo [mínimo, máximo] (ex.: [2, 5]).
function converterAcertos(multihit) {
  if (multihit === undefined) {
    return { minHits: 1, maxHits: 1 };
  }
  if (Array.isArray(multihit)) {
    return { minHits: multihit[0], maxHits: multihit[1] };
  }
  return { minHits: multihit, maxHits: multihit };
}

// Devolve o motivo da exclusão se o golpe estiver incompleto nos dados do
// motor, ou null se ele tiver todos os campos necessários.
// A categoria não entra aqui: ela falta em muitos golpes de status completos
// e é obtida pela classe Move (ver exportarGolpe).
function motivoIncompleto(m) {
  const faltando = [];
  if (m.type === undefined) faltando.push('type');
  if (m.basePower === undefined) faltando.push('basePower');

  if (faltando.length === 0) {
    return null;
  }
  return `entrada incompleta: sem ${juntarComE(faltando)}`;
}

// ['a'] -> 'a'; ['a', 'b'] -> 'a e b'; ['a', 'b', 'c'] -> 'a, b e c'
function juntarComE(itens) {
  if (itens.length === 1) {
    return itens[0];
  }
  return `${itens.slice(0, -1).join(', ')} e ${itens[itens.length - 1]}`;
}

// Converte um golpe completo do motor para o formato do catálogo.
// A categoria vem da classe Move, que a corrige quando falta no dado bruto.
function exportarGolpe(m) {
  return {
    id: m.id,
    name: m.name,
    type: m.type,
    category: new Move(gen, m.name).category,
    basePower: m.basePower,
    ...converterAcertos(m.multihit),
  };
}

const moves = [];
const excluded = [];

for (const m of gen.moves) {
  if (m.id === 'nomove') {
    continue; // marcador interno do motor, não é um golpe
  }
  const motivo = motivoIncompleto(m);
  if (motivo !== null) {
    excluded.push({ kind: 'move', name: m.name, reason: motivo });
  } else {
    moves.push(exportarGolpe(m));
  }
}

// Comparação por código de caractere, independente do idioma da máquina,
// para que a ordem (e portanto o arquivo gerado) seja sempre a mesma.
function compararTexto(a, b) {
  if (a < b) return -1;
  if (a > b) return 1;
  return 0;
}

const species = [...gen.species].map(exportarEspecie);

species.sort((a, b) => compararTexto(a.id, b.id));
moves.sort((a, b) => compararTexto(a.id, b.id));
excluded.sort((a, b) => compararTexto(a.name, b.name));

const catalogo = {
  metadata: {
    source: '@smogon/calc',
    version,
    generation: 'champions',
    excluded,
  },
  species,
  moves,
};

const destino = path.join(__dirname, '..', 'src', 'main', 'resources', 'catalogo-champions.json');
const conteudo = JSON.stringify(catalogo, null, 2) + '\n';

const caminhoExibido = path.relative(process.cwd(), destino);

// Com --verificar, não grava nada: só confere se o arquivo versionado é
// idêntico ao que seria gerado agora (útil depois de atualizar o motor).
if (process.argv.includes('--verificar')) {
  const atual = fs.existsSync(destino) ? fs.readFileSync(destino, 'utf8') : null;
  if (atual === conteudo) {
    console.log(`${caminhoExibido} está atualizado.`);
  } else {
    console.error(`${caminhoExibido} está desatualizado. Rode: npm run exportar-catalogo`);
    process.exit(1);
  }
} else {
  fs.mkdirSync(path.dirname(destino), { recursive: true });
  fs.writeFileSync(destino, conteudo, 'utf8');
  console.log(`catálogo gravado em ${caminhoExibido}`);
  console.log(`${species.length} espécies, ${moves.length} golpes, ${excluded.length} excluídos`);
}
