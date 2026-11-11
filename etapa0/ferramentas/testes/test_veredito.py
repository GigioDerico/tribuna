"""veredito.py com rodadas sintéticas em pasta temporária (spec 0005, D3, D7, D8, D9, D10; impl_plan 0005, A5).

O `Rodada` escreve, para cada gravação, arquivos de entrada com conteúdo único, a folha de d e as das corridas
vizinhas (a mesma folha no relógio do WAV: t + 200 na de d − 200 e t − 200 na de d + 200), os três JSON do
metricas.py (d, d − 200, d + 200) com a proveniência certa, o JSON do sincronizar.py, o meta.txt e a revisão.
A trajetória é um CSV válido em que o cursor anda em 100.000 ms do app, depois de todos os intervalos padrão.
Um teste roda o metricas.py de verdade para travar os campos que o veredito lê (Q4).
"""

import copy
import hashlib
import json
import unittest
from pathlib import Path

import metricas
import veredito
from testes import sintetico as s
from testes.apoio import Pasta, rodar

D = 1370
META_OFICIAL = dict(veredito.CONFIG_OFICIAL)
TRAJETORIA = [(0, 0, "Reposition"), (100000, 150, "Short")]
VIZINHAS = {"m200": 200, "p200": -200}  # soma ao t da folha de d (corridas d − 200 e d + 200)


def _sha(caminho):
    return hashlib.sha256(Path(caminho).read_bytes()).hexdigest()


def metricas_json(atraso="PASSA", amostras=600, salto="PASSA", movimentos=(), recuo="PASSA",
                  recuperacao="NAO_SE_APLICA", cursor=150, leitura=600):
    """Recorte do JSON do metricas.py com os campos que o veredito lê."""
    return {
        "criterios": {
            "atraso": {"veredito": atraso, "amostras": amostras, "ate_3": amostras, "fracao": 1.0 if amostras else None,
                       "maior_atraso": 2},
            "salto_errado": {"veredito": salto, "total": len(movimentos), "por_posicao": [],
                             "por_movimento": [{"t_ms": 1, "cursor": 1, "move": "Short", "intervalo": k,
                                                "esperado_no_inicio": 0, "classe": classe}
                                               for k, classe in movimentos]},
            "recuo": {"veredito": recuo, "violacoes": []},
            "recuperacao": {"veredito": recuperacao,
                            "pulos": [] if recuperacao == "NAO_SE_APLICA" else
                            [{"pulo_inicio_ms": 1, "primeiro_pulado": 47, "pulo_fim_ms": 2, "alvo": 91,
                              "recuperacao_ms": 900 if recuperacao == "PASSA" else 4000}]},
        },
        "amostras": {"total": 900, "leitura_normal": leitura, "excluidas_toque": 0, "excluidas_intervalo": 0,
                     "passo_ms": 100, "primeira_ms": 0, "ultima_ms": 90000},
        "janelas_toque": [],
        "cursor_final": {"cursor": cursor, "ultimo_token": 214},
    }


class Rodada:
    def __init__(self, raiz, nome_rodada="S1", sufixo="", config=()):
        self.nome_rodada = nome_rodada
        self.sufixo, self.config = sufixo, list(config)
        self.pastas = {k: Path(raiz) / k for k in ("relatorios", "trajetorias", "eventos", "esperado", "gravacoes")}
        for p in self.pastas.values():
            p.mkdir(parents=True, exist_ok=True)
        self.oficiais = []

    def gravacao(self, nome, variante="", eventos=(), folha=(), m=None, m200=None, p200=None, sinc_motivos=(),
                 meta=None, revisada=True, sem=(), deslocamentos=(D, D - 200, D + 200), trajetoria=TRAJETORIA,
                 folhas=None):
        """`folhas`: {corrida: lista de eventos} troca a folha de uma corrida vizinha (padrão: a de d deslocada)."""
        self.oficiais.append((nome, variante))
        oficial, nome = nome, nome + self.sufixo
        tp, ev = self.pastas["trajetorias"], self.pastas["eventos"]
        arquivos = {"trajetoria": tp / f"{nome}.trajectory.csv",
                    "esperado": self.pastas["esperado"] / f"{oficial}.expected.csv",
                    "hipoteses": tp / f"{nome}.hypotheses.csv"}
        for papel, caminho in arquivos.items():
            caminho.write_text(f"{papel} {nome}\n", encoding="utf-8")
        arquivos["trajetoria"].write_text(s.trajetoria_csv(trajetoria), encoding="utf-8")
        app = ev / f"{nome}.events.csv"
        app.write_text(s.eventos_csv(eventos), encoding="utf-8")
        entradas = [{"papel": p, "arquivo": c.name, "sha256": _sha(c)} for p, c in arquivos.items()]
        entradas.append({"papel": "eventos", "arquivo": app.name, "sha256": _sha(app)})
        por_corrida = {"d": list(entradas), "m200": list(entradas), "p200": list(entradas)}
        if folha:
            conteudos = {"d": folha, **{k: [(t + ms, e, x) for t, e, x in folha] for k, ms in VIZINHAS.items()},
                         **(folhas or {})}
            for corrida, lista in conteudos.items():
                f = ev / veredito.folha_da_corrida(nome, corrida)
                f.write_text(s.eventos_csv(lista), encoding="utf-8")
                por_corrida[corrida].append({"papel": "eventos", "arquivo": f.name, "sha256": _sha(f)})
        wav, level = self.pastas["gravacoes"] / f"{oficial}.wav", tp / f"{nome}.level.csv"
        wav.write_bytes(b"RIFF" + oficial.encode())
        level.write_text(f"level {nome}\n", encoding="utf-8")
        rel = self.pastas["relatorios"]
        base = m or metricas_json()
        for sufixo, dados, desl, corrida in ((".json", base, deslocamentos[0], "d"),
                                             (".m200.json", m200 or base, deslocamentos[1], "m200"),
                                             (".p200.json", p200 or base, deslocamentos[2], "p200")):
            j = copy.deepcopy(dados)
            j["deslocamento_ms"], j["entradas"] = desl, por_corrida[corrida]
            (rel / f"{nome}{sufixo}").write_text(json.dumps(j), encoding="utf-8")
        sinc = {"deslocamento_ms": D, "motivos": list(sinc_motivos),
                "entradas": [{"papel": "wav", "arquivo": wav.name, "sha256": _sha(wav)},
                             {"papel": "level", "arquivo": level.name, "sha256": _sha(level)}]}
        (rel / f"{nome}.sinc.json").write_text(json.dumps(sinc), encoding="utf-8")
        texto_id = nome.split("_")[0]
        valores = {**META_OFICIAL, "text_id": texto_id, **dict(c.split("=", 1) for c in self.config), **(meta or {})}
        (tp / f"{nome}.meta.txt").write_text("".join(f"{k}={v}\n" for k, v in valores.items()), encoding="utf-8")
        if revisada:
            (self.pastas["esperado"] / f"{oficial}.revisao.md").write_text(
                f"# Revisão {oficial}\n\n{veredito.REVISAO_CONCLUIDA}\n", encoding="utf-8")
        for sufixo in sem:
            (rel / f"{nome}{sufixo}").unlink()
        return self

    def completa(self, voz="giorgio", amb="sala", com_l30=True, **por_texto):
        """Todas as obrigatórias da rodada, passando, com os eventos exigidos; por_texto[ID] troca os kwargs."""
        padrao = {
            "T04": {"folha": [(1000, "improviso_inicio", ""), (2000, "improviso_fim", "")]},
            "T05": {"folha": [(1000, "pulo_inicio", "47"), (2000, "pulo_fim", "")],
                    "m": metricas_json(recuperacao="PASSA")},
            "T06": {"folha": [(1000, "silencio_inicio", ""), (31000, "silencio_fim", "")]},
            "T07": {"eventos": [(1000, "pausa_toque", ""), (11000, "retoma_toque", "")]},
            "L30": {"eventos": [(5000, "pausa_toque", ""), (25000, "retoma_toque", "")],
                    "folha": [(1000, "improviso_inicio", ""), (2000, "improviso_fim", ""),
                              (3000, "pulo_inicio", "1804"), (4000, "pulo_fim", ""),
                              (30000, "silencio_inicio", ""), (60000, "silencio_fim", "")],
                    "m": metricas_json(recuperacao="PASSA")},
        }
        for texto_id, variante in veredito.obrigatorios(self.nome_rodada) + ([("L30", "")] if com_l30 and
                                                                             self.nome_rodada == "S2" else []):
            if texto_id == "L30" and not com_l30:
                continue
            kwargs = {**padrao.get(texto_id, {}), **por_texto.get(texto_id, {})}
            take = f"t{variante}" if variante else "t1"
            self.gravacao(f"{texto_id}_{voz}_{amb}_{take}", variante, **kwargs)
        return self

    def tsv(self):
        caminho = self.pastas["relatorios"] / "oficiais.tsv"
        caminho.write_text("nome\tvariante\n" + "".join(f"{n}\t{v}\n" for n, v in self.oficiais), encoding="utf-8")
        return caminho

    def rodar(self):
        saida = self.pastas["relatorios"] / "rodada.json"
        argv = ["rodada", "--rodada", self.nome_rodada, "--oficiais", str(self.tsv()),
                "--relatorios", str(self.pastas["relatorios"]), "--trajetorias", str(self.pastas["trajetorias"]),
                "--eventos", str(self.pastas["eventos"]), "--esperado", str(self.pastas["esperado"]),
                "--gravacoes", str(self.pastas["gravacoes"]), "--json", str(saida)]
        if self.sufixo:
            argv += ["--sufixo", self.sufixo]
        for c in self.config:
            argv += ["--config", c]
        codigo, out, err = rodar(veredito.main, argv)
        dados = json.loads(saida.read_text(encoding="utf-8")) if saida.exists() else None
        return codigo, dados, out, err


