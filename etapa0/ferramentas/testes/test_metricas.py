"""metricas.py com esperados, trajetórias e eventos sintéticos (spec 0004, E1 a E12; impl_plan A5 a A9).

Esperado base: tokens do T01, token i em [300i, 300i + 250] (sintetico.tempos_regulares). Os números do
veredito vêm do JSON de --json.
"""

import hashlib
import unittest

import metricas
from testes import sintetico as s

N = len(s.tokens("T01"))


def _base(sem_tempo=(), deslocamento=None):
    tempos = s.tempos_regulares(N, sem_tempo, deslocamento)
    return tempos, s.esperado_csv("T01", tempos)


def _medir(linhas, esperado, eventos=(), arquivos_eventos=None, **kw):
    arquivos = arquivos_eventos if arquivos_eventos is not None else [s.eventos_csv(eventos)]
    return s.rodar_metricas(metricas.main, s.trajetoria_csv(linhas), esperado, arquivos, **kw)


def _sem_proveniencia(dados):
    return {k: v for k, v in dados.items() if k not in ("entradas", "deslocamento_ms")}


class AtrasoTest(unittest.TestCase):
    def test_E1_dois_atras_o_tempo_todo_passa(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 2)
        codigo, dados, _out, _err = _medir(linhas, esperado)
        a = dados["criterios"]["atraso"]
        self.assertEqual(0, codigo)
        self.assertEqual("PASSA", a["veredito"])
        self.assertEqual(1.0, a["fracao"])
        self.assertEqual(2, a["maior_atraso"])
        self.assertGreaterEqual(a["amostras"], 600, "Q3")
        self.assertEqual("PASSA", dados["veredito"])

    def test_E1_atraso_3_ainda_conta_como_dentro(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 3)
        codigo, dados, _out, _err = _medir(linhas, esperado)
        a = dados["criterios"]["atraso"]
        self.assertEqual((1.0, 3, "PASSA"), (a["fracao"], a["maior_atraso"], a["veredito"]))
        self.assertEqual(0, codigo)

    def _com_janela(self, fim_janela, eventos=(), lag_no_intervalo=None):
        """600 amostras (tokens >= 200 sem tempo); atraso 4 em [30000, fim_janela); atraso 2 no resto."""
        tempos, esperado = _base(sem_tempo=range(200, N))
        intervalos = [(a, b) for (a, _), (b, _) in zip(eventos[::2], eventos[1::2])]

        def cursor(t, e):
            if lag_no_intervalo is not None and any(a <= t < b for a, b in intervalos):
                return e - lag_no_intervalo
            return e - (4 if 30000 <= t < fim_janela else 2)

        extras = [30000, fim_janela] + [t for par in intervalos for t in par]
        linhas = s.trajetoria_seguindo(tempos, cursor, extras=extras, move="Reposition")
        linhas[0] = (0, 0, "Reposition")
        evs = [(t, nome, "") for t, nome in eventos]
        return _medir(linhas, esperado, evs)

    def test_E2_atraso_4_em_5_por_cento_passa(self):
        codigo, dados, _out, _err = self._com_janela(33000)
        a = dados["criterios"]["atraso"]
        self.assertEqual(600, a["amostras"])
        self.assertEqual(570, a["ate_3"])
        self.assertEqual("PASSA", a["veredito"])
        self.assertEqual(0, codigo)

    def test_E2_atraso_4_em_6_por_cento_reprova(self):
        codigo, dados, _out, _err = self._com_janela(33600)
        a = dados["criterios"]["atraso"]
        self.assertEqual(564, a["ate_3"])
        self.assertEqual("REPROVA", a["veredito"])
        self.assertEqual(1, codigo)

    def test_E2_intervalos_de_excecao_nao_entram_na_conta(self):
        casos = {
            "improviso": [(40000, "improviso_inicio"), (43000, "improviso_fim")],
            "silencio": [(40000, "silencio_inicio"), (43000, "silencio_fim")],
            "pausa": [(40000, "pausa_toque"), (43000, "retoma_toque")],
        }
        for nome, eventos in casos.items():
            with self.subTest(intervalo=nome):
                _c, normal, _o, _e = self._com_janela(33000, eventos, lag_no_intervalo=2)
                _c, atrasado, _o, _e = self._com_janela(33000, eventos, lag_no_intervalo=50)
                self.assertEqual(570, normal["criterios"]["atraso"]["amostras"])
                self.assertEqual(normal["criterios"]["atraso"], atrasado["criterios"]["atraso"])

    def test_E2_tres_segundos_depois_do_pulo_nao_entram_na_conta(self):
        tempos, esperado = _base(sem_tempo=range(200, N))

        def medir(lag):
            def cursor(t, e):
                return e - (lag if 40000 <= t < 43000 else 2)

            linhas = s.trajetoria_seguindo(tempos, cursor, extras=[40000, 43000], move="Reposition")
            evs = [(39000, "pulo_inicio", "130"), (40000, "pulo_fim", "")]
            return _medir(linhas, esperado, evs)[1]["criterios"]["atraso"]

        self.assertEqual(570, medir(2)["amostras"])
        self.assertEqual(medir(2), medir(50))

    def test_atraso_sem_amostras_de_leitura_normal_reprova(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e)
        fim = 300 * (N - 1) + 250
        codigo, dados, _o, _e = _medir(linhas, esperado, [(0, "silencio_inicio", ""), (fim + 1, "silencio_fim", "")])
        a = dados["criterios"]["atraso"]
        self.assertEqual(0, a["amostras"])
        self.assertEqual("REPROVA", a["veredito"])
        self.assertEqual(1, codigo)


