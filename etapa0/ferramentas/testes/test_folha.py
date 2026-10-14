"""folha.py com esperados e transcrições sintéticos (spec 0005, D5, D6, D7; impl_plan 0005, A4).

Relógio do WAV: token i em [300i, 300i + 250], mais GAP ms para os tokens depois da fronteira. A folha sai no
relógio do app com deslocamento D (t_app = t_wav − D).
"""

import csv
import io
import unittest

import alinhar
import folha
import metricas
import roteiro
from testes import sintetico as s
from testes.apoio import Pasta, rodar

D = 1370
GAP = 60000


def _tempos(n, depois_de, sem_tempo=(), gap=GAP):
    saida = []
    for i in range(n):
        if i in sem_tempo:
            saida.append(None)
            continue
        extra = gap if i > depois_de else 0
        saida.append((300 * i + extra, 300 * i + 250 + extra))
    return saida


def _palavras(texto_id, tempos, extras=()):
    """Uma palavra (1ª da 1ª forma) por token com tempo, mais `extras` [(palavra, ini, fim)], em ordem."""
    tokens = s.tokens(texto_id)
    lista = [(t.formas[0][0], *tempos[t.indice]) for t in tokens if tempos[t.indice] is not None]
    lista += list(extras)
    lista.sort(key=lambda p: (p[1], p[2]))
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(alinhar.CABECALHO_TRANSCRICAO)
    for palavra, ini, fim in lista:
        w.writerow([palavra, ini, fim, "1.0"])
    return saida.getvalue()


def _rodar(texto_id, tempos, extras=(), deslocamento=D, texto=None, esperado_de=None):
    with Pasta() as p:
        conteudo = texto if texto is not None else (roteiro.TEXTOS / f"{texto_id}.txt").read_bytes()
        argv = [p.arquivo("texto.txt", conteudo),
                p.arquivo("x.expected.csv", s.esperado_csv(esperado_de or texto_id, tempos)),
                p.arquivo("x.words.csv", _palavras(esperado_de or texto_id, tempos, extras)),
                "--deslocamento-ms", str(deslocamento)]
        codigo, out, err = rodar(folha.main, argv)
    return codigo, [tuple(linha) for linha in csv.reader(io.StringIO(out))], err