def _estado(dados, nome, criterio):
    return next(g for g in dados["gravacoes"] if g["nome"] == nome)["criterios"][criterio]


class RodadaTest(unittest.TestCase):
    def test_D9_rodada_completa_passando(self):
        with Pasta() as p:
            codigo, dados, out, err = Rodada(p.dir).completa().rodar()
        self.assertEqual(0, codigo, err)
        self.assertEqual({c: "PASSOU" for c in veredito.CRITERIOS}, {c: v["estado"] for c, v in dados["criterios"].items()})
        self.assertEqual({"CS1": 3, "CS2": 8, "CS3": 2, "CS4": 3},
                         {c: v["gravacoes"] for c, v in dados["criterios"].items()})
        self.assertEqual([], dados["ausentes"])
        self.assertIn("| CS1 | PASSOU | 3 |", out)
        self.assertIn("Regras de contagem", out)

    def test_D9_ligacoes_de_cada_texto(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa().rodar()
        ligados = {g["texto"]: sorted(g["criterios"]) for g in dados["gravacoes"]}
        self.assertEqual(["CS1"], ligados["T01"])
        self.assertEqual(["CS2"], ligados["T02"])
        self.assertEqual(["CS2"], ligados["T03"])
        self.assertEqual(["CS2"], ligados["T04"])
        self.assertEqual(["CS3"], ligados["T05"])
        self.assertEqual(["CS4"], ligados["T06"])
        self.assertEqual(["CS4"], ligados["T07"])
        self.assertEqual(["CS2"], ligados["T08"])
        self.assertEqual(["CS2"], ligados["T09"])
        self.assertEqual(["CS1"], ligados["T10"])
        self.assertEqual(["CS1", "CS2", "CS3", "CS4"], ligados["L30"])

    def test_D9_salao_nao_liga_cs1(self):
        with Pasta() as p:
            codigo, dados, _, _ = Rodada(p.dir, "S3").completa(amb="salao").rodar()
        self.assertEqual(0, codigo)
        self.assertEqual("NÃO SE APLICA", dados["criterios"]["CS1"]["estado"])
        self.assertNotIn("CS1", next(g for g in dados["gravacoes"] if g["texto"] == "T01")["criterios"])

    def test_D9_uma_gravacao_reprovada_reprova_o_criterio(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T09={"m": metricas_json(salto="REPROVA", movimentos=())})
            codigo, dados, _, _ = r.rodar()
        self.assertEqual(1, codigo)
        self.assertEqual("REPROVOU", dados["criterios"]["CS2"]["estado"])
        self.assertEqual(["T09_giorgio_sala_t1: REPROVOU"], dados["criterios"]["CS2"]["motivos"])
        self.assertEqual("PASSOU", dados["criterios"]["CS1"]["estado"])

    def test_D9_recuo_reprova_cs2(self):
        with Pasta() as p:
            codigo, dados, _, _ = Rodada(p.dir).completa(T08={"m": metricas_json(recuo="REPROVA")}).rodar()
        self.assertEqual("REPROVOU", _estado(dados, "T08_giorgio_sala_t1", "CS2")["estado"])

    def test_D8_gravacao_obrigatoria_ausente_nao_mede(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            r.oficiais = [o for o in r.oficiais if not o[0].startswith("T10")]
            codigo, dados, _, _ = r.rodar()
        self.assertEqual(1, codigo)
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertIn("T10 (ausente): NÃO MEDIDO", dados["criterios"]["CS1"]["motivos"])
        self.assertEqual(["T10"], dados["ausentes"])

    def test_D8_variante_do_t02_ausente_nao_mede(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            r.oficiais = [o for o in r.oficiais if o != ("T02_giorgio_sala_t2", "2")]
            _, dados, _, _ = r.rodar()
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS2"]["estado"])
        self.assertEqual(["T02 variante 2"], dados["ausentes"])

    def test_D8_l30_opcional_na_s2(self):
        with Pasta() as p:
            codigo, dados, _, _ = Rodada(p.dir, "S2").completa(voz="voz2", com_l30=False).rodar()
        self.assertEqual(0, codigo)
        self.assertEqual({"CS1": 2, "CS2": 7, "CS3": 1, "CS4": 2},
                         {c: v["gravacoes"] for c, v in dados["criterios"].items()})
        with Pasta() as p:
            r = Rodada(p.dir, "S1").completa(com_l30=False)
            _, dados, _, _ = r.rodar()
        self.assertEqual(["L30"], dados["ausentes"])

    def test_C1_arquivo_que_falta_nao_mede(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T06={"sem": (".p200.json",)})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T06_giorgio_sala_t1", "CS4")
        self.assertEqual("NÃO MEDIDO", v["estado"])
        self.assertIn("T06_giorgio_sala_t1.p200.json", v["motivo"])


class SensibilidadeTest(unittest.TestCase):
    def test_D3_passa_em_d_e_reprova_em_d_mais_200_vira_sensivel(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"p200": metricas_json(atraso="REPROVA")})
            codigo, dados, _, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual("SENSÍVEL AO DESLOCAMENTO", v["estado"])
        self.assertIn("PASSOU, REPROVOU", v["motivo"])
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertEqual(1, codigo)

    def test_D3_passa_em_d_e_reprova_em_d_menos_200_vira_sensivel(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T01={"m200": metricas_json(atraso="REPROVA")}).rodar()
        self.assertEqual("SENSÍVEL AO DESLOCAMENTO", _estado(dados, "T01_giorgio_sala_t1", "CS1")["estado"])

    def test_D3_reprova_em_d_continua_reprovado_e_marca_sensivel(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"m": metricas_json(atraso="REPROVA"), "m200": metricas_json()})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual(("REPROVOU", True), (v["estado"], v["sensivel"]))
        self.assertEqual("REPROVOU", dados["criterios"]["CS1"]["estado"])

    def test_D3_deriva_ou_palma_da_sincronizacao_vira_sensivel(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T10={"sinc_motivos": ["deriva acima de 200 ms"]})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T10_giorgio_sala_t1", "CS1")
        self.assertEqual("SENSÍVEL AO DESLOCAMENTO", v["estado"])
        self.assertIn("deriva acima de 200 ms", v["motivo"])

    def test_D3_deslocamentos_inconsistentes_saem_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"deslocamentos": (D, D - 100, D + 200)})
            codigo, dados, out, err = r.rodar()
        self.assertEqual((2, None, ""), (codigo, dados, out))
        self.assertIn("T01_giorgio_sala_t1.m200.json com deslocamento 1270, esperado 1170", err)