class SaltoErradoTest(unittest.TestCase):
    def _salto_em_30100(self, a_frente):
        tempos, esperado = _base()
        esperado_em = s.fim_em(tempos)
        alvo = esperado_em(30100) + a_frente  # esperado(30100) = 99

        def cursor(t, e):
            return max(e - 1, alvo) if t >= 30100 else e - 1

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[30100])
        return _medir(linhas, esperado)

    def test_E3_tres_a_frente_por_200_ms_conta(self):
        codigo, dados, _o, _e = self._salto_em_30100(3)
        sal = dados["criterios"]["salto_errado"]
        self.assertGreaterEqual(sal["total"], 1)
        self.assertEqual([30100, 30200, 2], [sal["por_posicao"][0][k] for k in ("inicio_ms", "fim_ms", "amostras")])
        self.assertEqual("REPROVA", sal["veredito"])
        self.assertEqual(1, codigo)

    def test_E3_dois_saltos_separados_contam_dois(self):
        tempos, esperado = _base()

        def cursor(t, e):
            if 30100 <= t < 30300 or 50100 <= t < 50300:
                return e + 3
            return e - 1

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[30100, 30300, 50100, 50300])
        _c, dados, _o, _e = _medir(linhas, esperado)
        sal = dados["criterios"]["salto_errado"]
        self.assertEqual(2, sal["total"])
        self.assertEqual([30100, 50100], [ep["inicio_ms"] for ep in sal["por_posicao"]])

    def test_E3_dois_a_frente_nao_conta(self):
        codigo, dados, _o, _e = self._salto_em_30100(2)
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
        self.assertEqual(0, codigo)

    def _movimento(self, inicio, fim, t_short):
        """Tokens >= 100 depois de um intervalo [30000, 40000) sem fala; Short para o esperado em t_short."""
        tempos, esperado = _base(deslocamento=lambda i: 10000 if i >= 100 else 0)

        def cursor(t, e):
            return e if t >= t_short and e == 99 else e - 1

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[t_short])
        self.assertIn((t_short, 99, "Short"), linhas)
        return _medir(linhas, esperado, [(30000, inicio, ""), (40000, fim, "")])

    def test_E4_movimento_automatico_no_intervalo_conta_como_alcance(self):
        for inicio, fim in (("silencio_inicio", "silencio_fim"), ("improviso_inicio", "improviso_fim"),
                            ("pausa_toque", "retoma_toque")):
            with self.subTest(intervalo=inicio):
                codigo, dados, _o, _e = self._movimento(inicio, fim, 35000)
                sal = dados["criterios"]["salto_errado"]
                self.assertEqual(1, sal["total"])
                self.assertEqual([], sal["por_posicao"])
                mov = sal["por_movimento"][0]
                self.assertEqual((35000, 99, "Short", "alcance", 99),
                                 (mov["t_ms"], mov["cursor"], mov["move"], mov["classe"], mov["esperado_no_inicio"]))
                self.assertEqual("REPROVA", sal["veredito"])
                self.assertEqual(1, codigo)

    def test_E4_um_ms_depois_do_fim_nao_conta(self):
        for inicio, fim in (("silencio_inicio", "silencio_fim"), ("improviso_inicio", "improviso_fim"),
                            ("pausa_toque", "retoma_toque")):
            with self.subTest(intervalo=inicio):
                codigo, dados, _o, _e = self._movimento(inicio, fim, 40001)
                self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
                self.assertEqual(0, codigo)

    def test_E4_bordas_do_intervalo(self):
        """Fechado no início e aberto no fim (D12): Short em 30000 conta; em 40000 não conta."""
        codigo, dados, _o, _e = self._movimento("silencio_inicio", "silencio_fim", 30000)
        self.assertEqual(1, dados["criterios"]["salto_errado"]["total"])
        self.assertEqual(1, codigo)
        codigo, dados, _o, _e = self._movimento("silencio_inicio", "silencio_fim", 40000)
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
        self.assertEqual(0, codigo)

    def test_E4_classe_usa_o_esperado_do_inicio_do_intervalo(self):
        """Tokens com fim dentro do improviso: a classe compara com esperado(início), não com esperado(t)."""
        tempos, esperado = _base()

        def cursor(t, e):
            return e - 1 if t < 30000 else (98 if t < 35000 else 101)

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[35000], ate=35000)
        _c, dados, _o, _e = _medir(linhas, esperado, [(30000, "improviso_inicio", ""), (40000, "improviso_fim", "")])
        mov = dados["criterios"]["salto_errado"]["por_movimento"]
        self.assertEqual([(35000, 101, 99, "além do esperado")],
                         [(m["t_ms"], m["cursor"], m["esperado_no_inicio"], m["classe"]) for m in mov])

    def test_E4_alem_do_esperado(self):
        tempos, esperado = _base(deslocamento=lambda i: 10000 if i >= 100 else 0)

        def cursor(t, e):
            return 101 if 35000 <= t < 40000 else (max(e - 1, 101) if t >= 40000 else e - 1)

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[35000, 40000])
        _c, dados, _o, _e = _medir(linhas, esperado, [(30000, "silencio_inicio", ""), (40000, "silencio_fim", "")])
        mov = dados["criterios"]["salto_errado"]["por_movimento"]
        self.assertEqual("além do esperado", mov[0]["classe"])


