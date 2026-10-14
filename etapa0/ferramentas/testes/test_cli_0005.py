"""Linha de comando de verdade das ferramentas da 0005 e a cadeia sincronizar -> folha -> vizinhas -> metricas ->
veredito (spec 0005, D3 a D6, E1; impl_plan 0005, Tarefa 6 e Correções da revisão rodada 1, C1)."""

import csv
import hashlib
import io
import json
import subprocess
import sys
import unittest
from pathlib import Path

import alinhar
import folha
import metricas
import roteiro
import veredito
import vizinhas
from testes import sinal
from testes import sintetico as s
from testes.apoio import rodar
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
            "vizinhas.py": ["folha"],
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
                                     ("veredito.py", ["final", x]),
                                     ("vizinhas.py", [p.arquivo("x.folha.events.csv", "x\n")]), ("nivel.py", ["T01_giorgio_sala_t1", "--relatorios", p.arquivo("d", "")])):
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
            r = _cli("vizinhas.py", str(folha))
            self.assertEqual(0, r.returncode, r.stderr)
            for sufixo, desl in (("", d), (".m200", d - 200), (".p200", d + 200)):
                folha_corrida = p.dir / f"T06_giorgio_sala_t1{sufixo}.folha.events.csv"
                r = _cli("metricas.py", trajetoria, esperado, app, str(folha_corrida), "--deslocamento-ms", str(desl),
                         "--json", str(p.dir / f"T06_giorgio_sala_t1{sufixo}.json"))
                self.assertIn(r.returncode, (0, 1), r.stderr)
                na_wav = metricas.ler_eventos([folha_corrida.read_text(encoding="utf-8")], desl, 10**9)
                self.assertEqual(tempos[134][1], na_wav[0].t, "a folha de cada corrida fica no relógio do WAV")
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


D_VIVO = 2500


def _sha(caminho):
    return hashlib.sha256(Path(caminho).read_bytes()).hexdigest()


def _words(texto_id, tempos, extras=()):
    lista = [(t.formas[0][0], *tempos[t.indice]) for t in s.tokens(texto_id) if tempos[t.indice] is not None]
    lista = sorted(lista + list(extras), key=lambda x: (x[1], x[2]))
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(alinhar.CABECALHO_TRANSCRICAO)
    for palavra, ini, fim in lista:
        w.writerow([palavra, ini, fim, "1.0"])
    return saida.getvalue()


def _caso_t04():
    """Improviso depois do token 46 com respiro de 600 ms; o cursor chega 500 ms depois do fim de cada palavra."""
    n = len(s.tokens("T04"))
    tempos = [(6000 + 400 * i, 6300 + 400 * i) if i <= 46 else (34000 + 400 * (i - 47), 34300 + 400 * (i - 47))
              for i in range(n)]
    fim46 = tempos[46][1]
    extras = [("exemplo", fim46 + 600 + 400 * k, fim46 + 900 + 400 * k) for k in range(20)]
    traj = [(0, 0, "Reposition")] + [(tempos[i][1] + 500 - D_VIVO, i, "Short") for i in range(1, n)]
    return "T04_giorgio_sala_t1", tempos, extras, traj, "CS2"


def _caso_t05():
    """Pulo do T05 (47 a 90) com recuperação de 2.900 ms em d, medida do pulo_fim (25.700 ms do WAV)."""
    n = len(s.tokens("T05"))
    tempos = [None if 47 <= i <= 90 else
              ((6000 + 400 * i, 6300 + 400 * i) if i <= 46 else (25700 + 400 * (i - 91), 26000 + 400 * (i - 91)))
              for i in range(n)]
    traj = [(0, 0, "Reposition")] + [(tempos[i][1] + 500 - D_VIVO, i, "Short") for i in range(1, 47)]
    traj.append((25700 + 2900 - D_VIVO, 97, "LongJump"))
    traj += [(tempos[i][1] + 500 - D_VIVO, i, "Short") for i in range(98, n)]
    return "T05_giorgio_sala_t1", tempos, (), traj, "CS3"