class EventosTest(unittest.TestCase):
    def test_Q3_t06_sem_silencio_na_folha_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T06={"folha": ()}).rodar()
        v = _estado(dados, "T06_giorgio_sala_t1", "CS4")
        self.assertEqual(("NÃO MEDIDO", "sem silencio nos eventos"), (v["estado"], v["motivo"]))

    def test_Q3_l30_sem_pausa_nao_mede_cs4_mas_mede_cs2(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"eventos": ()}).rodar()
        self.assertEqual("NÃO MEDIDO", _estado(dados, "L30_giorgio_sala_t1", "CS4")["estado"])
        self.assertEqual("sem pausa nos eventos", _estado(dados, "L30_giorgio_sala_t1", "CS4")["motivo"])
        self.assertEqual("PASSOU", _estado(dados, "L30_giorgio_sala_t1", "CS2")["estado"])

    def test_Q3_l30_sem_silencio_ou_sem_improviso_nao_mede(self):
        folha = [(3000, "pulo_inicio", "1804"), (4000, "pulo_fim", ""),
                 (30000, "silencio_inicio", ""), (60000, "silencio_fim", "")]
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"folha": folha}).rodar()
        self.assertEqual("sem improviso nos eventos", _estado(dados, "L30_giorgio_sala_t1", "CS2")["motivo"])
        self.assertEqual("PASSOU", _estado(dados, "L30_giorgio_sala_t1", "CS4")["estado"])
        folha = [(1000, "improviso_inicio", ""), (2000, "improviso_fim", ""), (3000, "pulo_inicio", "1804"),
                 (4000, "pulo_fim", "")]
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"folha": folha}).rodar()
        self.assertEqual("sem silencio nos eventos", _estado(dados, "L30_giorgio_sala_t1", "CS4")["motivo"])

    def test_Q3_t04_sem_improviso_nao_mede_cs2(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T04={"folha": ()}).rodar()
        self.assertEqual("sem improviso nos eventos", _estado(dados, "T04_giorgio_sala_t1", "CS2")["motivo"])

    def test_eventos_desemparelhados_saem_com_2(self):
        with Pasta() as p:
            codigo, _, _, err = Rodada(p.dir).completa(T06={"folha": [(1000, "silencio_inicio", "")]}).rodar()
        self.assertEqual(2, codigo)
        self.assertIn("sem fim", err)

    def test_Q3_t07_sem_pausa_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T07={"eventos": ()}).rodar()
        self.assertEqual("sem pausa nos eventos", _estado(dados, "T07_giorgio_sala_t1", "CS4")["motivo"])

    def test_Q3_t05_sem_pulo_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T05={"folha": ()}).rodar()
        self.assertEqual("sem pulo nos eventos", _estado(dados, "T05_giorgio_sala_t1", "CS3")["motivo"])

    def test_D9_movimento_no_silencio_ou_na_pausa_reprova_cs4_e_no_improviso_nao(self):
        for intervalo, esperado in (("silencio", "REPROVOU"), ("pausa", "REPROVOU"), ("improviso", "PASSOU")):
            with self.subTest(intervalo=intervalo), Pasta() as p:
                m = metricas_json(salto="REPROVA", movimentos=[(intervalo, "alcance")])
                _, dados, _, _ = Rodada(p.dir).completa(T06={"m": m}).rodar()
                v = _estado(dados, "T06_giorgio_sala_t1", "CS4")
                self.assertEqual(esperado, v["estado"])
                self.assertEqual(1 if esperado == "REPROVOU" else 0, v["numeros"]["movimentos_por_classe"]["alcance"])

    def test_E3_classes_e_janela_de_toque_contadas(self):
        m = metricas_json(salto="REPROVA", movimentos=[("improviso", "alcance"), ("silencio", "além do esperado")])
        m["janelas_toque"] = [{"indice": 3, "inicio_ms": 0, "fim_ms": 900, "amostras_excluidas": 9}]
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"m": m}).rodar()
        n = _estado(dados, "L30_giorgio_sala_t1", "CS2")["numeros"]
        self.assertEqual({"alcance": 1, "além do esperado": 1}, n["movimentos_por_classe"])
        self.assertEqual(9, n["amostras_janela_toque"])
        self.assertEqual(round(150 / 214, 4), n["fracao_lida"])


class AmostrasTest(unittest.TestCase):
    def test_Q3_cs1_sem_amostras_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T01={"m": metricas_json(atraso="REPROVA", amostras=0)}).rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual(("NÃO MEDIDO", "sem amostras de leitura normal"), (v["estado"], v["motivo"]))

    def test_Q3_cs2_cursor_parado_no_zero_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T09={"m": metricas_json(cursor=0)}).rodar()
        self.assertEqual("o cursor não saiu do token 0", _estado(dados, "T09_giorgio_sala_t1", "CS2")["motivo"])

    def test_Q3_cs2_sem_leitura_normal_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T09={"m": metricas_json(leitura=0)}).rodar()
        self.assertEqual("sem amostras de leitura normal", _estado(dados, "T09_giorgio_sala_t1", "CS2")["motivo"])

    def test_cs3_reprova_com_pulo_lento(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T05={"m": metricas_json(recuperacao="REPROVA")}).rodar()
        v = _estado(dados, "T05_giorgio_sala_t1", "CS3")
        self.assertEqual("REPROVOU", v["estado"])
        self.assertEqual(4000, v["numeros"]["pulos"][0]["recuperacao_ms"])