class ToqueTest(unittest.TestCase):
    """L30 min 22 e PRD: toque numa palavra 3 parágrafos à frente (spec D15; impl_plan A7)."""

    def _cenario(self, t_reposition=40000, t_toque=40000, short_na_janela=False):
        par = s.paragrafos("T01")
        i = par[3][0]  # 1º token do 4º parágrafo: 3 parágrafos à frente do 1º
        ultimo_lido = par[0][-1]
        retomada = 42000  # o orador começa a ler i 2 s depois do toque

        def deslocamento(k):
            return 0 if k <= ultimo_lido else retomada - 300 * i

        sem_tempo = [k for k in range(N) if ultimo_lido < k < i]
        tempos, esperado = _base(sem_tempo=sem_tempo, deslocamento=deslocamento)
        fim_i = tempos[i][1]
        linhas = [ln for ln in s.trajetoria_seguindo(tempos, lambda t, e: e - 1) if ln[0] < t_reposition]
        linhas.append((t_reposition, i, "Reposition"))
        if short_na_janela:
            linhas.append((t_toque + 500, i + 3, "Short"))
        seguintes = s.trajetoria_seguindo(tempos, lambda t, e: max(e - 1, i + 3 if short_na_janela else i))
        linhas += [ln for ln in seguintes if ln[0] > t_toque + 500 and ln[1] > linhas[-1][1]]
        eventos = [(t_toque, "toque_palavra", str(i))]
        return i, fim_i, _medir(linhas, esperado, eventos)

    def test_E5_toque_tres_paragrafos_a_frente_nao_conta_salto(self):
        i, fim_i, (codigo, dados, out, _e) = self._cenario()
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
        janela = dados["janelas_toque"][0]
        self.assertEqual((i, 40000, fim_i), (janela["indice"], janela["inicio_ms"], janela["fim_ms"]))
        self.assertEqual(len(range(40000, fim_i, 100)), janela["amostras_excluidas"])
        self.assertGreaterEqual(janela["amostras_excluidas"], 20, "Q3")
        self.assertEqual(janela["amostras_excluidas"], dados["amostras"]["excluidas_toque"])
        self.assertIn("janela de toque", out)
        self.assertEqual(0, codigo)

    def test_E5_short_na_janela_conta_na_primeira_amostra_depois(self):
        _i, fim_i, (codigo, dados, _o, _e) = self._cenario(short_na_janela=True)
        sal = dados["criterios"]["salto_errado"]
        self.assertEqual(1, sal["total"])
        primeira = -(-fim_i // 100) * 100
        self.assertEqual(primeira, sal["por_posicao"][0]["inicio_ms"])
        self.assertEqual(3, sal["por_posicao"][0]["maior_avanco"])
        self.assertEqual("REPROVA", sal["veredito"])
        self.assertEqual(1, codigo)

    def test_plano_A7_reposition_um_ms_antes_do_evento_fica_na_janela(self):
        _i, _f, (codigo, dados, _o, _e) = self._cenario(t_reposition=39999, t_toque=40001)
        self.assertEqual(39999, dados["janelas_toque"][0]["inicio_ms"])
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
        self.assertEqual(0, codigo)


    def test_plano_A7_so_a_reposition_para_i_antecipa_a_janela(self):
        _tempos, esperado = _base()
        for linhas in ([(0, 0, "Reposition")], [(0, 0, "Reposition"), (4000, 150, "Short")]):
            with self.subTest(ultima_linha=linhas[-1]):
                _c, dados, _o, _e = _medir(linhas, esperado, [(5000, "toque_palavra", "150")])
                self.assertEqual(5000, dados["janelas_toque"][0]["inicio_ms"])


class RecuperacaoTest(unittest.TestCase):
    """T05: o 2º parágrafo não é lido; pulo_fim no início da 1ª palavra do 3º (spec D16; impl_plan A5)."""

    def _cenario(self, recupera_em):
        par = s.paragrafos("T05")
        pulados = par[1]
        tempos, esperado = _base(sem_tempo=pulados, deslocamento=lambda k: -300 * len(pulados) if k > pulados[-1] else 0)
        primeiro_depois = pulados[-1] + 1
        pulo_fim = tempos[primeiro_depois][0]
        self.assertEqual(0, pulo_fim % 100, "pulo_fim cai numa amostra")
        ultimo_antes = pulados[0] - 1

        def cursor(t, e):
            if t < pulo_fim:
                return e - 1
            if recupera_em is None or t < pulo_fim + recupera_em:
                return ultimo_antes - 1
            return e - 1

        extras = [pulo_fim] + ([pulo_fim + recupera_em] if recupera_em is not None else [])
        linhas = s.trajetoria_seguindo(tempos, cursor, extras=extras, move="Recovery")
        eventos = [(pulo_fim - 100, "pulo_inicio", str(pulados[0])), (pulo_fim, "pulo_fim", "")]
        return _medir(linhas, esperado, eventos)

    def test_E6_recupera_em_2900_passa(self):
        _c, dados, _o, _e = self._cenario(2900)
        r = dados["criterios"]["recuperacao"]
        self.assertEqual(2900, r["pulos"][0]["recuperacao_ms"])
        self.assertEqual("PASSA", r["veredito"])

    def test_E6_recupera_em_3000_passa(self):
        _c, dados, _o, _e = self._cenario(3000)
        r = dados["criterios"]["recuperacao"]
        self.assertEqual(3000, r["pulos"][0]["recuperacao_ms"])
        self.assertEqual("PASSA", r["veredito"])

    def test_plano_A5_alvo_e_o_primeiro_token_com_tempo_a_partir_do_pulo_inicio(self):
        """Recuperação rápida: no 1º token depois do pulo. Com o alvo errado (índice seguinte), daria 600."""
        par = s.paragrafos("T05")
        pulados = par[1]
        tempos, esperado = _base(sem_tempo=pulados, deslocamento=lambda k: -300 * len(pulados) if k > pulados[-1] else 0)
        alvo = pulados[-1] + 1
        p = tempos[alvo][0]
        for detalhe, marca, recuperacao in ((pulados[0], p, 300), (alvo, p, 300), (pulados[0], p + 300, 0)):
            with self.subTest(detalhe=detalhe, pulo_fim=marca):
                linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1 if t < p or t >= p + 250 else pulados[0] - 2,
                                               extras=[p, p + 250])
                eventos = [(p - 100, "pulo_inicio", str(detalhe)), (marca, "pulo_fim", "")]
                _c, dados, _o, _e = _medir(linhas, esperado, eventos)
                self.assertEqual(alvo, dados["criterios"]["recuperacao"]["pulos"][0]["alvo"])
                self.assertEqual(recuperacao, dados["criterios"]["recuperacao"]["pulos"][0]["recuperacao_ms"])

    def test_E6_recupera_em_3100_reprova(self):
        codigo, dados, _o, _e = self._cenario(3100)
        r = dados["criterios"]["recuperacao"]
        self.assertEqual(3100, r["pulos"][0]["recuperacao_ms"])
        self.assertEqual("REPROVA", r["veredito"])
        self.assertEqual(1, codigo)

    def test_E6_nunca_recupera_reprova_mesmo_com_atraso_baixo_no_pulo_fim(self):
        """A5: no pulo_fim o esperado ainda não saltou; a leitura literal daria 0 ms e passaria (Q3)."""
        codigo, dados, _o, _e = self._cenario(None)
        r = dados["criterios"]["recuperacao"]
        self.assertIsNone(r["pulos"][0]["recuperacao_ms"])
        self.assertEqual("REPROVA", r["veredito"])
        self.assertEqual(1, codigo)

    def test_E6_sem_pulo_nao_se_aplica(self):
        tempos, esperado = _base()
        codigo, dados, out, _e = _medir(s.trajetoria_seguindo(tempos, lambda t, e: e - 1), esperado)
        self.assertEqual("NAO_SE_APLICA", dados["criterios"]["recuperacao"]["veredito"])
        self.assertEqual(0, codigo)
        self.assertIn("NAO_SE_APLICA", out)