class SensibilidadeDeVerdadeTest(unittest.TestCase):
    """Os dois casos do revisor (achado 1) pela cadeia real folha -> vizinhas -> metricas -> veredito."""

    def _rodada(self, raiz, caso, mesma_folha=False):
        nome, tempos, extras, traj, criterio = caso
        texto_id = nome.split("_")[0]
        r = Rodada(raiz)
        pa = r.pastas
        esp = pa["esperado"] / f"{nome}.expected.csv"
        esp.write_text(s.esperado_csv(texto_id, tempos), encoding="utf-8")
        words = pa["esperado"] / f"{nome}.words.csv"
        words.write_text(_words(texto_id, tempos, extras), encoding="utf-8")
        tr = pa["trajetorias"] / f"{nome}.trajectory.csv"
        tr.write_text(s.trajetoria_csv(traj), encoding="utf-8")
        app = pa["eventos"] / f"{nome}.events.csv"
        app.write_text(s.eventos_csv([]), encoding="utf-8")
        f_d = pa["eventos"] / f"{nome}.folha.events.csv"
        codigo, _, err = rodar(folha.main, [str(roteiro.TEXTOS / f"{texto_id}.txt"), str(esp), str(words),
                                            "--deslocamento-ms", str(D_VIVO), "-o", str(f_d)])
        self.assertIn(codigo, (0, 1), err)
        self.assertEqual(0, rodar(vizinhas.main, [str(f_d)])[0])
        for sufixo, desl in (("", D_VIVO), (".m200", D_VIVO - 200), (".p200", D_VIVO + 200)):
            f = f_d if mesma_folha else pa["eventos"] / f"{nome}{sufixo}.folha.events.csv"
            codigo, _, err = rodar(metricas.main, [str(tr), str(esp), str(app), str(f), "--deslocamento-ms", str(desl),
                                                   "--json", str(pa["relatorios"] / f"{nome}{sufixo}.json")])
            self.assertIn(codigo, (0, 1), err)
        wav, level = pa["gravacoes"] / f"{nome}.wav", pa["trajetorias"] / f"{nome}.level.csv"
        wav.write_bytes(b"RIFF" + nome.encode())
        level.write_text("t_ms,rms\n200,0.1\n", encoding="utf-8")
        sinc = {"deslocamento_ms": D_VIVO, "motivos": [],
                "entradas": [{"papel": "wav", "arquivo": wav.name, "sha256": _sha(wav)},
                             {"papel": "level", "arquivo": level.name, "sha256": _sha(level)}]}
        (pa["relatorios"] / f"{nome}.sinc.json").write_text(json.dumps(sinc), encoding="utf-8")
        meta = {**veredito.CONFIG_OFICIAL, "text_id": texto_id}
        (pa["trajetorias"] / f"{nome}.meta.txt").write_text("".join(f"{k}={v}\n" for k, v in meta.items()),
                                                           encoding="utf-8")
        (pa["esperado"] / f"{nome}.revisao.md").write_text(veredito.REVISAO_CONCLUIDA + "\n", encoding="utf-8")
        r.oficiais = [(nome, "")]
        codigo, dados, _, err = r.rodar()
        if codigo == 2:
            return None, err
        g = dados["gravacoes"][0]
        return g["criterios"][criterio], [json.loads((pa["relatorios"] / f"{nome}{suf}.json").read_text(
            encoding="utf-8"))["criterios"] for suf in ("", ".m200", ".p200")]

    def test_D3_alcance_no_comeco_do_improviso_sai_sensivel(self):
        with s.Pasta() as p:
            v, crit = self._rodada(p.dir, _caso_t04())
        self.assertEqual(["PASSA", "PASSA", "REPROVA"], [c["salto_errado"]["veredito"] for c in crit])
        self.assertEqual([("improviso", 46)], [(m["intervalo"], m["cursor"])
                                               for m in crit[2]["salto_errado"]["por_movimento"]])
        self.assertEqual("SENSÍVEL AO DESLOCAMENTO", v["estado"])
        self.assertIn("com d−200 e d+200: PASSOU, REPROVOU", v["motivo"])

    def test_D3_recuperacao_de_2900_ms_sai_sensivel(self):
        with s.Pasta() as p:
            v, crit = self._rodada(p.dir, _caso_t05())
        self.assertEqual([2900, 2700, 3100], [c["recuperacao"]["pulos"][0]["recuperacao_ms"] for c in crit])
        self.assertEqual("SENSÍVEL AO DESLOCAMENTO", v["estado"])

    def test_D3_a_mesma_folha_nas_tres_corridas_sai_com_2(self):
        for caso in (_caso_t04(), _caso_t05()):
            with self.subTest(caso=caso[0]), s.Pasta() as p:
                v, err = self._rodada(p.dir, caso, mesma_folha=True)
                self.assertIsNone(v)
                self.assertIn(f"a corrida m200 usou {caso[0]}.folha.events.csv como eventos", err)


if __name__ == "__main__":
    unittest.main()