class RevisaoEConfiguracaoTest(unittest.TestCase):
    def test_D7_sem_revisao_fica_pendente(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T09={"m": metricas_json(salto="REPROVA"), "revisada": False})
            codigo, dados, out, _ = r.rodar()
        v = _estado(dados, "T09_giorgio_sala_t1", "CS2")
        self.assertEqual("PENDENTE DE REVISÃO", v["estado"])
        self.assertIn("sem ela seria REPROVOU", v["motivo"])
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS2"]["estado"])
        self.assertEqual(["T09_giorgio_sala_t1 CS2"], dados["pendencias"])
        self.assertEqual(1, codigo)
        self.assertIn("Pendências de revisão: T09_giorgio_sala_t1 CS2", out)

    def test_D7_sensivel_sem_revisao_fica_pendente(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"p200": metricas_json(atraso="REPROVA"), "revisada": False})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual("PENDENTE DE REVISÃO", v["estado"])
        self.assertIn("sem ela seria SENSÍVEL AO DESLOCAMENTO", v["motivo"])

    def test_D7_revisao_sem_a_linha_exata_fica_pendente(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            (r.pastas["esperado"] / "T01_giorgio_sala_t1.revisao.md").write_text("Revisão concluída: não\n",
                                                                                   encoding="utf-8")
            _, dados, _, _ = r.rodar()
        self.assertEqual("PENDENTE DE REVISÃO", _estado(dados, "T01_giorgio_sala_t1", "CS1")["estado"])

    def test_D10_configuracao_diferente_nao_mede(self):
        for chave, valor in (("preset", "Agil"), ("vocabulary", "on"), ("recognizer_strategy", "Reset"),
                             ("level_threshold", "0.02"), ("animation_ms", "600"), ("audio_source", "wav")):
            with self.subTest(chave=chave), Pasta() as p:
                _, dados, _, _ = Rodada(p.dir).completa(T01={"meta": {chave: valor}}).rodar()
                v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
                self.assertEqual(("NÃO MEDIDO", False), (v["estado"], v["sensivel"]))
                self.assertIn(f"{chave}={valor}", v["motivo"])

    def test_C2_text_id_diferente_sai_com_2(self):
        with Pasta() as p:
            codigo, _, _, err = Rodada(p.dir).completa(T01={"meta": {"text_id": "T10"}}).rodar()
        self.assertEqual(2, codigo)
        self.assertIn("text_id=T10", err)


class ProvenienciaTest(unittest.TestCase):
    def test_Q8_arquivo_mudado_depois_do_relatorio_sai_com_2(self):
        for pasta, arquivo in (("esperado", "T01_giorgio_sala_t1.expected.csv"),
                               ("eventos", "T06_giorgio_sala_t1.folha.events.csv"),
                               ("gravacoes", "T01_giorgio_sala_t1.wav"),
                               ("trajetorias", "T01_giorgio_sala_t1.level.csv")):
            with self.subTest(arquivo=arquivo), Pasta() as p:
                r = Rodada(p.dir).completa()
                with open(r.pastas[pasta] / arquivo, "a", encoding="utf-8") as f:
                    f.write("x")
                codigo, _, _, err = r.rodar()
                self.assertEqual(2, codigo)
                self.assertIn(f"{arquivo} mudou ou sumiu", err)

    def test_Q8_relatorios_com_arquivos_diferentes_saem_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            caminho = r.pastas["relatorios"] / "T01_giorgio_sala_t1.p200.json"
            j = json.loads(caminho.read_text(encoding="utf-8"))
            j["entradas"][0]["sha256"] = "0" * 64
            caminho.write_text(json.dumps(j), encoding="utf-8")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("não usaram os mesmos arquivos", err)

    def test_Q8_relatorio_de_outra_gravacao_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            rel = r.pastas["relatorios"]
            for suf in (".json", ".m200.json", ".p200.json"):
                (rel / f"T10_giorgio_sala_t1{suf}").write_text((rel / f"T01_giorgio_sala_t1{suf}").read_text(
                    encoding="utf-8"), encoding="utf-8")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("T10_giorgio_sala_t1: o relatório usou T01_giorgio_sala_t1.trajectory.csv", err)


class OficiaisTest(unittest.TestCase):
    def test_lista_invalida_sai_com_2(self):
        casos = {
            "cabeçalho": "nome,variante\n",
            "voz de outra rodada": "nome\tvariante\nT01_voz2_sala_t1\t\n",
            "ambiente de outra rodada": "nome\tvariante\nT01_giorgio_salao_t1\t\n",
            "T02 sem variante": "nome\tvariante\nT02_giorgio_sala_t1\t\n",
            "variante fora do T02": "nome\tvariante\nT01_giorgio_sala_t1\t1\n",
            "variante 4": "nome\tvariante\nT02_giorgio_sala_t4\t4\n",
            "repetido": "nome\tvariante\nT01_giorgio_sala_t1\t\nT01_giorgio_sala_t2\t\n",
            "texto que não é de teste": "nome\tvariante\nX99_giorgio_sala_t1\t\n",
            "campos": "nome\tvariante\nT01_giorgio_sala_t1\n",
        }
        for nome, conteudo in casos.items():
            with self.subTest(caso=nome):
                with self.assertRaises(veredito.ErroEntrada):
                    veredito.ler_oficiais(conteudo, "S1")

    def test_take_refeito_vale_pela_lista(self):
        lista = veredito.ler_oficiais("nome\tvariante\nT01_giorgio_sala_t2\t\nT02_giorgio_sala_t4\t2\n", "S1")
        self.assertEqual([("T01_giorgio_sala_t2", "T01", ""), ("T02_giorgio_sala_t4", "T02", "2")], lista)

    def test_json_invalido_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            (r.pastas["relatorios"] / "T01_giorgio_sala_t1.sinc.json").write_text("{", encoding="utf-8")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("JSON inválido", err)

    def test_json_sem_campo_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            (r.pastas["relatorios"] / "T01_giorgio_sala_t1.sinc.json").write_text("{}", encoding="utf-8")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("JSON sem o campo esperado", err)


class ReproducaoTest(unittest.TestCase):
    CONFIG = ("preset=Agil", "audio_source=wav")

    def test_D11_reproducao_usa_os_arquivos_dela_e_o_esperado_da_oficial(self):
        with Pasta() as p:
            r = Rodada(p.dir, sufixo="_r_agil_off_novo_l0000", config=self.CONFIG).completa()
            codigo, dados, out, err = r.rodar()
        self.assertEqual(0, codigo, err)
        self.assertEqual(("T01_giorgio_sala_t1_r_agil_off_novo_l0000", "T01_giorgio_sala_t1"),
                         (dados["gravacoes"][0]["nome"], dados["gravacoes"][0]["oficial"]))
        self.assertEqual("Agil", dados["config"]["preset"])
        self.assertIn("reprodução _r_agil_off_novo_l0000", out)
        self.assertIn("preset=Agil", out)

    def test_D11_reproducao_com_meta_da_configuracao_oficial_nao_mede(self):
        with Pasta() as p:
            r = Rodada(p.dir, sufixo="_r_agil_off_novo_l0000", config=("audio_source=wav",))
            r.completa()
            r.config = list(self.CONFIG)
            _, dados, _, _ = r.rodar()
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertIn("preset=Normal", _estado(dados, "T01_giorgio_sala_t1_r_agil_off_novo_l0000", "CS1")["motivo"])

    def test_config_e_sufixo_invalidos_saem_com_2(self):
        for sufixo, config in (("_r-agil", ()), ("_R_AGIL", ()), ("_r_agil", ("cor=azul",)), ("_r_agil", ("preset",))):
            with self.subTest(sufixo=sufixo, config=config), Pasta() as p:
                r = Rodada(p.dir).completa()
                r.sufixo, r.config = sufixo, list(config)
                codigo, _, _, _ = r.rodar()
                self.assertEqual(2, codigo)

    def test_final_recusa_rodada_de_reproducao(self):
        with Pasta() as p:
            r = Rodada(p.dir, sufixo="_r_agil_off_novo_l0000", config=self.CONFIG).completa()
            r.rodar()
            codigo, _, err = rodar(veredito.main, ["final", str(r.pastas["relatorios"] / "rodada.json")])
        self.assertEqual(2, codigo)
        self.assertIn("reprodução", err)


class FinalTest(unittest.TestCase):
    def _rodadas(self, p, **kwargs):
        caminhos = []
        for nome, (voz, amb, com_l30) in (("S1", ("giorgio", "sala", True)), ("S2", ("voz2", "sala", False)),
                                          ("S3", ("giorgio", "salao", True))):
            if nome in kwargs.get("sem", ()):
                continue
            r = Rodada(p.dir / nome, nome).completa(voz=voz, amb=amb, com_l30=com_l30, **kwargs.get(nome, {}))
            r.rodar()
            caminhos.append(str(r.pastas["relatorios"] / "rodada.json"))
        saida = p.dir / "final.json"
        codigo, out, err = rodar(veredito.main, ["final", *caminhos, "--json", str(saida)])
        dados = json.loads(saida.read_text(encoding="utf-8")) if saida.exists() else None
        return codigo, dados, out, err

    def test_H1_tres_rodadas_passando(self):
        with Pasta() as p:
            codigo, dados, out, err = self._rodadas(p)
        self.assertEqual(0, codigo, err)
        self.assertEqual({c: "PASSOU" for c in veredito.CRITERIOS}, {c: v["estado"] for c, v in dados["criterios"].items()})
        self.assertEqual(["S1", "S2"], dados["criterios"]["CS1"]["rodadas"])
        self.assertEqual(["S1", "S2", "S3"], dados["criterios"]["CS4"]["rodadas"])
        self.assertEqual(3 + 2, dados["criterios"]["CS1"]["gravacoes"])
        self.assertIn("| CS1 | PASSOU | S1, S2 | 5 |", out)

    def test_D8_sem_segunda_voz_cs1_nao_mede_e_o_resto_passa(self):
        with Pasta() as p:
            codigo, dados, _, _ = self._rodadas(p, sem=("S2",))
        self.assertEqual(1, codigo)
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertIn("rodada S2 ausente", dados["criterios"]["CS1"]["motivos"])
        self.assertEqual("PASSOU", dados["criterios"]["CS2"]["estado"])

    def test_D8_sem_salao_cs2_a_cs4_nao_medem(self):
        with Pasta() as p:
            _, dados, _, _ = self._rodadas(p, sem=("S3",))
        self.assertEqual("PASSOU", dados["criterios"]["CS1"]["estado"])
        for c in ("CS2", "CS3", "CS4"):
            self.assertEqual("NÃO MEDIDO", dados["criterios"][c]["estado"])
            self.assertIn("rodada S3 ausente", dados["criterios"][c]["motivos"])

    def test_D9_reprovacao_numa_rodada_reprova_o_criterio(self):
        with Pasta() as p:
            _, dados, _, _ = self._rodadas(p, S3={"T03": {"m": metricas_json(salto="REPROVA")}})
        self.assertEqual("REPROVOU", dados["criterios"]["CS2"]["estado"])
        self.assertIn("S3: REPROVOU", dados["criterios"]["CS2"]["motivos"])

    def test_D9_reprovacao_vence_rodada_ausente(self):
        with Pasta() as p:
            _, dados, _, _ = self._rodadas(p, sem=("S3",), S1={"T03": {"m": metricas_json(salto="REPROVA")}})
        self.assertEqual("REPROVOU", dados["criterios"]["CS2"]["estado"])

    def test_D7_pendencia_numa_rodada_vira_nao_medido_no_final(self):
        with Pasta() as p:
            codigo, dados, _, _ = self._rodadas(p, S2={"T01": {"revisada": False}})
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertEqual(["T01_voz2_sala_t1 CS1"], dados["pendencias"])
        self.assertEqual(1, codigo)

    def test_rodada_repetida_ou_json_estranho_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir / "S1").completa()
            r.rodar()
            caminho = str(r.pastas["relatorios"] / "rodada.json")
            codigo, _, err = rodar(veredito.main, ["final", caminho, caminho])
            self.assertEqual(2, codigo)
            self.assertIn("repetida", err)
            codigo, _, _ = rodar(veredito.main, ["final", p.arquivo("x.json", "{}")])
            self.assertEqual(2, codigo)


