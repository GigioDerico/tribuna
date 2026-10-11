"""alinhar.py com transcrições sintéticas de etapa0/textos/ (spec 0004, D1 a D9; impl_plan A3)."""

import sys
import time
import unittest
from pathlib import Path

import alinhar
import roteiro
from testes import sintetico as s


def _alinhar(texto_id, palavras, saida=None):
    """Roda alinhar.main com o texto de etapa0/textos/ e a transcrição sintética de `palavras` (cruas)."""
    with s.Pasta() as p:
        transcricao = p.arquivo("t.words.csv", s.transcricao_csv(palavras))
        argv = [str(roteiro.TEXTOS / f"{texto_id}.txt"), transcricao]
        if saida:
            argv += ["-o", saida]
        return s.rodar(alinhar.main, argv)


def _tempos(out):
    """expected.csv -> {token: (ini, fim) ou None}."""
    linhas = s.linhas_csv(out)
    return {int(t): (None if a == "" else (int(a), int(b))) for t, _p, a, b in linhas[1:]}


def _indice(texto_id, exibicao):
    return next(t.indice for t in s.tokens(texto_id) if s.exibido(texto_id, t) == exibicao)


class LeituraLimpaTest(unittest.TestCase):
    def test_D1_t01_todos_os_tokens_com_o_tempo_das_suas_palavras(self):
        tokens = s.tokens("T01")
        pares = s.palavras_dos_tokens(tokens)
        codigo, out, _err = _alinhar("T01", [p for _, p in pares])
        self.assertEqual(0, codigo)
        linhas = s.linhas_csv(out)
        self.assertEqual(["token", "palavra", "t_inicio_ms", "t_fim_ms"], linhas[0])
        self.assertEqual(["0", "ouvir", "0", "250"], linhas[1])
        self.assertEqual(len(tokens), len(linhas) - 1)
        self.assertEqual([str(t.indice) for t in tokens], [linha[0] for linha in linhas[1:]])
        esperado = s.tempos_esperados(pares)
        self.assertEqual({i: esperado[i] for i in range(len(tokens))}, _tempos(out))

    def test_D1_coluna_palavra_e_o_exibido_normalizado(self):
        tokens = s.tokens("T02")
        _codigo, out, _err = _alinhar("T02", [p for _, p in s.palavras_dos_tokens(tokens)])
        palavras = [linha[1] for linha in s.linhas_csv(out)[1:]]
        self.assertEqual([roteiro.normalizar(s.exibido("T02", t)) for t in tokens], palavras)
        self.assertIn("3 16", palavras)


