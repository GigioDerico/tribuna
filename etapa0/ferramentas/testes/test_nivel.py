"""nivel.py com gravações sintéticas (spec 0005, D12, F4; impl_plan 0005, A6).

Base: level.csv a cada 200 ms (leitura 0,05; silêncio do app em [10.000, 40.000) com 0,002 e um pico de 0,004
em 20.000); relatório do metricas.py com deslocamento D (os t_ms do relatório estão no relógio do WAV).
"""

import hashlib
import json
import unittest
from pathlib import Path

import nivel
from testes import sintetico as s
from testes.apoio import Pasta, rodar

D = 1370


def _sha(caminho):
    return hashlib.sha256(Path(caminho).read_bytes()).hexdigest()


def _level(extras=None, fim=60000):
    linhas = ["t_ms,rms"]
    for t in range(200, fim, 200):
        rms = 0.002 if 10000 <= t < 40000 else 0.05
        rms = (extras or {}).get(t, rms)
        linhas.append(f"{t},{rms:.6f}")
    return "\n".join(linhas) + "\n"


def _hipoteses(linhas):
    """linhas: (t, kind, palavras, aceita)."""
    return "t_ms,session,kind,words,accepted\n" + "".join(
        f'{t},1,{k},"{p}",{1 if a else 0}\n' for t, k, p, a in linhas)


class Gravacoes:
    def __init__(self, raiz):
        self.pastas = {k: Path(raiz) / k for k in ("relatorios", "trajetorias", "eventos", "esperado")}
        for p in self.pastas.values():
            p.mkdir(parents=True, exist_ok=True)

    def gravacao(self, nome, level, hipoteses, eventos, movimentos=()):
        tp = self.pastas["trajetorias"]
        (tp / f"{nome}.level.csv").write_text(level, encoding="utf-8")
        hip = tp / f"{nome}.hypotheses.csv"
        hip.write_text(_hipoteses(hipoteses), encoding="utf-8")
        traj = tp / f"{nome}.trajectory.csv"
        traj.write_text(f"trajetoria {nome}\n", encoding="utf-8")
        esp = self.pastas["esperado"] / f"{nome}.expected.csv"
        esp.write_text(f"esperado {nome}\n", encoding="utf-8")
        ev = self.pastas["eventos"] / f"{nome}.events.csv"
        ev.write_text(s.eventos_csv(eventos), encoding="utf-8")
        entradas = [{"papel": p, "arquivo": c.name, "sha256": _sha(c)}
                    for p, c in (("trajetoria", traj), ("esperado", esp), ("eventos", ev), ("hipoteses", hip))]
        relatorio = {"deslocamento_ms": D, "entradas": entradas,
                     "criterios": {"salto_errado": {"por_movimento": [
                         {"t_ms": t + D, "cursor": 5, "move": "Short", "intervalo": k, "esperado_no_inicio": 4,
                          "classe": "alcance"} for t, k in movimentos]}}}
        (self.pastas["relatorios"] / f"{nome}.json").write_text(json.dumps(relatorio), encoding="utf-8")
        return self

    def rodar(self, *nomes):
        saida = self.pastas["relatorios"] / "nivel.json"
        argv = [*nomes, "--relatorios", str(self.pastas["relatorios"]), "--trajetorias", str(self.pastas["trajetorias"]),
                "--eventos", str(self.pastas["eventos"]), "--esperado", str(self.pastas["esperado"]), "--json", str(saida)]
        codigo, out, err = rodar(nivel.main, argv)
        dados = json.loads(saida.read_text(encoding="utf-8")) if saida.exists() else None
        return codigo, dados, out, err


SILENCIO = [(10000, "silencio_inicio", ""), (40000, "silencio_fim", "")]
PICO = {20000: 0.004}