class MetricasDeVerdadeTest(unittest.TestCase):
    def test_Q4_campos_lidos_existem_no_json_do_metricas(self):
        tempos = s.tempos_regulares(len(s.tokens("T01")))
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        jsons = {}
        for desl in (0, -200, 200):
            codigo, dados, _, err = s.rodar_metricas(metricas.main, s.trajetoria_csv(linhas),
                                                     s.esperado_csv("T01", tempos), [s.eventos_csv([])],
                                                     deslocamento=desl)
            self.assertIn(codigo, (0, 1), err)
            jsons[desl] = dados
        for c in veredito.CRITERIOS:
            with self.subTest(criterio=c):
                estado, _ = veredito.estado_base(c, jsons[0])
                self.assertIn(estado, ("PASSOU", "REPROVOU", "NÃO MEDIDO"))
                veredito.numeros(c, jsons[0], {"improviso": 0, "silencio": 0, "pausa": 0, "pulo": 0})
        self.assertEqual("PASSOU", veredito.estado_base("CS1", jsons[0])[0])
        self.assertEqual("PASSOU", veredito.estado_base("CS2", jsons[0])[0])
        self.assertEqual("NÃO MEDIDO", veredito.estado_base("CS3", jsons[0])[0])
        self.assertEqual(-200, jsons[-200]["deslocamento_ms"])
        self.assertEqual({"trajetoria", "esperado", "eventos"}, {e["papel"] for e in jsons[0]["entradas"]})


def _trocar_entradas(r, nome, sufixo, troca):
    """Reescreve as entradas de <nome><sufixo> (relatório do metricas.py) com troca(lista) -> lista."""
    caminho = r.pastas["relatorios"] / f"{nome}{sufixo}"
    j = json.loads(caminho.read_text(encoding="utf-8"))
    j["entradas"] = troca(j["entradas"])
    caminho.write_text(json.dumps(j), encoding="utf-8")


def _entrada(r, pasta, arquivo, papel="eventos"):
    return {"papel": papel, "arquivo": arquivo, "sha256": _sha(r.pastas[pasta] / arquivo)}