class VariantesTest(unittest.TestCase):
    def test_D2_tres_formas_das_referencias(self):
        tokens = s.tokens("T02")
        alvos = {_indice("T02", "3:16"), _indice("T02", "13:4-7"), _indice("T02", "1 Coríntios")}
        referencias = [t.indice for t in tokens if len(t.formas) in (3, 6) and s.exibido("T02", t)[0].isdigit()]
        self.assertGreaterEqual(len(referencias), 5, "Q3: 3:16, 23:1, 24:14, 12:2 e 13:4-7")
        for take in (0, 1, 2):  # "tres dezesseis", "tres e dezesseis", "capitulo tres versiculo dezesseis"
            with self.subTest(take=take):
                formas = {i: take for i in referencias}
                formas[_indice("T02", "13:4-7")] = ["treze", "de", "quatro", "a", "sete"]
                formas[_indice("T02", "1 Coríntios")] = ["primeira", "corintios"]
                pares = s.palavras_dos_tokens(tokens, formas)
                codigo, out, _err = _alinhar("T02", [p for _, p in pares])
                self.assertEqual(0, codigo)
                tempos = _tempos(out)
                esperado = s.tempos_esperados(pares)
                for i in alvos | set(referencias):
                    self.assertEqual(esperado[i], tempos[i], f"token {i} ({s.exibido('T02', tokens[i])})")
                ref = _indice("T02", "3:16")
                self.assertEqual(len(tokens[ref].formas[take]) - 1, (tempos[ref][1] - tempos[ref][0] - 250) // 300)


class EventosTest(unittest.TestCase):
    def test_D3_pulo_de_paragrafo_t05(self):
        par = s.paragrafos("T05")
        self.assertEqual(5, len(par))
        tokens = s.tokens("T05")
        pares = [x for x in s.palavras_dos_tokens(tokens) if x[0] not in par[1]]
        codigo, out, _err = _alinhar("T05", [p for _, p in pares])
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        esperado = s.tempos_esperados(pares)
        for i in range(len(tokens)):
            if i in par[1]:
                self.assertIsNone(tempos[i], f"token {i} do 2º parágrafo")
            else:
                self.assertEqual(esperado[i], tempos[i], f"token {i}")

    def test_D4_improviso_t04_nao_da_tempo_a_token(self):
        par = s.paragrafos("T04")
        tokens = s.tokens("T04")
        improviso = [p for _, p in s.palavras_dos_tokens(s.tokens("T03"))]
        base = s.palavras_dos_tokens(tokens)
        corte = max(n for n, (i, _) in enumerate(base) if i in par[0]) + 1
        pares = base[:corte] + [(None, p) for p in improviso] + base[corte:]
        codigo, out, err = _alinhar("T04", [p for _, p in pares])
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        esperado = s.tempos_esperados(pares)
        self.assertEqual({i: esperado[i] for i in range(len(tokens))}, tempos)
        a, z = corte, corte + len(improviso) - 1
        self.assertIn(f"inseridas: palavras {a}-{z} [{300 * a}-{300 * z + 250} ms]", err)
        self.assertIn(f"palavras inseridas: {len(improviso)}", err)

    def test_D5_repeticao_t08_fica_com_a_primeira_vez(self):
        tokens = s.tokens("T08")
        frase = [_indice("T08", w) for w in ("Isso",)]
        inicio = frase[0]
        indices = list(range(inicio, inicio + 4))  # Isso tem um custo
        base = s.palavras_dos_tokens(tokens)
        corte = max(n for n, (i, _) in enumerate(base) if i == indices[-1]) + 1
        repeticao = [p for i, p in base if i in indices]
        self.assertEqual(["isso", "tem", "um", "custo"], repeticao)
        pares = base[:corte] + [(None, p) for p in repeticao] + base[corte:]
        codigo, out, err = _alinhar("T08", [p for _, p in pares])
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        esperado = s.tempos_esperados(pares)
        for i in indices:
            self.assertEqual(esperado[i], tempos[i])
        self.assertIn(f"inseridas: palavras {corte}-{corte + 3} [{300 * corte}-{300 * (corte + 3) + 250} ms]", err)

    def test_D6_refrao_t03_cada_ocorrencia_com_o_seu_tempo(self):
        tokens = s.tokens("T03")
        texto = roteiro.ler_texto("T03")
        inicios = [t.indice for t in tokens if texto.startswith("E a escolha é sempre nossa", t.inicio)]
        self.assertEqual(3, len(inicios))
        pares = s.palavras_dos_tokens(tokens)
        codigo, out, _err = _alinhar("T03", [p for _, p in pares])
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        esperado = s.tempos_esperados(pares)
        for k in inicios:
            for i in range(k, k + 6):
                self.assertEqual(esperado[i], tempos[i])
        self.assertLess(tempos[inicios[0]][0], tempos[inicios[1]][0])
        self.assertLess(tempos[inicios[1]][0], tempos[inicios[2]][0])

    def test_D7_palavras_com_acento_e_maiuscula_como_o_vosk_grande(self):
        """Cada palavra só de letras sai como está no texto (Em, João, Repare...): todas casam depois do normalizador."""
        tokens = s.tokens("T02")
        formas = {t.indice: [s.exibido("T02", t)] for t in tokens if s.exibido("T02", t).isalpha()}
        self.assertIn("João", [f[0] for f in formas.values()])
        pares = s.palavras_dos_tokens(tokens, formas)
        _codigo, out, err = _alinhar("T02", [p for _, p in pares])
        self.assertEqual(s.tempos_esperados(pares), _tempos(out))
        self.assertIn("palavras inseridas: 0", err)

    def test_D7_acento_e_hifen_da_transcricao(self):
        tokens = s.tokens("T02")
        ref = _indice("T02", "3:16")
        pares = s.palavras_dos_tokens(tokens, {ref: ["capítulo", "três", "Versículo", "dezesseis"]})
        _codigo, out, _err = _alinhar("T02", [p for _, p in pares])
        self.assertEqual(s.tempos_esperados(pares)[ref], _tempos(out)[ref])
        tokens = s.tokens("T03")
        bom = _indice("T03", "bom-dia")
        pares = s.palavras_dos_tokens(tokens, {bom: ["bom", "dia"]})
        _codigo, out, _err = _alinhar("T03", [p for _, p in pares])
        self.assertEqual(s.tempos_esperados(pares)[bom], _tempos(out)[bom])
        self.assertIsNotNone(_tempos(out)[bom])


class DesempateTest(unittest.TestCase):
    def test_plano_A3_improviso_que_termina_com_palavra_do_roteiro_nao_rouba_o_tempo(self):
        """T09 tem "por isso que estamos aqui": o "Isso" do T01 fica com a sua ocorrência real."""
        par = s.paragrafos("T04")
        tokens = s.tokens("T04")
        improviso = [p for _, p in s.palavras_dos_tokens(s.tokens("T09"))]
        self.assertIn("isso", improviso)
        base = s.palavras_dos_tokens(tokens)
        corte = max(n for n, (i, _) in enumerate(base) if i in par[0]) + 1
        pares = base[:corte] + [(None, p) for p in improviso] + base[corte:]
        codigo, out, _err = _alinhar("T04", [p for _, p in pares])
        self.assertEqual(0, codigo)
        esperado = s.tempos_esperados(pares)
        self.assertEqual({i: esperado[i] for i in range(len(tokens))}, _tempos(out))

    def test_plano_A3_palavra_comum_antes_do_improviso_fica_antes(self):
        """O improviso do T03 tem "conversa", último token do 1º parágrafo: fica a ocorrência de antes."""
        par = s.paragrafos("T04")
        tokens = s.tokens("T04")
        improviso = [p for _, p in s.palavras_dos_tokens(s.tokens("T03"))]
        self.assertIn("conversa", improviso)
        ultimo = par[0][-1]
        self.assertEqual("conversa", tokens[ultimo].formas[0][0])
        base = s.palavras_dos_tokens(tokens)
        corte = max(n for n, (i, _) in enumerate(base) if i in par[0]) + 1
        pares = base[:corte] + [(None, p) for p in improviso] + base[corte:]
        _codigo, out, _err = _alinhar("T04", [p for _, p in pares])
        self.assertEqual(s.tempos_esperados(pares)[ultimo], _tempos(out)[ultimo])

    def test_plano_A3_substituicao_nao_da_tempo(self):
        tokens = s.tokens("T01")
        pares = s.palavras_dos_tokens(tokens, {1: ["mal"]})  # "bem" -> "mal"
        _codigo, out, _err = _alinhar("T01", [p for _, p in pares])
        tempos = _tempos(out)
        self.assertIsNone(tempos[1])
        self.assertEqual((0, 250), tempos[0])
        self.assertEqual((600, 850), tempos[2])


class DesempateMinimoTest(unittest.TestCase):
    """Casos mínimos com tokens do T02 (achados por busca de mutações na prova do plano) que travam o D6 e o A3:
    menos edições primeiro; depois menos trechos (inclusive o de inserção no começo); depois a primeira forma."""

    def _tempos(self, inicio, fim, palavras):
        tokens = [roteiro.Token(k, t.inicio, t.fim, t.formas) for k, t in enumerate(s.tokens("T02")[inicio:fim])]
        transcricao = [alinhar.Palavra(p, 300 * n, 300 * n + 250) for n, p in enumerate(palavras)]
        pares, inseridas = alinhar.alinhar(tokens, transcricao)
        return alinhar.tempos_dos_tokens(tokens, transcricao, pares), inseridas

    def test_plano_A3_uma_edicao_vale_mais_que_os_trechos(self):
        tempos, inseridas = self._tempos(7, 9, ["mais", "livro", "de", "comecar", "fala"])
        self.assertEqual([(0, 250), (600, 850)], tempos)
        self.assertEqual([1, 3, 4], inseridas)

    def test_plano_A3_insercao_no_comeco_tambem_e_trecho(self):
        tempos, inseridas = self._tempos(53, 58, ["muita", "versiculo", "dois", "tambem", "vale", "ler", "corintios"])
        self.assertEqual([(0, 850), (900, 1150), (1200, 1450), (1500, 1750), (1800, 2050)], tempos)
        self.assertEqual([], inseridas)

    def test_plano_A3_empate_de_formas_fica_com_a_primeira(self):
        tempos, inseridas = self._tempos(56, 60, ["ler", "treze", "em", "quatro", "a", "sete", "do"])
        self.assertEqual([(0, 250), None, (600, 1750), None], tempos)
        self.assertEqual([], inseridas)


class RelatorioDoAlinhamentoTest(unittest.TestCase):
    def test_rodada2_insercao_no_comeco_aparece_no_stderr(self):
        """OPR-037 (Y08): duas palavras antes da leitura (improviso de abertura) são listadas na revisão manual."""
        palavras = ["livro", "fala"] + [p for _, p in s.palavras_dos_tokens(s.tokens("T01"))]
        codigo, out, err = _alinhar("T01", palavras)
        self.assertEqual(0, codigo)
        self.assertEqual((600, 850), _tempos(out)[0])
        self.assertIn("palavras inseridas: 2\n", err)
        self.assertIn("inseridas: palavras 0-1 [0-550 ms] livro fala\n", err)

    def test_rodada2_faixas_de_tokens_sem_tempo_e_de_palavras_inseridas_no_stderr(self):
        """Varredura (alinhar.py:208-209): faixas consecutivas viram 'a-z', as separadas ficam separadas, e a 2ª e a
        3ª faixa também se fundem (não só a primeira)."""
        pares = s.palavras_dos_tokens(s.tokens("T01"))
        palavras = [p for i, p in pares if i not in (2, 3, 4, 8, 9, 100)]
        palavras[30:30] = ["livro", "fala"]  # duas inseridas seguidas, longe dos tokens omitidos
        palavras[60:60] = ["mal", "mais"]  # outras duas, em outra faixa
        palavras[150:150] = ["muita"]  # e uma sozinha
        codigo, out, err = _alinhar("T01", palavras)
        self.assertEqual(0, codigo)
        self.assertEqual(
            ["tokens com tempo: 209", "tokens sem tempo: 6", "palavras inseridas: 5",
             "sem tempo: tokens 2-4", "sem tempo: tokens 8-9", "sem tempo: tokens 100-100"],
            err.splitlines()[:6])
        inseridas = err.splitlines()[6:]
        self.assertEqual(3, len(inseridas), inseridas)
        self.assertTrue(inseridas[0].startswith("inseridas: palavras 30-31 [9000-9550 ms] livro fala"), inseridas[0])
        self.assertTrue(inseridas[1].startswith("inseridas: palavras 60-61 [18000-18550 ms] mal mais"), inseridas[1])
        self.assertTrue(inseridas[2].startswith("inseridas: palavras 150-150 [45000-45250 ms] muita"), inseridas[2])

    def test_rodada2_ultima_palavra_repetida_fica_com_a_primeira_vez(self):
        """OPR-037 (Y07): empate no fim da transcrição (a última palavra dita duas vezes): o token fica com a
        primeira ocorrência e a repetição é a inserida, como no meio do texto (A3)."""
        tokens = s.tokens("T01")
        palavras = [p for _, p in s.palavras_dos_tokens(tokens)]
        palavras += [palavras[-1]]
        codigo, out, err = _alinhar("T01", palavras)
        self.assertEqual(0, codigo, err)
        n = len(palavras) - 1
        self.assertEqual((300 * (n - 1), 300 * (n - 1) + 250), _tempos(out)[len(tokens) - 1])
        self.assertIn(f"inseridas: palavras {n}-{n} ", err)


class DesempateComTokensExplicitosTest(unittest.TestCase):
    """Casos mínimos achados pela varredura anti-recorrência da 0004 (rodada 2): cada um separa o alinhador original
    de uma quebra numérica em `_linha`/`alinhar` (índices de coluna, custo da inserção inicial). Os tokens são
    palavras soltas dos textos de teste; as saídas são as do D6/A3 (menos edições, depois menos trechos)."""

    def _alinhar(self, tokens, palavras):
        toks = [roteiro.Token(k, 0, 0, tuple((w,) for w in t)) for k, t in enumerate(tokens)]
        transcricao = [alinhar.Palavra(p, 300 * n, 300 * n + 250) for n, p in enumerate(palavras)]
        pares, inseridas = alinhar.alinhar(toks, transcricao)
        return alinhar.tempos_dos_tokens(toks, transcricao, pares), inseridas

    def test_rodada2_omissao_no_meio_e_insercao_no_fim(self):
        tempos, inseridas = self._alinhar(
            [("pessoa",), ("sai",), ("da",), ("conversa",), ("com",), ("a",)],
            ["pessoa", "conversa", "com", "a", "nao"])
        self.assertEqual([(0, 250), None, None, (300, 550), (600, 850), (900, 1150)], tempos)
        self.assertEqual([4], inseridas)

    def test_rodada2_dois_trechos_de_insercao_no_comeco_valem_um_trecho(self):
        tempos, inseridas = self._alinhar([("comeca",)], ["se", "comeca", "comeca"])
        self.assertEqual([(600, 850)], tempos)
        self.assertEqual([0, 1], inseridas)

    def test_rodada2_insercao_no_comeco_com_substituicao_adiante(self):
        tempos, inseridas = self._alinhar(
            [("rodeios",), ("outra",), ("parte",)], ["resposta", "ler", "rodeios", "parte", "muito"])
        self.assertEqual([(600, 850), None, None], tempos)
        self.assertEqual([0, 1], inseridas)

    def test_rodada2_tres_palavras_no_comeco_pagam_um_trecho_so(self):
        """O custo da inserção inicial cresce `estende` por palavra (A3): três palavras antes da leitura não
        custam mais que o alinhamento que as troca por substituições."""
        tempos, inseridas = self._alinhar([("ouvir",), ("bem",)], ["livro", "fala", "mal", "ouvir", "bem"])
        self.assertEqual([(900, 1150), (1200, 1450)], tempos)
        self.assertEqual([0, 1, 2], inseridas)


class GuardaTest(unittest.TestCase):
    def test_D8_texto_diferente_de_etapa0_textos_sai_com_2_sem_escrever(self):
        original = (roteiro.TEXTOS / "T01.txt").read_bytes()
        transcricao = s.transcricao_csv(["ouvir", "bem"])
        for nome, conteudo in (("mais", original + b"x"), ("menos", original[:-1])):
            with self.subTest(caso=nome), s.Pasta() as p:
                destino = p.dir / "saida.expected.csv"
                argv = [p.arquivo("T01.txt", conteudo), p.arquivo("t.csv", transcricao), "-o", str(destino)]
                codigo, out, err = s.rodar(alinhar.main, argv)
                self.assertEqual(2, codigo)
                self.assertEqual("", out)
                self.assertIn("etapa0/textos", err)
                self.assertFalse(destino.exists())

    def test_transcricao_invalida_sai_com_2(self):
        casos = {
            "cabeçalho": "word,start,end,conf\nouvir,0,250,1.0\n",
            "tempo não inteiro": "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,0.5,250,1.0\n",
            "fim antes do início": "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,300,250,1.0\n",
            "fora de ordem": "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,300,550,1.0\nbem,0,250,1.0\n",
            "campos": "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,0,250\n",
        }
        for nome, conteudo in casos.items():
            with self.subTest(caso=nome), s.Pasta() as p:
                argv = [str(roteiro.TEXTOS / "T01.txt"), p.arquivo("t.csv", conteudo)]
                codigo, out, _err = s.rodar(alinhar.main, argv)
                self.assertEqual(2, codigo)
                self.assertEqual("", out)

    def test_rodada2_transcricao_invalida_mais_casos(self):
        """OPR-036 (Y09, Y10, Y15): início negativo, conf que não é número e ordem pelo início."""
        cab = "palavra,t_inicio_ms,t_fim_ms,conf\n"
        casos = {
            "início negativo": cab + "ouvir,-5,250,1.0\n",
            "início -1": cab + "ouvir,-1,250,1.0\n",
            "conf não numérica": cab + "ouvir,0,250,abc\n",
            "início anterior ao da linha de cima, fim depois": cab + "ouvir,1000,1200,1.0\nbem,500,1500,1.0\n",
        }
        for nome, conteudo in casos.items():
            with self.subTest(caso=nome), s.Pasta() as p:
                codigo, out, err = s.rodar(alinhar.main, [str(roteiro.TEXTOS / "T01.txt"), p.arquivo("t.csv", conteudo)])
                self.assertEqual((2, ""), (codigo, out))
                self.assertIn("alinhar.py: transcrição:", err)

    def test_rodada2_mensagem_de_erro_aponta_a_linha_certa(self):
        """Varredura anti-recorrência (alinhar.py:50): a numeração das linhas de dados começa em 1."""
        cab = "palavra,t_inicio_ms,t_fim_ms,conf\n"
        conteudo = cab + "ouvir,0,250,1.0\nbem,300,550,1.0\ne,600,x,1.0\n"
        with s.Pasta() as p:
            codigo, out, err = s.rodar(alinhar.main, [str(roteiro.TEXTOS / "T01.txt"), p.arquivo("t.csv", conteudo)])
        self.assertEqual((2, ""), (codigo, out))
        self.assertEqual("alinhar.py: transcrição: linha 3 com número inválido\n", err)

    def test_rodada2_palavras_que_se_sobrepoem_sao_aceitas_se_o_inicio_nao_recua(self):
        """Y15: a ordem exigida é a do início, não a do fim da linha de cima."""
        conteudo = "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,0,1000,1.0\nbem,500,600,1.0\n"
        with s.Pasta() as p:
            codigo, out, err = s.rodar(alinhar.main, [str(roteiro.TEXTOS / "T01.txt"), p.arquivo("t.csv", conteudo)])
        self.assertEqual(0, codigo, err)
        self.assertEqual({0: (0, 1000), 1: (500, 600)}, {k: v for k, v in _tempos(out).items() if v})

    def test_rodada2_arquivo_ausente_ou_nao_utf8_sai_com_2(self):
        with s.Pasta() as p:
            texto = str(roteiro.TEXTOS / "T01.txt")
            invalido = p.arquivo("ruim.csv", b"palavra,t_inicio_ms,t_fim_ms,conf\n\xff\xfe,0,1,1.0\n")
            for nome, argv in (("transcrição ausente", [texto, str(p.dir / "nao_existe.csv")]),
                               ("texto ausente", [str(p.dir / "nao_existe.txt"), invalido]),
                               ("transcrição não UTF-8", [texto, invalido])):
                with self.subTest(caso=nome):
                    codigo, out, err = s.rodar(alinhar.main, argv)
                    self.assertEqual((2, ""), (codigo, out))
                    self.assertIn("alinhar.py:", err)

    def test_rodada2_transcricao_grande_demais_sai_com_2(self):
        """OPR-036 (Y12): a guarda recusa antes de montar o DP, em vez de estourar array('i') ou levar horas."""
        conteudo = "palavra,t_inicio_ms,t_fim_ms,conf\n" + "ouvir,0,250,1.0\n" * 25000
        with s.Pasta() as p:
            codigo, out, err = s.rodar(alinhar.main, [str(roteiro.TEXTOS / "T01.txt"), p.arquivo("t.csv", conteudo)])
        self.assertEqual((2, ""), (codigo, out))
        self.assertIn("grandes demais", err)

    def test_palavra_de_duracao_zero_e_aceita(self):
        with s.Pasta() as p:
            conteudo = "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,0,0,1.0\nbem,0,250,\n"
            codigo, out, err = s.rodar(alinhar.main, [str(roteiro.TEXTOS / "T01.txt"), p.arquivo("t.csv", conteudo)])
        self.assertEqual(0, codigo, err)
        self.assertEqual(["0", "ouvir", "0", "0"], s.linhas_csv(out)[1])

    def test_saida_em_arquivo_com_o(self):
        with s.Pasta() as p:
            destino = str(p.dir / "x.expected.csv")
            codigo, out, _err = _alinhar("T09", [w for _, w in s.palavras_dos_tokens(s.tokens("T09"))], destino)
            self.assertEqual(0, codigo)
            self.assertEqual("", out)
            self.assertTrue(Path(destino).read_text(encoding="utf-8").startswith("token,palavra,t_inicio_ms,t_fim_ms\n"))


class DeterminismoTest(unittest.TestCase):
    def test_D9_mesma_entrada_mesma_saida(self):
        par = s.paragrafos("T04")
        base = s.palavras_dos_tokens(s.tokens("T04"))
        corte = max(n for n, (i, _) in enumerate(base) if i in par[0]) + 1
        palavras = [p for _, p in base[:corte]] + [p for _, p in s.palavras_dos_tokens(s.tokens("T03"))]
        palavras += [p for _, p in base[corte:]]
        primeira = _alinhar("T04", palavras)
        segunda = _alinhar("T04", palavras)
        self.assertEqual(0, primeira[0])
        self.assertEqual(primeira, segunda)

    def test_D9_l30_sintetico_completo(self):
        tokens = s.tokens("L30")
        pares = s.palavras_dos_tokens(tokens)
        inicio = time.perf_counter()
        codigo, out, _err = _alinhar("L30", [p for _, p in pares])
        segundos = time.perf_counter() - inicio
        sys.stderr.write(f"\nD9: L30 sintético ({len(tokens)} tokens, {len(pares)} palavras) em {segundos:.2f} s\n")
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        self.assertEqual(len(tokens), len(tempos))
        self.assertTrue(all(v is not None for v in tempos.values()))


if __name__ == "__main__":
    unittest.main()
