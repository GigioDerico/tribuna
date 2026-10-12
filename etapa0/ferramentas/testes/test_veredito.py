"""veredito.py com rodadas sintéticas em pasta temporária (spec 0005, D3, D7, D8, D9, D10; impl_plan 0005, A5).

O `Rodada` escreve, para cada gravação, arquivos de entrada com conteúdo único, os três JSON do metricas.py
(d, d − 200, d + 200) com a proveniência certa, o JSON do sincronizar.py, o meta.txt e a revisão. Um teste roda
o metricas.py de verdade para travar os campos que o veredito lê (Q4).
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
                 meta=None, revisada=True, sem=(), deslocamentos=(D, D - 200, D + 200)):
        self.oficiais.append((nome, variante))
        oficial, nome = nome, nome + self.sufixo
        tp, ev = self.pastas["trajetorias"], self.pastas["eventos"]
        arquivos = {"trajetoria": tp / f"{nome}.trajectory.csv",
                    "esperado": self.pastas["esperado"] / f"{oficial}.expected.csv",
                    "hipoteses": tp / f"{nome}.hypotheses.csv"}
        for papel, caminho in arquivos.items():
            caminho.write_text(f"{papel} {nome}\n", encoding="utf-8")
        app = ev / f"{nome}.events.csv"
        app.write_text(s.eventos_csv(eventos), encoding="utf-8")
        entradas = [{"papel": p, "arquivo": c.name, "sha256": _sha(c)} for p, c in arquivos.items()]
        entradas.append({"papel": "eventos", "arquivo": app.name, "sha256": _sha(app)})
        if folha:
            f = ev / f"{nome}.folha.events.csv"
            f.write_text(s.eventos_csv(folha), encoding="utf-8")
            entradas.append({"papel": "eventos", "arquivo": f.name, "sha256": _sha(f)})
        wav, level = self.pastas["gravacoes"] / f"{oficial}.wav", tp / f"{nome}.level.csv"
        wav.write_bytes(b"RIFF" + oficial.encode())
        level.write_text(f"level {nome}\n", encoding="utf-8")
        rel = self.pastas["relatorios"]
        base = m or metricas_json()
        for sufixo, dados, desl in ((".json", base, deslocamentos[0]), (".m200.json", m200 or base, deslocamentos[1]),
                                    (".p200.json", p200 or base, deslocamentos[2])):
            j = copy.deepcopy(dados)
            j["deslocamento_ms"], j["entradas"] = desl, entradas
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


if __name__ == "__main__":
    unittest.main()
