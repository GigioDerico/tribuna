"""Linha de comando de verdade (subprocesso) e a cadeia ponta a ponta (spec 0004, A5 e E13)."""

import json
import subprocess
import sys
import unittest

import roteiro
from testes import sintetico as s

FERRAMENTAS = roteiro.FERRAMENTAS


def _cli(ferramenta, *argv):
    return subprocess.run(
        [sys.executable, str(FERRAMENTAS / ferramenta), *argv],
        capture_output=True, text=True, encoding="utf-8", timeout=120, check=False,
    )


class AjudaTest(unittest.TestCase):
    def test_A5_ajuda_das_tres_ferramentas(self):
        esperado = {
            "timestamps.py": ["wav", "--modelo", "-o"],
            "alinhar.py": ["texto", "transcricao", "-o"],
            "metricas.py": ["trajetoria", "esperado", "eventos", "--deslocamento-ms", "--hipoteses", "--json"],
        }
        for ferramenta, termos in esperado.items():
            with self.subTest(ferramenta=ferramenta):
                r = _cli(ferramenta, "--help")
                self.assertEqual(0, r.returncode)
                for termo in termos:
                    self.assertIn(termo, r.stdout)

    def test_codigos_de_saida_pelo_processo(self):
        r = _cli("timestamps.py", "discurso.wav")
        self.assertEqual((2, ""), (r.returncode, r.stdout))
        with s.Pasta() as p:
            quase_t01 = (roteiro.TEXTOS / "T01.txt").read_bytes() + b"x"
            r = _cli("alinhar.py", p.arquivo("texto.txt", quase_t01), p.arquivo("t.csv", "x\n"))
            self.assertEqual((2, ""), (r.returncode, r.stdout))
            r = _cli("metricas.py", p.arquivo("a.csv", "x\n"), p.arquivo("b.csv", "y\n"), p.arquivo("c.csv", "z\n"))
            self.assertEqual((2, ""), (r.returncode, r.stdout))


class PontaAPontaTest(unittest.TestCase):
    def test_E13_transcricao_alinhar_metricas(self):
        tokens = s.tokens("T01")
        pares = s.palavras_dos_tokens(tokens)
        with s.Pasta() as p:
            transcricao = p.arquivo("T01_sintetico_sala_t1.words.csv", s.transcricao_csv([w for _, w in pares]))
            esperado = str(p.dir / "T01_sintetico_sala_t1.expected.csv")
            r = _cli("alinhar.py", str(roteiro.TEXTOS / "T01.txt"), transcricao, "-o", esperado)
            self.assertEqual(0, r.returncode, r.stderr)
            linhas = s.linhas_csv(open(esperado, encoding="utf-8").read())
            tempos = [(int(a), int(b)) for _t, _p, a, b in linhas[1:]]
            self.assertEqual(len(tokens), len(tempos))
            trajetoria = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
            argv = [p.arquivo("x.trajectory.csv", trajetoria), esperado, p.arquivo("x.events.csv", s.eventos_csv([]))]
            saida_json = str(p.dir / "r.json")
            r = _cli("metricas.py", *argv, "--json", saida_json)
            self.assertEqual(0, r.returncode, r.stdout + r.stderr)
            dados = json.load(open(saida_json, encoding="utf-8"))
        c = dados["criterios"]
        self.assertEqual(["PASSA", "PASSA", "NAO_SE_APLICA", "PASSA"],
                         [c[k]["veredito"] for k in ("atraso", "salto_errado", "recuperacao", "recuo")])
        self.assertGreater(c["atraso"]["amostras"], 600, "Q3")


if __name__ == "__main__":
    unittest.main()