class FolhaPorCorridaTest(unittest.TestCase):
    """Achado 1 da revisão: a folha fica parada no relógio do WAV; cada corrida usa a sua (spec D3, E1)."""

    T06 = "T06_giorgio_sala_t1"

    def test_D3_corrida_vizinha_com_a_folha_de_d_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            _trocar_entradas(r, self.T06, ".m200.json", lambda es: [
                _entrada(r, "eventos", f"{self.T06}.folha.events.csv") if e["arquivo"].endswith(".folha.events.csv")
                else e for e in es])
            codigo, dados, _, err = r.rodar()
        self.assertEqual((2, None), (codigo, dados))
        self.assertIn(f"a corrida m200 usou {self.T06}.folha.events.csv como eventos", err)

    def test_D3_corrida_d_com_a_folha_vizinha_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            _trocar_entradas(r, self.T06, ".json", lambda es: [
                _entrada(r, "eventos", f"{self.T06}.p200.folha.events.csv") if e["arquivo"].endswith(".folha.events.csv")
                else e for e in es])
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn(f"a corrida d usou {self.T06}.p200.folha.events.csv como eventos", err)

    def test_D3_folha_vizinha_que_falta_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            _trocar_entradas(r, self.T06, ".p200.json",
                             lambda es: [e for e in es if not e["arquivo"].endswith(".folha.events.csv")])
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn(f"a corrida p200 não usou a folha dela ({self.T06}.p200.folha.events.csv)", err)

    def test_D3_folha_vizinha_igual_a_de_d_ou_com_sinal_trocado_sai_com_2(self):
        folha = [(1000, "silencio_inicio", ""), (31000, "silencio_fim", "")]
        mais = [(t + 200, e, x) for t, e, x in folha]
        menos = [(t - 200, e, x) for t, e, x in folha]
        for corrida, lista in (("m200", folha), ("p200", folha), ("m200", menos), ("p200", mais),
                               ("m200", [(1200, "silencio_inicio", ""), (31100, "silencio_fim", "")])):
            with self.subTest(corrida=corrida, lista=lista), Pasta() as p:
                codigo, _, _, err = Rodada(p.dir).completa(T06={"folha": folha, "folhas": {corrida: lista}}).rodar()
                self.assertEqual(2, codigo)
                self.assertIn(f"{self.T06}.{corrida}.folha.events.csv não é {self.T06}.folha.events.csv no relógio "
                              "do WAV", err)

    def test_D3_folha_vizinha_com_outro_evento_ou_detalhe_sai_com_2(self):
        folha = [(1000, "pulo_inicio", "47"), (2000, "pulo_fim", "")]
        for lista in ([(1200, "pulo_inicio", "48"), (2200, "pulo_fim", "")],
                      [(1200, "silencio_inicio", ""), (2200, "silencio_fim", "")]):
            with self.subTest(lista=lista), Pasta() as p:
                r = Rodada(p.dir).completa(T05={"folha": folha, "folhas": {"m200": lista},
                                                "m": metricas_json(recuperacao="PASSA")})
                codigo, _, _, err = r.rodar()
                self.assertEqual(2, codigo)
                self.assertIn("no relógio do WAV", err)

    def test_D3_folha_vizinha_mudada_depois_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            with open(r.pastas["eventos"] / f"{self.T06}.p200.folha.events.csv", "a", encoding="utf-8") as f:
                f.write("\n")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn(f"{self.T06}.p200.folha.events.csv mudou ou sumiu", err)

    def test_D3_folha_so_na_corrida_vizinha_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            nome = "T07_giorgio_sala_t1"
            f = r.pastas["eventos"] / f"{nome}.m200.folha.events.csv"
            f.write_text(s.eventos_csv([(1200, "silencio_inicio", ""), (2200, "silencio_fim", "")]), encoding="utf-8")
            _trocar_entradas(r, nome, ".m200.json", lambda es: es + [_entrada(r, "eventos", f.name)])
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn(f"a corrida d não usou a folha dela ({nome}.folha.events.csv)", err)

    def test_D3_folha_citada_duas_vezes_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            nome = self.T06
            _trocar_entradas(r, nome, ".m200.json",
                             lambda es: es + [e for e in es if e["arquivo"].endswith(".folha.events.csv")])
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn(f"a corrida m200 não usou a folha dela", err)

    def test_D3_sem_folha_nas_tres_corridas_mede(self):
        with Pasta() as p:
            codigo, dados, _, err = Rodada(p.dir).completa().rodar()
        self.assertEqual(0, codigo, err)
        self.assertEqual("PASSOU", _estado(dados, "T07_giorgio_sala_t1", "CS4")["estado"])

    def test_Q8_m200_com_outros_arquivos_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            _trocar_entradas(r, "T01_giorgio_sala_t1", ".m200.json",
                             lambda es: [{**es[0], "sha256": "0" * 64}] + es[1:])
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("não usaram os mesmos arquivos", err)

    def test_Q8_eventos_de_outra_gravacao_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            outra = _entrada(r, "eventos", "T10_giorgio_sala_t1.events.csv")
            for sufixo in (".json", ".m200.json", ".p200.json"):
                _trocar_entradas(r, "T01_giorgio_sala_t1", sufixo,
                                 lambda es: [outra if e["papel"] == "eventos" else e for e in es])
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("a corrida d usou T10_giorgio_sala_t1.events.csv como eventos", err)


class Cs4ComCursorTest(unittest.TestCase):
    """Achado 2 da revisão: CS4 sem nada para checar sai NÃO MEDIDO, nunca PASSOU (Q3)."""

    def test_Q3_t06_e_t07_com_o_motor_parado_nao_medem(self):
        parado = metricas_json(cursor=0, leitura=0, amostras=0)
        with Pasta() as p:
            codigo, dados, _, _ = Rodada(p.dir).completa(T06={"m": parado}, T07={"m": parado}).rodar()
        self.assertEqual(1, codigo)
        for nome in ("T06_giorgio_sala_t1", "T07_giorgio_sala_t1"):
            self.assertEqual(("NÃO MEDIDO", "sem amostras de leitura normal"),
                             (_estado(dados, nome, "CS4")["estado"], _estado(dados, nome, "CS4")["motivo"]))
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS4"]["estado"])

    def test_Q3_cs4_cursor_no_zero_nao_mede_e_no_um_mede(self):
        for cursor, estado in ((0, "NÃO MEDIDO"), (1, "PASSOU")):
            with self.subTest(cursor=cursor), Pasta() as p:
                _, dados, _, _ = Rodada(p.dir).completa(T07={"m": metricas_json(cursor=cursor)}).rodar()
                v = _estado(dados, "T07_giorgio_sala_t1", "CS4")
                self.assertEqual(estado, v["estado"])
                if cursor == 0:
                    self.assertEqual("o cursor não saiu do token 0", v["motivo"])

    def test_Q3_cs4_sem_leitura_normal_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T06={"m": metricas_json(leitura=0)}).rodar()
        self.assertEqual("sem amostras de leitura normal", _estado(dados, "T06_giorgio_sala_t1", "CS4")["motivo"])

    def test_E2_cs4_mostra_leitura_normal_e_fracao_lida(self):
        with Pasta() as p:
            _, dados, out, _ = Rodada(p.dir).completa(T06={"m": metricas_json(leitura=480)}).rodar()
        n = _estado(dados, "T06_giorgio_sala_t1", "CS4")["numeros"]
        self.assertEqual((480, round(150 / 214, 4)), (n["leitura_normal"], n["fracao_lida"]))
        self.assertIn('"leitura_normal": 480', out)

    def test_Q3_t07_cursor_parado_depois_da_pausa_nao_mede(self):
        traj = [(0, 0, "Reposition"), (500, 40, "Short")]  # pausa do T07 padrão: [1000, 11000) do app
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T07={"trajetoria": traj}).rodar()
        v = _estado(dados, "T07_giorgio_sala_t1", "CS4")
        self.assertEqual(("NÃO MEDIDO", f"o cursor não andou depois de pausa (fim em {11000 + D} ms do WAV)"),
                         (v["estado"], v["motivo"]))

    def test_Q3_movimento_no_fim_exato_do_intervalo_conta_e_um_ms_antes_nao(self):
        for t, estado in ((11000, "PASSOU"), (10999, "NÃO MEDIDO")):
            with self.subTest(t=t), Pasta() as p:
                traj = [(0, 0, "Reposition"), (500, 40, "Short"), (t, 41, "Short")]
                _, dados, _, _ = Rodada(p.dir).completa(T07={"trajetoria": traj}).rodar()
                self.assertEqual(estado, _estado(dados, "T07_giorgio_sala_t1", "CS4")["estado"])

    def test_Q3_reposition_ou_stay_depois_nao_contam_e_os_tres_automaticos_contam(self):
        for move, estado in (("Reposition", "NÃO MEDIDO"), ("Stay", "NÃO MEDIDO"), ("Short", "PASSOU"),
                             ("LongJump", "PASSOU"), ("Recovery", "PASSOU")):
            with self.subTest(move=move), Pasta() as p:
                traj = [(0, 0, "Reposition"), (500, 40, "Short"), (20000, 60, move)]
                _, dados, _, _ = Rodada(p.dir).completa(T07={"trajetoria": traj}).rodar()
                self.assertEqual(estado, _estado(dados, "T07_giorgio_sala_t1", "CS4")["estado"])

    def test_Q3_sem_nenhum_movimento_automatico_nao_mede(self):
        with Pasta() as p:
            codigo, dados, _, err = Rodada(p.dir).completa(T06={"trajetoria": [(0, 0, "Reposition")]}).rodar()
        self.assertNotEqual(2, codigo, err)
        self.assertEqual("NÃO MEDIDO", _estado(dados, "T06_giorgio_sala_t1", "CS4")["estado"])

    def test_Q3_cada_intervalo_exigido_conta_no_seu_criterio(self):
        traj = [(0, 0, "Reposition"), (40000, 150, "Short")]  # L30: pausa até 25.000, silêncio até 60.000
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"trajetoria": traj}, T04={"trajetoria": [
                (0, 0, "Reposition"), (500, 10, "Short")]}).rodar()
        v = _estado(dados, "L30_giorgio_sala_t1", "CS4")
        self.assertEqual("NÃO MEDIDO", v["estado"])
        self.assertIn("silencio (fim em", v["motivo"])
        self.assertNotIn("pausa", v["motivo"])
        self.assertEqual("PASSOU", _estado(dados, "L30_giorgio_sala_t1", "CS2")["estado"], "improviso termina em 2.000")
        self.assertIn("improviso (fim em", _estado(dados, "T04_giorgio_sala_t1", "CS2")["motivo"])
        self.assertEqual("PASSOU", _estado(dados, "L30_giorgio_sala_t1", "CS1")["estado"])

    def test_Q3_pulo_nao_entra_na_guarda_do_cs3(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T05={"trajetoria": [(0, 0, "Reposition"), (500, 10, "Short")],
                                                         "m": metricas_json(recuperacao="PASSA")}).rodar()
        self.assertEqual("PASSOU", _estado(dados, "T05_giorgio_sala_t1", "CS3")["estado"])

    def test_Q3_reprovou_continua_reprovado_com_o_motor_parado_depois(self):
        m = metricas_json(salto="REPROVA", movimentos=[("silencio", "alcance")])
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T06={"m": m, "trajetoria": [(0, 0, "Reposition")]}).rodar()
        self.assertEqual("REPROVOU", _estado(dados, "T06_giorgio_sala_t1", "CS4")["estado"])