class RecuoTest(unittest.TestCase):
    def _recuo(self, sequencia):
        _tempos, esperado = _base()
        linhas = [(0, 0, "Reposition")] + [(100 * (k + 1), c, m) for k, (c, m) in enumerate(sequencia)]
        return _medir(linhas, esperado)[1]["criterios"]["recuo"]

    def test_E7_recuo_para_36_depois_de_40_viola(self):
        subida = [(c, "Short") for c in range(5, 41, 5)]
        r = self._recuo(subida + [(36, "Short")])
        self.assertEqual("REPROVA", r["veredito"])
        self.assertEqual([{"t_ms": 900, "cursor": 36, "hw": 40, "move": "Short"}], r["violacoes"])

    def test_E7_hw_e_o_maior_nao_o_ultimo(self):
        subida = [(c, "Short") for c in range(5, 41, 5)]
        r = self._recuo(subida + [(38, "Short"), (36, "Short")])
        self.assertEqual([36], [v["cursor"] for v in r["violacoes"]])
        self.assertEqual([40], [v["hw"] for v in r["violacoes"]])

    def test_E7_recuo_para_37_nao_viola(self):
        subida = [(c, "Short") for c in range(5, 41, 5)]
        self.assertEqual("PASSA", self._recuo(subida + [(37, "Short")])["veredito"])

    def test_E7_reposition_redefine_hw(self):
        subida = [(c, "Short") for c in range(5, 41, 5)]
        self.assertEqual("PASSA", self._recuo(subida + [(10, "Reposition"), (12, "Short")])["veredito"])

    def test_E7_longjump_e_recovery_tambem_contam(self):
        for move in ("LongJump", "Recovery"):
            with self.subTest(move=move):
                r = self._recuo([(40, move), (36, move)])
                self.assertEqual(1, len(r["violacoes"]))