def _improviso(ini, fim, n=10):
    passo = (fim - ini) // n
    return [("palavra", ini + k * passo, ini + k * passo + passo // 2) for k in range(n - 1)] + [("fim", fim - 100, fim)]


N = len(s.tokens("T01"))
NL = len(s.tokens("L30"))


class ImprovisoTest(unittest.TestCase):
    def test_D6_t04_improviso_da_primeira_a_ultima_palavra_inserida(self):
        tempos = _tempos(N, 46)
        fim46, ini47 = tempos[46][1], tempos[47][0]
        extras = _improviso(fim46 + 2000, ini47 - 1500)
        codigo, linhas, err = _rodar("T04", tempos, extras)
        self.assertEqual(0, codigo, err)
        self.assertEqual([("t_ms", "evento", "detalhe"),
                          (str(fim46 + 2000 - D), "improviso_inicio", ""),
                          (str(ini47 - 1500 - D), "improviso_fim", "")], linhas)
        self.assertEqual(2, err.count("revisar: "))
        self.assertIn(f"revisar: improviso_inicio em {fim46 + 2000} ms do WAV ({fim46 + 2000 - D} ms do app)", err)
        self.assertIn("'conversa' (token 46)", err)

    def test_D6_palavras_encostadas_nas_fronteiras_entram(self):
        tempos = _tempos(N, 46)
        fim46, ini47 = tempos[46][1], tempos[47][0]
        extras = [("primeira", fim46, fim46 + 300), ("meio", fim46 + 5000, fim46 + 5300), ("ultima", ini47 - 300, ini47)]
        codigo, linhas, _ = _rodar("T04", tempos, extras)
        self.assertEqual(0, codigo)
        self.assertEqual((str(fim46 - D), "improviso_inicio", ""), linhas[1])
        self.assertEqual((str(ini47 - D), "improviso_fim", ""), linhas[2])

    def test_D6_palavra_que_atravessa_a_fronteira_fica_fora(self):
        tempos = _tempos(N, 46)
        fim46, ini47 = tempos[46][1], tempos[47][0]
        extras = [("cedo", fim46 - 1, fim46 + 300), ("meio", fim46 + 5000, fim46 + 5300), ("tarde", ini47 - 300, ini47 + 1)]
        codigo, linhas, _ = _rodar("T04", tempos, extras)
        self.assertEqual(0, codigo)
        self.assertEqual([(str(fim46 + 5000 - D), "improviso_inicio", ""), (str(fim46 + 5300 - D), "improviso_fim", "")],
                         linhas[1:])

    def test_D6_sem_palavra_no_improviso_sai_com_2(self):
        codigo, linhas, err = _rodar("T04", _tempos(N, 46))
        self.assertEqual((2, []), (codigo, linhas))
        self.assertIn("improviso: nenhuma palavra", err)

    def test_D7_fronteira_em_token_sem_tempo_avisa_e_sai_com_1(self):
        tempos = _tempos(N, 46, sem_tempo=(46,))
        fim45, ini47 = tempos[45][1], tempos[47][0]
        extras = _improviso(fim45 + 2000, ini47 - 1500)
        codigo, linhas, err = _rodar("T04", tempos, extras)
        self.assertEqual(1, codigo)
        self.assertEqual(str(fim45 + 2000 - D), linhas[1][0])
        self.assertIn("aviso: improviso: o token 46 ('conversa') não tem tempo; a fronteira usa o token 45 ('na')", err)


class PuloTest(unittest.TestCase):
    def _tempos_t05(self, sem_tempo=()):
        pulados = set(range(47, 91))
        return _tempos(N, 46, sem_tempo=pulados | set(sem_tempo), gap=-300 * 44 + 400)

    def test_D6_t05_pulo_com_indice_do_primeiro_pulado(self):
        tempos = self._tempos_t05()
        codigo, linhas, err = _rodar("T05", tempos)
        self.assertEqual(0, codigo, err)
        self.assertEqual([("t_ms", "evento", "detalhe"),
                          (str(tempos[46][1] - D), "pulo_inicio", "47"),
                          (str(tempos[91][0] - D), "pulo_fim", "")], linhas)
        self.assertIn("primeiro pulado: token 47", err)

    def test_D7_primeiro_token_depois_do_pulo_sem_tempo_avisa(self):
        tempos = self._tempos_t05(sem_tempo=(91,))
        codigo, linhas, err = _rodar("T05", tempos)
        self.assertEqual(1, codigo)
        self.assertEqual((str(tempos[92][0] - D), "pulo_fim", ""), linhas[2])
        self.assertIn("o token 91 ('Ouvir') não tem tempo; a fronteira usa o token 92", err)

    def test_D7_token_do_paragrafo_pulado_com_tempo_avisa(self):
        tempos = self._tempos_t05()
        tempos[47] = (tempos[46][1] + 10, tempos[46][1] + 20)
        codigo, _, err = _rodar("T05", tempos)
        self.assertEqual(1, codigo)
        self.assertIn("aviso: pulo: 1 token(s) do parágrafo pulado com tempo (primeiro: token 47)", err)

    def test_D5_metricas_aceita_a_folha_do_pulo(self):
        tempos = self._tempos_t05()
        _codigo, linhas, _ = _rodar("T05", tempos)
        folha_csv = "\n".join(",".join(linha) for linha in linhas) + "\n"
        trajetoria = s.trajetoria_csv([(0, 0, "Reposition")])
        codigo, dados, _, err = s.rodar_metricas(metricas.main, trajetoria, s.esperado_csv("T05", tempos),
                                                 [s.eventos_csv([]), folha_csv], deslocamento=D)
        self.assertNotEqual(2, codigo, err)
        self.assertEqual(47, dados["criterios"]["recuperacao"]["pulos"][0]["primeiro_pulado"])
        self.assertEqual(tempos[91][0], dados["criterios"]["recuperacao"]["pulos"][0]["pulo_fim_ms"])


class SilencioTest(unittest.TestCase):
    def test_D6_t06_silencio_entre_os_tokens(self):
        tempos = _tempos(N, 134, gap=30000)
        codigo, linhas, err = _rodar("T06", tempos)
        self.assertEqual(0, codigo, err)
        self.assertEqual([("t_ms", "evento", "detalhe"),
                          (str(tempos[134][1] - D), "silencio_inicio", ""),
                          (str(tempos[135][0] - D), "silencio_fim", "")], linhas)
        self.assertIn("'rodeios' (token 134)", err)

    def test_D7_palavra_dentro_do_silencio_avisa(self):
        tempos = _tempos(N, 134, gap=30000)
        extras = [("tosse", tempos[134][1] + 1, tempos[134][1] + 400)]
        codigo, _, err = _rodar("T06", tempos, extras)
        self.assertEqual(1, codigo)
        self.assertIn("aviso: silencio: 1 palavra(s) da transcrição dentro do silêncio", err)

    def test_tokens_da_fronteira_sobrepostos_saem_com_2(self):
        tempos = _tempos(N, 134, gap=30000)
        tempos[135] = (tempos[134][1] - 1, tempos[134][1] + 100)
        codigo, _, err = _rodar("T06", tempos)
        self.assertEqual(2, codigo)
        self.assertIn("começa antes do fim do token 134", err)

    def test_D7_palavra_encostada_no_silencio_nao_avisa(self):
        tempos = _tempos(N, 134, gap=30000)
        extras = [("antes", tempos[134][1], tempos[134][1] + 400), ("depois", tempos[135][0] - 400, tempos[135][0])]
        codigo, _, err = _rodar("T06", tempos, extras)
        self.assertEqual(0, codigo, err)


class L30Test(unittest.TestCase):
    def test_D5_l30_tres_eventos_em_ordem_e_metricas_aceita(self):
        tempos = [(300 * i, 300 * i + 250) for i in range(NL)]
        for i in range(1121, NL):
            tempos[i] = (tempos[i][0] + 60000, tempos[i][1] + 60000)
        for i in range(1804, 1899):
            tempos[i] = None
        for i in range(2428, NL):
            tempos[i] = (tempos[i][0] + 30000, tempos[i][1] + 30000)
        extras = _improviso(tempos[1120][1] + 1000, tempos[1121][0] - 1000)
        codigo, linhas, err = _rodar("L30", tempos, extras)
        self.assertEqual(0, codigo, err)
        self.assertEqual(["improviso_inicio", "improviso_fim", "pulo_inicio", "pulo_fim", "silencio_inicio",
                          "silencio_fim"], [linha[1] for linha in linhas[1:]])
        self.assertEqual([tempos[1120][1] + 1000 - D, tempos[1121][0] - 1000 - D, tempos[1803][1] - D,
                          tempos[1899][0] - D, tempos[2427][1] - D, tempos[2428][0] - D],
                         [int(linha[0]) for linha in linhas[1:]])
        self.assertEqual("1804", linhas[3][2])
        self.assertEqual(6, err.count("revisar: "))
        app = s.eventos_csv([(tempos[1500][1] - D, "pausa_toque", ""), (tempos[1501][0] - D, "retoma_toque", "")])
        folha_csv = "\n".join(",".join(linha) for linha in linhas) + "\n"
        codigo_m, dados, _, err_m = s.rodar_metricas(metricas.main, s.trajetoria_csv([(0, 0, "Reposition")]),
                                                     s.esperado_csv("L30", tempos), [app, folha_csv], deslocamento=D)
        self.assertNotEqual(2, codigo_m, err_m)
        self.assertEqual(1, len(dados["criterios"]["recuperacao"]["pulos"]))


class EntradaTest(unittest.TestCase):
    def test_texto_sem_evento_pela_voz_sai_com_2(self):
        codigo, _, err = _rodar("T01", _tempos(N, 46))
        self.assertEqual(2, codigo)
        self.assertIn("T01 não tem evento pela voz", err)

    def test_texto_que_nao_e_de_teste_sai_com_2(self):
        quase = (roteiro.TEXTOS / "T04.txt").read_bytes() + b" "
        codigo, _, err = _rodar("T04", _tempos(N, 46), texto=quase)
        self.assertEqual(2, codigo)
        self.assertIn("não é idêntico", err)

    def test_esperado_de_outro_texto_sai_com_2(self):
        n9 = len(s.tokens("T09"))
        codigo, _, err = _rodar("T04", [(300 * i, 300 * i + 250) for i in range(n9)], esperado_de="T09")
        self.assertEqual(2, codigo)
        self.assertIn("não é do texto T04", err)

    def test_deslocamento_grande_demais_sai_com_2(self):
        tempos = _tempos(N, 134, gap=30000)
        codigo, _, err = _rodar("T06", tempos, deslocamento=tempos[134][1] + 1)
        self.assertEqual(2, codigo)
        self.assertIn("antes do início da gravação do app", err)

    def test_deslocamento_no_limite_aceito(self):
        tempos = _tempos(N, 134, gap=30000)
        codigo, linhas, _ = _rodar("T06", tempos, deslocamento=tempos[134][1])
        self.assertEqual(0, codigo)
        self.assertEqual(("0", "silencio_inicio", ""), linhas[1])

    def test_entradas_invalidas_saem_com_2(self):
        with Pasta() as p:
            texto = str(roteiro.TEXTOS / "T06.txt")
            bom_esp = p.arquivo("e.csv", s.esperado_csv("T06", _tempos(N, 134, gap=30000)))
            bom_pal = p.arquivo("w.csv", _palavras("T06", _tempos(N, 134, gap=30000)))
            casos = {
                "sem deslocamento": [texto, bom_esp, bom_pal],
                "esperado ruim": [texto, p.arquivo("e2.csv", "x\n"), bom_pal, "--deslocamento-ms", "0"],
                "transcrição ruim": [texto, bom_esp, p.arquivo("w2.csv", "x\n"), "--deslocamento-ms", "0"],
                "arquivo ausente": [texto, str(p.dir / "nada.csv"), bom_pal, "--deslocamento-ms", "0"],
            }
            for nome, argv in casos.items():
                with self.subTest(caso=nome):
                    codigo, out, _ = rodar(folha.main, argv)
                    self.assertEqual((2, ""), (codigo, out))

    def test_nota_repetida_sai_com_erro(self):
        texto = roteiro.ler_texto("T04") + "\n// IMPROVISAR de novo\n\nOutra frase.\n"
        with self.assertRaises(folha.ErroEntrada):
            folha.fronteiras("T04", texto, roteiro.tokenizar(texto))

    def test_saida_em_arquivo(self):
        tempos = _tempos(N, 134, gap=30000)
        with Pasta() as p:
            saida = p.dir / "T06_giorgio_sala_t1.folha.events.csv"
            argv = [str(roteiro.TEXTOS / "T06.txt"), p.arquivo("e.csv", s.esperado_csv("T06", tempos)),
                    p.arquivo("w.csv", _palavras("T06", tempos)), "--deslocamento-ms", str(D), "-o", str(saida)]
            codigo, out, _ = rodar(folha.main, argv)
            self.assertEqual((0, ""), (codigo, out))
            self.assertTrue(saida.read_text(encoding="utf-8").startswith("t_ms,evento,detalhe\n"))

    def test_blocos_e_notas(self):
        b = folha.blocos("a b\nc\n\n// nota\n\n\nfim")
        self.assertEqual([(0, 5, False), (7, 14, True), (17, 20, False)], [(x.ini, x.fim, x.nota) for x in b])

    def test_ajuda(self):
        codigo, out, _ = rodar(folha.main, ["--help"])
        self.assertEqual(0, codigo)
        for termo in ("texto", "esperado", "transcricao", "--deslocamento-ms", "-o"):
            self.assertIn(termo, out)



class GuardasTest(unittest.TestCase):
    """Achado 7 da revisão (RF08, RF12, RF13; OPR-044)."""

    def test_sem_token_com_tempo_antes_ou_depois_da_fronteira_sai_com_2(self):
        for nome, sem_tempo in (("antes", range(0, 135)), ("depois", range(135, N))):
            with self.subTest(lado=nome):
                codigo, linhas, err = _rodar("T06", _tempos(N, 134, sem_tempo=set(sem_tempo), gap=30000))
                self.assertEqual((2, []), (codigo, linhas))
                self.assertIn("silencio: nenhum token com tempo antes do token 134 ou depois do token 135", err)

    def test_nota_no_comeco_ou_no_fim_do_texto_sai_com_erro(self):
        base = roteiro.ler_texto("T06").replace("// SILÊNCIO", "// NOTA")
        for texto in ("// SILÊNCIO de 30 s\n\n" + base, base + "\n\n// SILÊNCIO de 30 s\n"):
            with self.subTest(texto=texto[:20]):
                with self.assertRaises(folha.ErroEntrada) as erro:
                    folha.fronteiras("T06", texto, roteiro.tokenizar(texto))
                self.assertIn("não fica entre dois tokens", str(erro.exception))

    def test_D7_ultimo_token_pulado_com_tempo_avisa(self):
        tempos = PuloTest()._tempos_t05()
        tempos[90] = (tempos[91][0] - 200, tempos[91][0] - 100)
        codigo, _, err = _rodar("T05", tempos)
        self.assertEqual(1, codigo)
        self.assertIn("aviso: pulo: 1 token(s) do parágrafo pulado com tempo (primeiro: token 90)", err)


if __name__ == "__main__":
    unittest.main()