class MotivoNoRelatorioTest(unittest.TestCase):
    """Achado 3 da revisão: a marca de sensível aparece no Markdown, também no REPROVOU e no PENDENTE."""

    def test_D3_reprovou_sensivel_aparece_no_markdown(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"m": metricas_json(atraso="REPROVA"), "m200": metricas_json()})
            _, dados, out, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual("REPROVOU", v["estado"])
        self.assertEqual("sensível ao deslocamento: com d−200 e d+200: PASSOU, REPROVOU", v["motivo"])
        self.assertIn("| REPROVOU |", out)
        self.assertIn("| sensível ao deslocamento: com d−200 e d+200: PASSOU, REPROVOU |", out)

    def test_D3_reprovou_com_motivo_da_sincronizacao(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"m": metricas_json(atraso="REPROVA"), "sinc_motivos": ["palma não confere"]})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual(("REPROVOU", True), (v["estado"], v["sensivel"]))
        self.assertIn("palma não confere", v["motivo"])

    def test_D3_passou_sem_sensibilidade_fica_sem_motivo(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa().rodar()
        self.assertEqual(("PASSOU", None, False), tuple(_estado(dados, "T01_giorgio_sala_t1", "CS1")[k]
                                                         for k in ("estado", "motivo", "sensivel")))

    def test_D7_pendente_guarda_o_motivo(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T10={"sinc_motivos": ["deriva acima de 200 ms"], "revisada": False})
            _, dados, out, _ = r.rodar()
        v = _estado(dados, "T10_giorgio_sala_t1", "CS1")
        self.assertEqual("PENDENTE DE REVISÃO", v["estado"])
        self.assertEqual("sem revisão concluída (D7); sem ela seria SENSÍVEL AO DESLOCAMENTO (sensível ao "
                         "deslocamento: deriva acima de 200 ms; com d−200 e d+200: PASSOU, PASSOU)", v["motivo"])
        self.assertIn("deriva acima de 200 ms", out)

    def test_D7_pendente_sem_motivo_fica_curto(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T09={"revisada": False}).rodar()
        self.assertEqual("sem revisão concluída (D7); sem ela seria PASSOU",
                         _estado(dados, "T09_giorgio_sala_t1", "CS2")["motivo"])


class LacunasOpr049Test(unittest.TestCase):
    """OPR-049 (revisão rodada 2 da 0005, achados 2 a 4): borda da guarda do motor com intervalo da folha, dois
    intervalos do mesmo tipo, corrida vizinha sem o events.csv do app e motivo do PENDENTE de um REPROVOU sensível."""

    def test_Q3_borda_da_guarda_com_o_silencio_da_folha(self):
        # T06: silêncio da folha em [1.000, 31.000) no relógio do WAV; a guarda lê a folha de d (nem m200 nem p200)
        for t, estado in ((31000, "PASSOU"), (30999, "NÃO MEDIDO")):
            with self.subTest(t=t), Pasta() as p:
                traj = [(0, 0, "Reposition"), (500, 40, "Short"), (t, 41, "Short")]
                _, dados, _, _ = Rodada(p.dir).completa(T06={"trajetoria": traj}).rodar()
                v = _estado(dados, "T06_giorgio_sala_t1", "CS4")
                self.assertEqual(estado, v["estado"])
                if estado == "NÃO MEDIDO":
                    self.assertEqual(f"o cursor não andou depois de silencio (fim em {31000 + D} ms do WAV)",
                                     v["motivo"])

    def test_Q3_dois_silencios_o_ultimo_movimento_entre_eles_nao_mede(self):
        folha = [(1000, "improviso_inicio", ""), (2000, "improviso_fim", ""), (3000, "pulo_inicio", "1804"),
                 (4000, "pulo_fim", ""), (26000, "silencio_inicio", ""), (27000, "silencio_fim", ""),
                 (30000, "silencio_inicio", ""), (60000, "silencio_fim", "")]
        for t, estado in ((28000, "NÃO MEDIDO"), (60000, "PASSOU")):
            with self.subTest(t=t), Pasta() as p:
                traj = [(0, 0, "Reposition"), (500, 40, "Short"), (t, 41, "Short")]
                _, dados, _, _ = Rodada(p.dir).completa(L30={"trajetoria": traj, "folha": folha}).rodar()
                v = _estado(dados, "L30_giorgio_sala_t1", "CS4")
                self.assertEqual(estado, v["estado"])
                if estado == "NÃO MEDIDO":
                    self.assertEqual(f"o cursor não andou depois de silencio (fim em {60000 + D} ms do WAV)",
                                     v["motivo"])

    def test_Q8_corrida_vizinha_sem_o_events_csv_do_app_sai_com_2(self):
        for texto, nome in (("T07", "T07_giorgio_sala_t1"), ("T06", "T06_giorgio_sala_t1")):
            for sufixo in (".m200.json", ".p200.json"):
                with self.subTest(nome=nome, sufixo=sufixo), Pasta() as p:
                    r = Rodada(p.dir).completa()
                    _trocar_entradas(r, nome, sufixo, lambda es: [
                        e for e in es if e["arquivo"] != f"{nome}.events.csv"])
                    codigo, dados, _, err = r.rodar()
                    self.assertEqual((2, None), (codigo, dados))
                    self.assertIn("mesmos arquivos", err)

    def test_D7_reprovou_sensivel_sem_revisao_fica_pendente_com_o_motivo(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"m": metricas_json(atraso="REPROVA"), "m200": metricas_json(),
                                            "revisada": False})
            _, dados, out, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual("PENDENTE DE REVISÃO", v["estado"])
        self.assertEqual("sem revisão concluída (D7); sem ela seria REPROVOU (sensível ao deslocamento: com d−200 e "
                         "d+200: PASSOU, REPROVOU)", v["motivo"])
        self.assertIn("sensível ao deslocamento", out)