class EntradaTest(unittest.TestCase):
    def test_E8_deslocamento_leva_ao_relogio_do_esperado(self):
        par = s.paragrafos("T05")
        pulados = par[1]
        tempos, esperado = _base(sem_tempo=pulados, deslocamento=lambda k: -300 * len(pulados) if k > pulados[-1] else 0)
        pulo_fim = tempos[pulados[-1] + 1][0]
        linhas = s.trajetoria_seguindo(
            tempos, lambda t, e: pulados[0] - 2 if pulo_fim <= t < pulo_fim + 2000 else e - 1, extras=[pulo_fim + 2000]
        )
        eventos = [(5000, "improviso_inicio", ""), (6000, "improviso_fim", ""),
                   (pulo_fim - 100, "pulo_inicio", str(pulados[0])), (pulo_fim, "pulo_fim", "")]
        hip = ["t_ms,session,kind,words,accepted"] + [
            f'{f[1] + 100},1,partial,"{" ".join(t.formas[0])}",1' for t, f in zip(s.tokens("T05"), tempos) if f
        ]
        hip_menos = [hip[0]] + [f"{int(h.split(',')[0]) - 1500}," + h.split(",", 1)[1] for h in hip[1:]]
        _c, original, _o, _e = _medir(linhas, esperado, eventos, hipoteses="\n".join(hip) + "\n")
        menos = [(t - 1500, c, m) for t, c, m in linhas]
        eventos_menos = [(t - 1500, e, d) for t, e, d in eventos]
        _c, deslocado, _o, _e = _medir(menos, esperado, eventos_menos, deslocamento=1500,
                                       hipoteses="\n".join(hip_menos) + "\n")
        self.assertGreater(original["latencias"]["latencia_1"]["n"], 0, "Q3: as hipóteses entram")
        self.assertEqual(1500, deslocado["deslocamento_ms"])
        self.assertEqual(_sem_proveniencia(original), _sem_proveniencia(deslocado))
        self.assertEqual(2000, original["criterios"]["recuperacao"]["pulos"][0]["recuperacao_ms"], "Q3")

    def test_plano_A6_deslocamento_positivo_comeca_na_trajetoria(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        codigo, dados, _o, _e = _medir(linhas, esperado, deslocamento=1550)
        self.assertNotEqual(2, codigo)
        self.assertEqual(1600, dados["amostras"]["primeira_ms"])

    def test_E9_varios_arquivos_de_eventos(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        manual = [(10000, "improviso_inicio", ""), (12000, "improviso_fim", "")]
        app = [(20000, "pausa_toque", ""), (21000, "retoma_toque", "")]
        _c, um, _o, _e = _medir(linhas, esperado, manual + app)
        _c, dois, _o, _e = _medir(linhas, esperado, arquivos_eventos=[s.eventos_csv(manual), s.eventos_csv(app)])
        self.assertEqual(_sem_proveniencia(um), _sem_proveniencia(dois))
        self.assertEqual(30, um["amostras"]["excluidas_intervalo"], "Q3: os dois intervalos contam (20 + 10)")

    def test_E9_eventos_fora_de_ordem_sao_ordenados(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        ordenado = [(10000, "improviso_inicio", ""), (12000, "improviso_fim", "")]
        _c, a, _o, _e = _medir(linhas, esperado, ordenado)
        codigo, b, _o, _e = _medir(linhas, esperado, list(reversed(ordenado)))
        self.assertNotEqual(2, codigo)
        self.assertEqual(_sem_proveniencia(a), _sem_proveniencia(b))

    def test_E10_t_ms_igual_e_aceito(self):
        tempos, esperado = _base()
        linhas = [(0, 0, "Reposition"), (5000, 20, "Reposition"), (5000, 21, "Short")]
        codigo, _d, _o, err = _medir(linhas, esperado)
        self.assertNotEqual(2, codigo, err)

    def test_E10_entrada_invalida_sai_com_2(self):
        tempos, esperado = _base()
        traj = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
        ev = s.eventos_csv([])
        casos = {
            "cabeçalho da trajetória": (traj.replace("t_ms,cursor,move,score", "t,cursor,move,score"), esperado, [ev]),
            "cabeçalho do esperado": (traj, esperado.replace("token,palavra", "indice,palavra"), [ev]),
            "cabeçalho dos eventos": (traj, esperado, ["t_ms,event,detalhe\n"]),
            "move desconhecido": (traj.replace(",Short,", ",Jump,", 1), esperado, [ev]),
            "t_ms decrescente": (traj + "10,5,Short,0.0\n", esperado, [ev]),
            "não começa por Reposition": (traj.replace("0,0,Reposition", "0,0,Short", 1), esperado, [ev]),
            "evento fora da lista": (traj, esperado, [s.eventos_csv([(100, "aplauso", "")])]),
            "toque sem índice": (traj, esperado, [s.eventos_csv([(100, "toque_palavra", "")])]),
            "toque com índice não inteiro": (traj, esperado, [s.eventos_csv([(100, "toque_palavra", "x")])]),
            "improviso sem fim": (traj, esperado, [s.eventos_csv([(100, "improviso_inicio", "")])]),
            "fim sem início": (traj, esperado, [s.eventos_csv([(100, "silencio_fim", "")])]),
            "pulo_inicio sem índice": (traj, esperado, [s.eventos_csv([(100, "pulo_inicio", ""), (200, "pulo_fim", "")])]),
            "cursor além do roteiro": (traj + f"999999,{N},Short,0.0\n", esperado, [ev]),
            "toque além do roteiro": (traj, esperado, [s.eventos_csv([(100, "toque_palavra", str(N))])]),
            "esperado sem tempo": (traj, s.esperado_csv("T01", [None] * N), [ev]),
            "esperado fora de sequência": (traj, esperado.replace("\n1,", "\n2,", 1), [ev]),
        }
        for nome, (t, e, evs) in casos.items():
            with self.subTest(caso=nome):
                codigo, dados, out, err = s.rodar_metricas(metricas.main, t, e, evs)
                self.assertEqual(2, codigo)
                self.assertIsNone(dados)
                self.assertEqual("", out)
                self.assertIn("metricas.py:", err)

    def test_E10_hipoteses_com_cabecalho_errado(self):
        tempos, esperado = _base()
        traj = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
        codigo, _d, out, _e = s.rodar_metricas(metricas.main, traj, esperado, [s.eventos_csv([])],
                                               hipoteses="t_ms,kind,words\n")
        self.assertEqual(2, codigo)
        self.assertEqual("", out)


class LatenciasTest(unittest.TestCase):
    def _hipoteses(self, tempos, atraso_aceita, elegiveis):
        """Uma linha accepted=1 por token: 150 ms depois do fim nos elegíveis, 10 ms nos outros. Devem ser
        ignoradas: accepted=0 (fim + 50), kind=error (fim + 20) e a linha antes do t_inicio (início − 10)."""
        tokens = s.tokens("T01")
        linhas = ["t_ms,session,kind,words,accepted"]
        for t, tempo in zip(tokens, tempos):
            palavras = " ".join(t.formas[0])
            linhas.append(f'{tempo[1] + 50},1,partial,"{palavras}",0')
            linhas.append(f'{tempo[1] + 20},,error,"{palavras}",1')
            if t.indice in elegiveis:
                linhas.append(f'{tempo[0] - 10},1,partial,"{palavras}",1')  # antes do início: não vale
            atraso = atraso_aceita if t.indice in elegiveis else 10
            linhas.append(f'{tempo[1] + atraso},1,partial,"{palavras}",1')
        linhas.sort(key=lambda linha: int(linha.split(",")[0]) if linha[0].isdigit() else -1)
        return "\n".join(linhas) + "\n"

    def _elegiveis(self):
        donos = {}
        for t in s.tokens("T01"):
            for forma in t.formas:
                for p in forma:
                    donos.setdefault(p, set()).add(t.indice)
        return {t.indice for t in s.tokens("T01") if all(donos[p] == {t.indice} for f in t.formas for p in f)}

    def test_E11_fala_cursor_e_latencias_1_e_2(self):
        tempos, esperado = _base()
        fins = {tempo[1] + 400: i for i, tempo in enumerate(tempos)}
        linhas = [(0, 0, "Reposition")] + [(t, i, "Short") for t, i in sorted(fins.items()) if i > 0]
        elegiveis = self._elegiveis()
        self.assertTrue(10 <= len(elegiveis) < N - 50, "Q3: há elegíveis e há excluídos")
        for comum in ("de", "que", "e", "o", "a"):
            self.assertTrue(all(comum not in f for i in elegiveis for f in s.tokens("T01")[i].formas))
        codigo, dados, _o, _e = _medir(linhas, esperado, hipoteses=self._hipoteses(tempos, 150, elegiveis))
        lat = dados["latencias"]
        self.assertEqual({"n": N - 1, "mediana": 400, "p95": 400, "maximo": 400}, lat["fala_cursor"])
        self.assertEqual({"n": len(elegiveis - {0}), "mediana": 150, "p95": 150, "maximo": 150}, lat["latencia_1"])
        self.assertEqual({"n": len(elegiveis - {0}), "mediana": 250, "p95": 250, "maximo": 250}, lat["latencia_2"])
        self.assertEqual(500, lat["meta_provisoria_ms"])
        self.assertEqual(0, codigo)

    def test_E11_tokens_com_fim_em_intervalo_ficam_fora(self):
        tempos, esperado = _base()
        linhas = [(0, 0, "Reposition")] + [(tempo[1] + 400, i, "Short") for i, tempo in enumerate(tempos) if i > 0]
        eventos = [(30000, "improviso_inicio", ""), (33000, "improviso_fim", "")]
        _c, dados, _o, _e = _medir(linhas, esperado, eventos)
        self.assertEqual(N - 1 - 10, dados["latencias"]["fala_cursor"]["n"])  # fins 30050..32950: tokens 100..109

    def test_E11_latencia_acima_da_meta_nao_muda_o_codigo(self):
        tempos, esperado = _base()
        linhas = [(0, 0, "Reposition")] + [(tempo[1] + 600, i, "Short") for i, tempo in enumerate(tempos) if i > 0]
        codigo, dados, _o, _e = _medir(linhas, esperado)
        self.assertEqual(600, dados["latencias"]["fala_cursor"]["p95"])
        self.assertEqual(300 * (N - 1) + 250 + 600 - 50, dados["amostras"]["ultima_ms"], "vai até a última linha")
        self.assertIsNone(dados["latencias"]["latencia_1"])
        self.assertEqual(0, codigo)


class RelatorioTest(unittest.TestCase):
    def test_E12_stdout_traz_criterios_latencias_fim_e_proveniencia(self):
        tempos, esperado = _base()
        parado = s.fim_em(tempos)(10000) - 1  # o cursor fica parado no improviso

        def cursor(t, e):
            return parado if 10000 <= t < 12000 else e - 1

        traj = s.trajetoria_csv(s.trajetoria_seguindo(tempos, cursor, extras=[12000]))
        eventos = s.eventos_csv([(10000, "improviso_inicio", ""), (12000, "improviso_fim", "")])
        codigo, dados, out, _e = s.rodar_metricas(metricas.main, traj, esperado, [eventos], deslocamento=0)
        self.assertEqual(0, codigo)
        a = dados["criterios"]["atraso"]
        for trecho in ("Atraso: **PASSA**", f"{a['ate_3']}/{a['amostras']}", "Salto errado: **PASSA**",
                       "Recuperação de pulo: **NAO_SE_APLICA**", "Recuo: **PASSA**", "Fala -> cursor",
                       "Latência 1: sem --hipoteses", f"cursor final {N - 2}; último token {N - 1}",
                       "deslocamento: 0 ms", "Meta provisória"):
            self.assertIn(trecho, out)
        for nome, conteudo in (("x.trajectory.csv", traj), ("x.expected.csv", esperado), ("x0.events.csv", eventos)):
            self.assertIn(f"{nome} sha256={hashlib.sha256(conteudo.encode('utf-8')).hexdigest()}", out)
        self.assertEqual(["trajetoria", "esperado", "eventos"], [e["papel"] for e in dados["entradas"]])


class DefinicoesTest(unittest.TestCase):
    """esperado(t), cursor(t), amostras e p95 (D12, D18), direto na classe Medicao."""

    def _medicao(self, esperados, linhas):
        trajetoria = [metricas.Linha(t, c, m) for t, c, m in linhas]
        esp = [metricas.Esperado(i, a, b) for i, (a, b) in enumerate(esperados)]
        return metricas.Medicao(trajetoria, esp, [], None, None)

    def test_esperado_inclui_o_fim_exato_e_usa_o_maior_indice(self):
        m = self._medicao([(0, 2000), (500, 1000), (2000, 3000)], [(0, 0, "Reposition")])
        self.assertEqual(-1, m.esperado_em(999))
        self.assertEqual(1, m.esperado_em(1000))
        self.assertEqual(1, m.esperado_em(1999))
        self.assertEqual(1, m.esperado_em(2000))
        self.assertEqual(2, m.esperado_em(3000))

    def test_cursor_e_a_ultima_linha_ate_t_e_stay_e_ignorada(self):
        m = self._medicao([(0, 1000)], [(0, 0, "Reposition"), (500, 3, "Short"), (600, 7, "Stay")])
        self.assertEqual(0, m.cursor_em(499))
        self.assertEqual(3, m.cursor_em(500))
        self.assertEqual(3, m.cursor_em(700))

    def test_amostras_vao_ate_o_maior_fim_do_esperado(self):
        tempos, esperado = _base()
        linhas = [ln for ln in s.trajetoria_seguindo(tempos, lambda t, e: e - 1) if ln[0] <= 30000]
        _c, dados, _o, _e = _medir(linhas, esperado)
        self.assertEqual(64400, dados["amostras"]["ultima_ms"])
        self.assertEqual(0, dados["amostras"]["primeira_ms"])
        self.assertEqual(645, dados["amostras"]["total"])

    def test_percentil95_posto_mais_proximo(self):
        self.assertEqual(19, metricas._percentil95(list(range(1, 21))))
        self.assertEqual(10, metricas._percentil95(list(range(1, 11))))
        self.assertEqual(7, metricas._percentil95([7]))


if __name__ == "__main__":
    unittest.main()
