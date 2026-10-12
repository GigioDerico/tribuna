"""Linha de comando de verdade das ferramentas da 0005 e a cadeia sincronizar -> folha -> metricas -> veredito
(spec 0005, D3 a D6, E1; impl_plan 0005, Tarefa 6)."""

import csv
import io
import json
import subprocess
import sys
import unittest

import roteiro
import veredito
from testes import sinal
from testes import sintetico as s
from testes.test_veredito import Rodada

FERRAMENTAS = roteiro.FERRAMENTAS


def _cli(ferramenta, *argv):
    return subprocess.run([sys.executable, str(FERRAMENTAS / ferramenta), *argv],
                          capture_output=True, text=True, encoding="utf-8", timeout=120, check=False)


class AjudaTest(unittest.TestCase):
    def test_ajuda_e_codigo_2_pelo_processo(self):
        esperado = {
            "sincronizar.py": ["wav", "level", "--faixa-ms", "--palma-ate-ms", "--json"],
            "folha.py": ["texto", "esperado", "transcricao", "--deslocamento-ms", "-o"],
            "veredito.py": ["rodada", "final"],
            "nivel.py": ["nomes", "--relatorios", "--json"],
        }
        for ferramenta, termos in esperado.items():
            with self.subTest(ferramenta=ferramenta):
                r = _cli(ferramenta, "--help")
                self.assertEqual(0, r.returncode)
                for termo in termos:
                    self.assertIn(termo, r.stdout)
        with s.Pasta() as p:
            x = p.arquivo("x.csv", "x\n")
            for ferramenta, argv in (("sincronizar.py", [x, x]), ("folha.py", [x, x, x, "--deslocamento-ms", "0"]),
                                     ("veredito.py", ["final", x]), ("nivel.py", ["T01_giorgio_sala_t1", "--relatorios", p.arquivo("d", "")])):
                with self.subTest(ferramenta=ferramenta):
                    r = _cli(ferramenta, *argv)
                    self.assertEqual((2, ""), (r.returncode, r.stdout))


class CadeiaTest(unittest.TestCase):
    def test_E1_sincronizar_folha_metricas_com_o_mesmo_deslocamento(self):
        d_real = 1370
        amostras = sinal.fala_sintetica(40000, 3, palma_ms=d_real + 2000, silencio_ate_ms=d_real + 4000)
        n = len(s.tokens("T06"))
        # esperado no relógio do WAV: leitura começa em d + 4000; silêncio de 10 s depois do token 134
        tempos = []
        for i in range(n):
            extra = 10000 if i > 134 else 0
            ini = d_real + 4000 + 100 * i + extra
            tempos.append((ini, ini + 80))
        with s.Pasta() as p:
            wav = sinal.escrever_wav(p.dir / "T06_giorgio_sala_t1.wav", amostras)
            level = p.arquivo("T06_giorgio_sala_t1.level.csv", sinal.level_csv(amostras, d_real, 38000, ganho=0.3))
            r = _cli("sincronizar.py", wav, level, "--json", str(p.dir / "s.json"))
            self.assertEqual(0, r.returncode, r.stderr)
            d = json.loads((p.dir / "s.json").read_text(encoding="utf-8"))["deslocamento_ms"]
            self.assertLessEqual(abs(d - d_real), 10)

            esperado = p.arquivo("T06_giorgio_sala_t1.expected.csv", s.esperado_csv("T06", tempos))
            palavras = io.StringIO()
            w = csv.writer(palavras, lineterminator="\n")
            w.writerow(["palavra", "t_inicio_ms", "t_fim_ms", "conf"])
            for t, (ini, fim) in zip(s.tokens("T06"), tempos):
                w.writerow([t.formas[0][0], ini, fim, "1.0"])
            words = p.arquivo("T06_giorgio_sala_t1.words.csv", palavras.getvalue())
            texto = str(roteiro.TEXTOS / "T06.txt")
            folha = p.dir / "T06_giorgio_sala_t1.folha.events.csv"
            r = _cli("folha.py", texto, esperado, words, "--deslocamento-ms", str(d), "-o", str(folha))
            self.assertEqual(0, r.returncode, r.stderr)
            linhas = list(csv.reader(io.StringIO(folha.read_text(encoding="utf-8"))))
            self.assertEqual(tempos[134][1] - d, int(linhas[1][0]))

            # trajetória no relógio do app: o cursor chega a cada token 10 ms depois do início dele
            traj = [(0, 0, "Reposition")]
            for i in range(1, n):
                traj.append((tempos[i][0] - d + 10, i, "Short"))
            trajetoria = p.arquivo("T06_giorgio_sala_t1.trajectory.csv", s.trajetoria_csv(traj))
            app = p.arquivo("T06_giorgio_sala_t1.events.csv", s.eventos_csv([]))
            for sufixo, desl in (("", d), (".m200", d - 200), (".p200", d + 200)):
                r = _cli("metricas.py", trajetoria, esperado, app, str(folha), "--deslocamento-ms", str(desl),
                         "--json", str(p.dir / f"T06_giorgio_sala_t1{sufixo}.json"))
                self.assertIn(r.returncode, (0, 1), r.stderr)
            dados = json.loads((p.dir / "T06_giorgio_sala_t1.json").read_text(encoding="utf-8"))
            self.assertEqual("PASSA", dados["criterios"]["salto_errado"]["veredito"])
            self.assertGreater(dados["amostras"]["excluidas_intervalo"], 90, "o silêncio da folha foi aplicado")


class VereditoPeloProcessoTest(unittest.TestCase):
    def test_H1_rodada_e_final_pelo_processo(self):
        with s.Pasta() as p:
            r = Rodada(p.dir / "S1").completa()
            argv = ["rodada", "--rodada", "S1", "--oficiais", str(r.tsv()), "--relatorios", str(r.pastas["relatorios"]),
                    "--trajetorias", str(r.pastas["trajetorias"]), "--eventos", str(r.pastas["eventos"]),
                    "--esperado", str(r.pastas["esperado"]), "--gravacoes", str(r.pastas["gravacoes"]),
                    "--json", str(p.dir / "S1.json")]
            res = _cli("veredito.py", *argv)
            self.assertEqual(0, res.returncode, res.stderr)
            res = _cli("veredito.py", "final", str(p.dir / "S1.json"))
            self.assertEqual(1, res.returncode)
            self.assertIn(f"| CS1 | {veredito.NAO_MEDIDO} |", res.stdout)


if __name__ == "__main__":
    unittest.main()