class FinalProvenienciaTest(unittest.TestCase):
    """OPR-052: a final confere de novo cada arquivo lido pelas rodadas (Q8) e os casos feitos à mão."""

    def _tres(self, p):
        caminhos = []
        for nome, (voz, amb, com_l30) in (("S1", ("giorgio", "sala", True)), ("S2", ("voz2", "sala", False)),
                                          ("S3", ("giorgio", "salao", True))):
            r = Rodada(p.dir / nome, nome).completa(voz=voz, amb=amb, com_l30=com_l30)
            codigo, _, _, err = r.rodar()
            self.assertEqual(0, codigo, err)
            caminhos.append(r.pastas["relatorios"] / "rodada.json")
        return caminhos

    def _final(self, caminhos):
        return rodar(veredito.main, ["final", *map(str, caminhos)])

    def test_Q8_rodada_guarda_os_arquivos_lidos_com_caminho_relativo(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            _, dados, _, _ = r.rodar()
            lidos = _estado_g(dados, "T06_giorgio_sala_t1")["lidos"]
            esperado_sha = _sha(r.pastas["trajetorias"] / "T06_giorgio_sala_t1.meta.txt")
        for k in ("T06_giorgio_sala_t1.json", "T06_giorgio_sala_t1.m200.json", "T06_giorgio_sala_t1.p200.json",
                  "T06_giorgio_sala_t1.sinc.json", "../trajetorias/T06_giorgio_sala_t1.meta.txt",
                  "../trajetorias/T06_giorgio_sala_t1.trajectory.csv", "../esperado/T06_giorgio_sala_t1.revisao.md",
                  "../esperado/T06_giorgio_sala_t1.expected.csv", "../eventos/T06_giorgio_sala_t1.folha.events.csv",
                  "../eventos/T06_giorgio_sala_t1.m200.folha.events.csv",
                  "../eventos/T06_giorgio_sala_t1.p200.folha.events.csv", "../gravacoes/T06_giorgio_sala_t1.wav",
                  "../trajetorias/T06_giorgio_sala_t1.level.csv"):
            self.assertIn(k, lidos)
        self.assertEqual(esperado_sha, lidos["../trajetorias/T06_giorgio_sala_t1.meta.txt"])

    def test_Q8_arquivo_mudado_depois_da_rodada_faz_a_final_sair_com_2(self):
        for pasta, arquivo in (("esperado", "T01_giorgio_sala_t1.expected.csv"),
                               ("relatorios", "T01_giorgio_sala_t1.p200.json"),
                               ("trajetorias", "T07_giorgio_sala_t1.meta.txt"),
                               ("esperado", "T05_giorgio_sala_t1.revisao.md"),
                               ("eventos", "T06_giorgio_sala_t1.m200.folha.events.csv")):
            with self.subTest(arquivo=arquivo), Pasta() as p:
                caminhos = self._tres(p)
                with open(p.dir / "S3" / pasta / arquivo.replace("sala", "salao"), "a", encoding="utf-8") as f:
                    f.write("x")
                codigo, out, err = self._final(caminhos)
                self.assertEqual((2, ""), (codigo, out))
                self.assertIn(f"{arquivo.replace('sala', 'salao')} mudou ou sumiu depois da rodada S3", err)

    def test_Q8_arquivo_apagado_depois_da_rodada_faz_a_final_sair_com_2(self):
        with Pasta() as p:
            caminhos = self._tres(p)
            (p.dir / "S1" / "esperado" / "T09_giorgio_sala_t1.revisao.md").unlink()
            codigo, _, err = self._final(caminhos)
        self.assertEqual(2, codigo)
        self.assertIn("T09_giorgio_sala_t1.revisao.md mudou ou sumiu depois da rodada S1", err)

    def test_Q8_rodada_sem_lidos_sai_com_2(self):
        with Pasta() as p:
            caminhos = self._tres(p)
            j = json.loads(caminhos[0].read_text(encoding="utf-8"))
            del j["gravacoes"][0]["lidos"]
            caminhos[0].write_text(json.dumps(j), encoding="utf-8")
            codigo, _, err = self._final(caminhos)
        self.assertEqual(2, codigo)
        self.assertIn("JSON sem o campo esperado", err)

    def test_final_recusa_outra_configuracao_sem_sufixo(self):
        with Pasta() as p:
            caminhos = self._tres(p)
            j = json.loads(caminhos[1].read_text(encoding="utf-8"))
            j["config"]["preset"] = "Agil"
            caminhos[1].write_text(json.dumps(j), encoding="utf-8")
            codigo, _, err = self._final(caminhos)
        self.assertEqual(2, codigo)
        self.assertIn("a rodada S2 é de reprodução ou de outra configuração", err)

    def test_final_criterio_sem_nenhuma_rodada_aplicavel_nao_passa(self):
        with Pasta() as p:
            caminhos = self._tres(p)
            for c in caminhos:
                j = json.loads(c.read_text(encoding="utf-8"))
                j["criterios"]["CS3"]["estado"] = veredito.NAO_SE_APLICA
                c.write_text(json.dumps(j), encoding="utf-8")
            saida = p.dir / "final.json"
            codigo, _, _ = rodar(veredito.main, ["final", *map(str, caminhos), "--json", str(saida)])
            dados = json.loads(saida.read_text(encoding="utf-8"))
        self.assertEqual(1, codigo)
        self.assertEqual(("NÃO MEDIDO", []), (dados["criterios"]["CS3"]["estado"], dados["criterios"]["CS3"]["rodadas"]))

    def test_D8_ausente_so_conta_nos_criterios_ligados(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            r.oficiais = [o for o in r.oficiais if not o[0].startswith("T10")]
            _, dados, _, _ = r.rodar()
        self.assertEqual({"CS1": "NÃO MEDIDO", "CS2": "PASSOU", "CS3": "PASSOU", "CS4": "PASSOU"},
                         {c: v["estado"] for c, v in dados["criterios"].items()})


def _estado_g(dados, nome):
    return next(g for g in dados["gravacoes"] if g["nome"] == nome)


if __name__ == "__main__":
    unittest.main()