class NivelTest(unittest.TestCase):
    def test_F4_sem_movimento_recomenda_zero_e_da_os_niveis(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level(PICO), [(5000, "partial", "ouvir", True)],
                                          SILENCIO)
            codigo, dados, out, err = g.rodar("T06_giorgio_sala_t1")
        self.assertEqual(0, codigo, err)
        r = dados["recomendacao"]
        self.assertEqual((0.0, None), (r["recomendacao"], r["candidato"]))
        n = dados["gravacoes"][0]["niveis"]
        self.assertEqual({"n": 150, "mediana": 0.002, "p95": 0.002, "maximo": 0.004}, n["silencio"])
        self.assertEqual(299 - 150, n["leitura"]["n"])
        self.assertEqual(0, n["pausa"]["n"])
        self.assertIn("| T06_giorgio_sala_t1 | silencio | 150 |", out)

    def test_F4_intervalo_fechado_no_inicio_e_aberto_no_fim(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({10000: 0.9, 40000: 0.8}), [], SILENCIO)
            _, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        self.assertEqual(0.9, dados["gravacoes"][0]["niveis"]["silencio"]["maximo"])
        self.assertEqual(0.8, dados["gravacoes"][0]["niveis"]["leitura"]["maximo"])

    def test_D12_movimento_por_parcial_no_silencio_da_candidato(self):
        hip = [(5000, "partial", "ouvir", True), (20100, "partial", "isso", True)]
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({**PICO, 30000: 0.006, 50000: 0.006}), hip,
                                          SILENCIO, movimentos=[(20300, "silencio")])
            codigo, dados, out, _ = g.rodar("T06_giorgio_sala_t1")
        self.assertEqual(1, codigo)
        mv = dados["gravacoes"][0]["movimentos"][0]
        self.assertEqual((20300, 20100, "partial", 0.004), (mv["t_app_ms"], mv["causa_t_ms"], mv["causa_kind"], mv["causa_nivel"]))
        r = dados["recomendacao"]
        self.assertEqual((None, 0.006, 0.004), (r["recomendacao"], r["candidato"], r["limite"]))
        self.assertEqual(0, dados["gravacoes"][0]["leitura_abaixo_do_candidato"])
        self.assertIn("verificar por reprodução", out)

    def test_D12_causa_e_a_ultima_aceita_e_o_nivel_e_o_da_linha_anterior(self):
        hip = [(19000, "partial", "a", True), (20000, "partial", "b", True), (20050, "partial", "c", False),
               (20060, "error", "Internal", True), (20300, "partial", "d", True)]
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({**PICO, 19800: 0.003}), hip, SILENCIO,
                                          movimentos=[(20300, "silencio")])
            _, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        mv = dados["gravacoes"][0]["movimentos"][0]
        self.assertEqual((20300, 0.002), (mv["causa_t_ms"], mv["causa_nivel"]))
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({**PICO, 19800: 0.003}), hip[:4], SILENCIO,
                                          movimentos=[(20100, "silencio")])
            _, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        mv = dados["gravacoes"][0]["movimentos"][0]
        self.assertEqual((20000, 0.004), (mv["causa_t_ms"], mv["causa_nivel"]))

    def test_D12_movimento_por_final_nao_da_candidato(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level(PICO), [(20100, "final", "isso", True)],
                                          SILENCIO, movimentos=[(20300, "silencio")])
            codigo, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        self.assertEqual(1, codigo)
        self.assertIsNone(dados["recomendacao"]["candidato"])
        self.assertIn("Final", dados["recomendacao"]["motivo"])

    def test_D12_movimento_no_improviso_nao_pede_limiar(self):
        eventos = [(10000, "improviso_inicio", ""), (40000, "improviso_fim", "")]
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T04_giorgio_sala_t1", _level(PICO), [(20100, "partial", "x", True)],
                                          eventos, movimentos=[(20300, "improviso")])
            codigo, dados, _, _ = g.rodar("T04_giorgio_sala_t1")
        self.assertEqual(0, codigo)
        self.assertEqual(0.0, dados["recomendacao"]["recomendacao"])
        self.assertEqual(1, len(dados["gravacoes"][0]["movimentos"]))

    def test_D12_candidato_olha_todas_as_gravacoes_e_o_maior_nivel_causador(self):
        pausa = [(10000, "pausa_toque", ""), (40000, "retoma_toque", "")]
        with Pasta() as p:
            g = Gravacoes(p.dir)
            g.gravacao("T06_giorgio_sala_t1", _level(PICO), [(20100, "partial", "x", True)], SILENCIO,
                       movimentos=[(20300, "silencio")])
            g.gravacao("T07_giorgio_sala_t1", _level({20000: 0.01, 30000: 0.012}), [(20100, "partial", "y", True)],
                       pausa, movimentos=[(20300, "pausa")])
            g.gravacao("T01_giorgio_sala_t1", _level({30000: 0.011}), [], [])
            codigo, dados, _, _ = g.rodar("T06_giorgio_sala_t1", "T07_giorgio_sala_t1", "T01_giorgio_sala_t1")
        self.assertEqual(1, codigo)
        self.assertEqual((0.01, 0.011), (dados["recomendacao"]["limite"], dados["recomendacao"]["candidato"]))

    def test_D12_nenhum_nivel_acima_do_causador(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({20000: 0.9}), [(20100, "partial", "x", True)],
                                          SILENCIO, movimentos=[(20300, "silencio")])
            _, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        self.assertIsNone(dados["recomendacao"]["candidato"])
        self.assertIn("nenhum nível", dados["recomendacao"]["motivo"])

    def test_entradas_invalidas_saem_com_2(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level(), [], SILENCIO)
            codigo, dados, out, err = g.rodar("T06_giorgio_sala_t1", "T07_giorgio_sala_t1")
            self.assertEqual((2, None, ""), (codigo, dados, out))
            self.assertIn("falta T07_giorgio_sala_t1.json", err)
            (g.pastas["eventos"] / "T06_giorgio_sala_t1.events.csv").write_text("t_ms,evento,detalhe\n", encoding="utf-8")
            codigo, _, _, err = g.rodar("T06_giorgio_sala_t1")
            self.assertEqual(2, codigo)
            self.assertIn("mudou ou sumiu", err)
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", "t_ms,rms\n200,2\n", [], SILENCIO)
            codigo, _, _, err = g.rodar("T06_giorgio_sala_t1")
            self.assertEqual(2, codigo)
            self.assertIn("level.csv", err)

    def test_estatisticas_p95_por_posto_mais_proximo(self):
        self.assertEqual({"n": 20, "mediana": 10.5, "p95": 19, "maximo": 20}, nivel.estatisticas(list(range(20, 0, -1))))
        self.assertEqual(20, nivel.estatisticas(list(range(1, 22)))["p95"])
        self.assertEqual({"n": 0, "mediana": None, "p95": None, "maximo": None}, nivel.estatisticas([]))

    def test_ajuda(self):
        codigo, out, _ = rodar(nivel.main, ["--help"])
        self.assertEqual(0, codigo)
        for termo in ("nomes", "--relatorios", "--trajetorias", "--eventos", "--json"):
            self.assertIn(termo, out)


if __name__ == "__main__":
    unittest.main()
