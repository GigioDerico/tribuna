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

    def test_rodada2_tres_segundos_depois_do_pulo_fecham_no_ms_exato(self):
        """Varredura (metricas.py:34, :263): com pulo_fim em 40001 a amostra 43000 ainda está nos 3 s (< 43001)."""
        tempos, esperado = _base(sem_tempo=range(200, N))

        def medir(lag):
            def cursor(t, e):
                return e - (lag if 40001 <= t < 43001 else 2)

            linhas = s.trajetoria_seguindo(tempos, cursor, extras=[40001, 43001], move="Reposition")
            evs = [(39000, "pulo_inicio", "130"), (40001, "pulo_fim", "")]
            return _medir(linhas, esperado, evs)[1]["criterios"]["atraso"]

        self.assertEqual(570, medir(2)["amostras"])
        self.assertEqual(medir(2), medir(50))

    def test_rodada2_relatorio_mostra_contagem_percentual_e_limites_do_atraso(self):
        """Varredura (metricas.py:449, :451): 570 de 600 é 95,00%; limites 'atraso <= 3' e 'mínimo 95%'."""
        _c, dados, out, _e = self._com_janela(33000)
        self.assertEqual(570, dados["criterios"]["atraso"]["ate_3"])
        self.assertIn("570/600 amostras de leitura normal com atraso <= 3 (95.00%; mínimo 95%)", out)

    def test_rodada2_sem_amostras_normais_o_relatorio_ainda_traz_total_e_ultima_amostra(self):
        """Varredura (metricas.py:293): o relatório descreve todas as amostras, mesmo sem nenhuma de leitura normal."""
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e)
        fim = 300 * (N - 1) + 250
        _c, dados, _o, _e = _medir(linhas, esperado, [(0, "silencio_inicio", ""), (fim + 1, "silencio_fim", "")])
        am = dados["amostras"]
        self.assertEqual((0, 645, 64400, 645), (am["leitura_normal"], am["total"], am["ultima_ms"], am["excluidas_intervalo"]))

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


    def test_rodada2_classe_um_a_frente_do_esperado_do_inicio_ja_e_alem(self):
        """X12: o limite da classe é cursor <= esperado no início ('alcance'); 1 a frente já é 'além do esperado'."""
        tempos, esperado = _base(deslocamento=lambda i: 10000 if i >= 100 else 0)

        def cursor(t, e):
            return max(e - 1, 100) if t >= 35000 else e - 1

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[35000])
        _c, dados, _o, _e = _medir(linhas, esperado, [(30000, "silencio_inicio", ""), (40000, "silencio_fim", "")])
        mov = dados["criterios"]["salto_errado"]["por_movimento"]
        self.assertEqual([(35000, 100, 99, "além do esperado")],
                         [(m["t_ms"], m["cursor"], m["esperado_no_inicio"], m["classe"]) for m in mov])

    def test_rodada2_cursor_adiantado_conta_como_dentro_do_atraso(self):
        """X20: atraso negativo (cursor à frente) é <= 3; quem pune é o salto errado, não o atraso."""
        codigo, dados, _o, _e = SaltoErradoTest()._salto_em_30100(10)
        a = dados["criterios"]["atraso"]
        self.assertEqual(a["amostras"], a["ate_3"])
        self.assertEqual("PASSA", a["veredito"])
        self.assertEqual("REPROVA", dados["criterios"]["salto_errado"]["veredito"])
        self.assertEqual(1, codigo)

    def test_rodada2_salto_por_posicao_vale_dentro_de_intervalos_sem_movimento_automatico(self):
        """Achado 2 do REVIEW: dentro de improviso, silêncio ou pausa o salto por posição também conta (Reposition
        sem toque_palavra não é movimento automático, então só a posição pega)."""
        for inicio, fim in (("improviso_inicio", "improviso_fim"), ("silencio_inicio", "silencio_fim"),
                            ("pausa_toque", "retoma_toque")):
            with self.subTest(intervalo=inicio):
                tempos, esperado = _base(deslocamento=lambda i: 10000 if i >= 100 else 0)  # sem fala no intervalo
                destino = s.fim_em(tempos)(31000) + 6
                linhas = s.trajetoria_seguindo(tempos, lambda t, e: max(e - 1, destino) if t >= 31000 else e - 1,
                                               extras=[31000])
                linhas = [(t, c, "Reposition" if t == 31000 else m) for t, c, m in linhas]
                codigo, dados, _o, _e = _medir(linhas, esperado, [(30000, inicio, ""), (40000, fim, "")])
                sal = dados["criterios"]["salto_errado"]
                self.assertEqual([], sal["por_movimento"])
                self.assertEqual(1, sal["total"])
                self.assertEqual((31000, 6), (sal["por_posicao"][0]["inicio_ms"], sal["por_posicao"][0]["maior_avanco"]))
                self.assertEqual(1, codigo)


class ToqueTest(unittest.TestCase):
    """L30 min 22 e PRD: toque numa palavra 3 parágrafos à frente (spec D15; impl_plan A7)."""

    def _cenario(self, t_reposition=40000, t_toque=40000, short_na_janela=False, sem_tempo_i=False,
                 eventos_extras=()):
        par = s.paragrafos("T01")
        i = par[3][0]  # 1º token do 4º parágrafo: 3 parágrafos à frente do 1º
        ultimo_lido = par[0][-1]
        retomada = 42000  # o orador começa a ler i 2 s depois do toque

        def deslocamento(k):
            return 0 if k <= ultimo_lido else retomada - 300 * i

        sem_tempo = [k for k in range(N) if ultimo_lido < k < i] + ([i] if sem_tempo_i else [])
        tempos, esperado = _base(sem_tempo=sem_tempo, deslocamento=deslocamento)
        fim_i = next(tempos[k][1] for k in range(i, N) if tempos[k] is not None)  # D15: 1º token com tempo >= i
        linhas = [ln for ln in s.trajetoria_seguindo(tempos, lambda t, e: e - 1) if ln[0] < t_reposition]
        linhas.append((t_reposition, i, "Reposition"))
        if short_na_janela:
            linhas.append((t_toque + 500, i + 3, "Short"))
        seguintes = s.trajetoria_seguindo(tempos, lambda t, e: max(e - 1, i + 3 if short_na_janela else i))
        linhas += [ln for ln in seguintes if ln[0] > t_toque + 500 and ln[1] > linhas[-1][1]]
        eventos = [(t_toque, "toque_palavra", str(i))] + list(eventos_extras)
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

    def test_rodada2_toque_em_token_sem_tempo_vai_ate_o_proximo_token_com_tempo(self):
        """OPR-036/037, achado 5 do REVIEW (D15): o token tocado não casou no alinhamento; a janela fecha no
        t_fim do primeiro token com tempo de índice >= i. Sem a guarda, `fim` seria None e o cálculo quebra."""
        i, fim_i, (codigo, dados, _o, err) = self._cenario(sem_tempo_i=True)
        janela = dados["janelas_toque"][0]
        self.assertEqual((i, 40000, fim_i), (janela["indice"], janela["inicio_ms"], janela["fim_ms"]))
        self.assertEqual(42550, fim_i, "fim do token i + 1: retomada em 42000, passo 300, duração 250")
        self.assertEqual(len(range(40000, fim_i, 100)), janela["amostras_excluidas"])
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"], err)
        self.assertEqual(0, codigo)

    def test_rodada2_reposition_dentro_de_pausa_improviso_ou_silencio_nao_e_movimento_automatico(self):
        """Achado 4 do REVIEW (spec: automático = Short, LongJump, Recovery): toque em palavra com a leitura
        pausada (ReaderController) é Reposition + toque_palavra dentro de pausa_toque..retoma_toque."""
        for inicio, fim in (("pausa_toque", "retoma_toque"), ("improviso_inicio", "improviso_fim"),
                            ("silencio_inicio", "silencio_fim")):
            with self.subTest(intervalo=inicio):
                _i, _f, (codigo, dados, _o, _e) = self._cenario(
                    t_reposition=31000, t_toque=31000, eventos_extras=[(30000, inicio, ""), (40000, fim, "")])
                sal = dados["criterios"]["salto_errado"]
                self.assertEqual(31000, dados["janelas_toque"][0]["inicio_ms"])
                self.assertEqual(([], []), (sal["por_movimento"], sal["por_posicao"]))
                self.assertEqual("PASSA", sal["veredito"])
                self.assertEqual(0, codigo)

    def test_rodada2_ultima_linha_ate_o_evento_manda_mesmo_com_t_igual(self):
        """X50: Reposition(i) em 4000, Short em 5000 e toque em 5000: a última linha com t <= evento é o Short,
        então a janela começa em 5000 (a mesma regra do cursor(t), D12)."""
        _tempos, esperado = _base()
        linhas = [(0, 0, "Reposition"), (4000, 150, "Reposition"), (5000, 151, "Short")]
        _c, dados, _o, _e = _medir(linhas, esperado, [(5000, "toque_palavra", "150")])
        self.assertEqual(5000, dados["janelas_toque"][0]["inicio_ms"])
        linhas = [(0, 0, "Reposition"), (4000, 150, "Reposition"), (5000, 151, "Short")]
        _c, dados, _o, _e = _medir(linhas, esperado, [(4000, "toque_palavra", "150")])
        self.assertEqual(4000, dados["janelas_toque"][0]["inicio_ms"])

    def test_rodada2_latencias_nao_contam_token_com_fim_dentro_da_janela_do_toque(self):
        """X80: o cursor chegou em 40 por Short em 9900 e o toque em 10000 leva ao 60; os tokens 33 a 40 têm
        fim em [10000, 18250), dentro da janela, e ficam fora do 'fala -> cursor'. Ficam 1 a 32."""
        _tempos, esperado = _base()
        linhas = [(0, 0, "Reposition"), (9900, 40, "Short"), (10000, 60, "Reposition")]
        _c, dados, _o, _e = _medir(linhas, esperado, [(10000, "toque_palavra", "60")])
        janela = dados["janelas_toque"][0]
        self.assertEqual((10000, 18250), (janela["inicio_ms"], janela["fim_ms"]))
        self.assertEqual(32, dados["latencias"]["fala_cursor"]["n"])


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

    def _pulos(self, regioes, passos, ajuste_ms=0):
        """Um ou mais pulos (OPR-037, achados 1 e 3 do REVIEW). `regioes`: faixas de tokens pulados (sem tempo; o
        orador segue sem lacuna). `passos[k]`: lista de (ms depois do pulo_fim, atraso) do cursor no pulo k; lista
        vazia = o cursor fica parado em pulados[0] - 2 até o fim. `ajuste_ms` move o evento pulo_fim
        (a folha manual não cai na amostra). Devolve (código, json)."""
        sem_tempo = {k for r in regioes for k in r}

        def deslocamento(k):
            return -300 * sum(len(r) for r in regioes if r[-1] < k)

        tempos, esperado = _base(sem_tempo=sem_tempo, deslocamento=deslocamento)
        fins = [tempos[r[-1] + 1][0] for r in regioes]
        for pulo_fim in fins:
            self.assertEqual(0, pulo_fim % 100, "pulo_fim cai numa amostra")

        def cursor(t, e):
            atraso = 1
            for r, pulo_fim, degraus in zip(regioes, fins, passos):
                if t < pulo_fim:
                    continue
                vigentes = [a for ms, a in degraus if ms <= t - pulo_fim]
                if not vigentes:
                    return r[0] - 2
                atraso = vigentes[-1]
            return e - atraso

        extras = [f + ms for f, degraus in zip(fins, passos) for ms in [0] + [d[0] for d in degraus]]
        linhas = s.trajetoria_seguindo(tempos, cursor, extras=extras, move="Recovery")
        eventos = []
        for r, pulo_fim in zip(regioes, fins):
            eventos += [(pulo_fim - 100, "pulo_inicio", str(r[0])), (pulo_fim + ajuste_ms, "pulo_fim", "")]
        codigo, dados, _o, _e = _medir(linhas, esperado, eventos)
        return codigo, dados["criterios"]["recuperacao"]

    def test_rodada2_atraso_exatamente_3_em_2900_passa(self):
        """Achado 1 do REVIEW (E6; X01 a X03): o cursor chega a esperado - 3 em pulo_fim + 2900."""
        _c, r = self._pulos([range(60, 72)], [[(2900, 3)]])
        self.assertEqual([2900], [p["recuperacao_ms"] for p in r["pulos"]])
        self.assertEqual("PASSA", r["veredito"])

    def test_rodada2_atraso_4_em_2900_e_3_em_3100_reprova_com_3100(self):
        codigo, r = self._pulos([range(60, 72)], [[(2900, 4), (3100, 3)]])
        self.assertEqual([3100], [p["recuperacao_ms"] for p in r["pulos"]])
        self.assertEqual("REPROVA", r["veredito"])

    def test_rodada2_atraso_4_em_2900_nunca_melhor_nunca_recupera(self):
        _c, r = self._pulos([range(60, 72)], [[(2900, 4)]])
        self.assertEqual([None], [p["recuperacao_ms"] for p in r["pulos"]])
        self.assertEqual("REPROVA", r["veredito"])

    def test_rodada2_dois_pulos_valem_todos_nao_qualquer(self):
        """Achado 3 do REVIEW (X06, all x any): vale o pior dos pulos."""
        regioes = [range(60, 72), range(130, 142)]
        casos = (
            ([[(2900, 1)], []], [2900, None], "REPROVA"),
            ([[(2900, 1)], [(3000, 1)]], [2900, 3000], "PASSA"),
            ([[(2900, 1)], [(3100, 1)]], [2900, 3100], "REPROVA"),
            ([[(3100, 1)], [(2900, 1)]], [3100, 2900], "REPROVA"),
        )
        for passos, esperado_ms, veredito in casos:
            with self.subTest(passos=passos):
                _c, r = self._pulos(regioes, passos)
                self.assertEqual(esperado_ms, [p["recuperacao_ms"] for p in r["pulos"]])
                self.assertEqual(veredito, r["veredito"])

    def test_rodada2_pulo_fim_marcado_depois_de_o_orador_retomar_da_zero_nao_negativo(self):
        """X08: com pulo_fim marcado 500 ms depois da fala do alvo, a recuperação conta a partir do pulo_fim."""
        par = s.paragrafos("T05")
        pulados = par[1]
        tempos, esperado = _base(sem_tempo=pulados, deslocamento=lambda k: -300 * len(pulados) if k > pulados[-1] else 0)
        p = tempos[pulados[-1] + 1][0]
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1 if t < p or t >= p + 250 else pulados[0] - 2,
                                       extras=[p, p + 250])
        eventos = [(p - 100, "pulo_inicio", str(pulados[0])), (p + 500, "pulo_fim", "")]
        _c, dados, _o, _e = _medir(linhas, esperado, eventos)
        self.assertEqual(0, dados["criterios"]["recuperacao"]["pulos"][0]["recuperacao_ms"])

    def test_rodada2_salto_errado_por_posicao_vale_dentro_dos_3_s_depois_do_pulo(self):
        """Achado 2 do REVIEW (X10, TESTES-ETAPA0 'em toda a trajetória'): o Recovery cai 6 tokens à frente 1 s depois
        do pulo_fim e o orador alcança em 1 s, tudo antes de pulo_fim + 3000. A recuperação PASSA; o salto REPROVA."""
        pulados = range(60, 72)
        tempos, esperado = _base(sem_tempo=pulados, deslocamento=lambda k: -300 * len(pulados) if k > pulados[-1] else 0)
        pulo_fim = tempos[72][0]
        t0 = pulo_fim + 1000
        destino = s.fim_em(tempos)(t0) + 6

        def cursor(t, e):
            if t < pulo_fim:
                return e - 1
            return pulados[0] - 2 if t < t0 else max(e - 1, destino)

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[pulo_fim, t0], move="Recovery")
        eventos = [(pulo_fim - 100, "pulo_inicio", str(pulados[0])), (pulo_fim, "pulo_fim", "")]
        codigo, dados, _o, _e = _medir(linhas, esperado, eventos)
        sal = dados["criterios"]["salto_errado"]
        self.assertEqual(1, sal["total"])
        ep = sal["por_posicao"][0]
        self.assertEqual((t0, pulo_fim + 2000, 6), (ep["inicio_ms"], ep["fim_ms"], ep["maior_avanco"]))
        self.assertLess(ep["fim_ms"], pulo_fim + 3000)
        self.assertEqual(1000, dados["criterios"]["recuperacao"]["pulos"][0]["recuperacao_ms"])
        self.assertEqual("PASSA", dados["criterios"]["recuperacao"]["veredito"])
        self.assertEqual("REPROVA", dados["veredito"])
        self.assertEqual(1, codigo)

    def test_rodada2_limite_de_3000_ms_vale_para_o_ms_exato(self):
        """Varredura (metricas.py:35, :375): com o pulo_fim marcado 1 ms antes da amostra, recuperar na amostra
        seguinte dá 2901 ms (passa) ou 3001 ms (reprova); 1 ms depois, 2999 ms (passa)."""
        for ajuste, offset, ms, veredito in ((-1, 2900, 2901, "PASSA"), (-1, 3000, 3001, "REPROVA"),
                                             (1, 3000, 2999, "PASSA"), (1, 3100, 3099, "REPROVA")):
            with self.subTest(ajuste=ajuste, offset=offset):
                _c, r = self._pulos([range(60, 72)], [[(offset, 1)]], ajuste_ms=ajuste)
                self.assertEqual([ms], [p["recuperacao_ms"] for p in r["pulos"]])
                self.assertEqual(veredito, r["veredito"])

    def test_rodada2_relatorio_da_recuperacao_traz_o_maximo(self):
        _c, _d, out, _e = self._cenario(2900)
        self.assertIn("pulo_fim em ", out)
        self.assertIn(": 2900 ms; máximo 3000 ms", out)

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


    def test_rodada2_linhas_stay_nao_repetem_a_violacao(self):
        """X40: depois de um recuo para 36 as linhas Stay (mesmo cursor) não geram novas violações."""
        subida = [(c, "Short") for c in range(5, 41, 5)]
        r = self._recuo(subida + [(36, "Short"), (36, "Stay"), (36, "Stay")])
        self.assertEqual([36], [v["cursor"] for v in r["violacoes"]])


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

    def test_rodada2_totais_do_relatorio_fecham_com_toque_intervalos_e_pulo(self):
        """Varredura (metricas.py:291-301): total = leitura normal + excluídas por toque + excluídas por intervalo,
        com cada exclusão contada à mão. Esperado deslocado 50 ms: o fim do token cai num múltiplo de 100."""
        tempos, esperado = _base(deslocamento=lambda i: 50)  # token i em [300i + 50, 300i + 300]
        base = s.trajetoria_seguindo(tempos, lambda t, e: e - 1, extras=[50000])
        linhas = [ln for ln in base if ln[0] < 50000] + [(50000, 190, "Reposition")]
        depois = s.trajetoria_seguindo(tempos, lambda t, e: max(e - 1, 190))
        linhas += [ln for ln in depois if ln[0] > 50000 and ln[1] > 190]
        eventos = [(10000, "improviso_inicio", ""), (12000, "improviso_fim", ""),  # 20 amostras
                   (20000, "silencio_inicio", ""), (21000, "silencio_fim", ""),  # 10
                   (25000, "pausa_toque", ""), (26000, "retoma_toque", ""),  # 10
                   (29900, "pulo_inicio", "101"), (30000, "pulo_fim", ""),  # 3 s depois: 30
                   (50000, "toque_palavra", "190")]  # janela [50000, 57300): 73
        _c, dados, out, _e = _medir(linhas, esperado, eventos)
        am = dados["amostras"]
        self.assertEqual((100, 0, 64500, 646), (am["passo_ms"], am["primeira_ms"], am["ultima_ms"], am["total"]))
        self.assertEqual((70, 73, 503), (am["excluidas_intervalo"], am["excluidas_toque"], am["leitura_normal"]))
        self.assertEqual(am["total"], am["leitura_normal"] + am["excluidas_toque"] + am["excluidas_intervalo"])
        self.assertEqual(503, dados["criterios"]["atraso"]["amostras"])
        janela = dados["janelas_toque"][0]
        self.assertEqual((50000, 57300, 73), (janela["inicio_ms"], janela["fim_ms"], janela["amostras_excluidas"]))
        self.assertIn("646 amostras a cada 100 ms, de 0 a 64500 ms", out)
        self.assertIn("leitura normal: 503; excluídas por intervalo: 70; excluídas por toque: 73", out)

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


    def _rejeita(self, trajetoria=None, esperado=None, eventos=None, hipoteses=None):
        tempos, esp_ok = _base()
        traj_ok = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
        codigo, dados, out, err = s.rodar_metricas(
            metricas.main, trajetoria if trajetoria is not None else traj_ok,
            esperado if esperado is not None else esp_ok,
            eventos if eventos is not None else [s.eventos_csv([])], hipoteses=hipoteses)
        self.assertEqual(2, codigo, err)
        self.assertIsNone(dados)
        self.assertEqual("", out)
        self.assertIn("metricas.py:", err)

    def test_rodada2_empate_de_t_ms_entre_arquivos_vale_o_arquivo_e_dentro_do_arquivo_a_linha(self):
        """Varredura (metricas.py:158, D11): o fim do improviso do arquivo 0 vem antes do início do arquivo 1, mesmo
        com o início na linha 1 e o fim na linha 2; no mesmo arquivo, vale a ordem das linhas, não a do nome."""
        a = s.eventos_csv([(10000, "improviso_inicio", ""), (12000, "improviso_fim", "")])
        b = s.eventos_csv([(12000, "improviso_inicio", ""), (13000, "improviso_fim", "")])
        eventos = metricas.ler_eventos([a, b], 0, 10)
        self.assertEqual([(10000, "improviso_inicio"), (12000, "improviso_fim"), (12000, "improviso_inicio"),
                          (13000, "improviso_fim")], [(e.t, e.nome) for e in eventos])
        c = s.eventos_csv([(10000, "pausa_toque", ""), (12000, "retoma_toque", ""), (12000, "pausa_toque", ""),
                           (13000, "retoma_toque", "")])
        eventos = metricas.ler_eventos([c], 0, 10)
        self.assertEqual(["pausa_toque", "retoma_toque", "pausa_toque", "retoma_toque"], [e.nome for e in eventos])
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        codigo, dados, _o, err = _medir(linhas, esperado, arquivos_eventos=[a, b])
        self.assertNotEqual(2, codigo, err)  # 1 = REPROVA (o cursor segue a fala dentro do improviso); 2 = entrada inválida
        self.assertEqual(30, dados["amostras"]["excluidas_intervalo"])

    def test_rodada2_indice_zero_e_o_ultimo_token_sao_validos_nos_eventos(self):
        """Varredura (metricas.py:155): o primeiro token (0) e o último (n - 1) existem; n não."""
        ok = s.eventos_csv([(100, "toque_palavra", "0"), (200, "pulo_inicio", "0"), (300, "pulo_fim", ""),
                            (400, "toque_palavra", str(N - 1))])
        eventos = metricas.ler_eventos([ok], 0, N)
        self.assertEqual(["toque_palavra", "pulo_inicio", "pulo_fim", "toque_palavra"], [e.nome for e in eventos])
        with self.assertRaises(metricas.ErroEntrada):
            metricas.ler_eventos([s.eventos_csv([(100, "toque_palavra", str(N))])], 0, N)

    def test_rodada2_mensagens_apontam_a_linha_de_dados_certa(self):
        """Varredura (metricas.py:95, :110, :150, :164): a primeira linha depois do cabeçalho é a linha 1."""
        cab_h = "t_ms,session,kind,words,accepted\n"
        casos = (
            (lambda: metricas.ler_trajetoria("t_ms,cursor,move,score\n0,0,Reposition,0.0\n5,1,Jump,0.0\n", 0),
             "trajetória: linha 2 com move fora de"),
            (lambda: metricas.ler_eventos(["t_ms,evento,detalhe\n1,improviso_inicio,\n2,aplauso,\n"], 0, 5),
             "eventos: linha 2 com evento fora da lista"),
            (lambda: metricas.ler_hipoteses(cab_h + "1,1,partial,x,1\n2,1,lixo,x,1\n", 0),
             "hipóteses: linha 2 com kind ou accepted inválido"),
            (lambda: metricas.ler_hipoteses(cab_h + "1,1,partial,x,1\n2,1,final,x,1\n3,1\n", 0),
             "hipóteses: linha 3 com 2 campos"),
        )
        for chamada, trecho in casos:
            with self.subTest(trecho=trecho):
                with self.assertRaises(metricas.ErroEntrada) as cm:
                    chamada()
                self.assertEqual(trecho, str(cm.exception)[:len(trecho)])

    def test_rodada2_ler_esperado_guarda_o_indice_e_aceita_duracao_zero(self):
        """Varredura (metricas.py:136, :139): token sem tempo mantém o índice; início = fim é um tempo válido."""
        saida, palavras = metricas.ler_esperado("token,palavra,t_inicio_ms,t_fim_ms\n0,a,10,20\n1,b,,\n2,c,30,30\n")
        self.assertEqual([metricas.Esperado(0, 10, 20), metricas.Esperado(1, None, None), metricas.Esperado(2, 30, 30)],
                         saida)
        self.assertEqual(["a", "b", "c"], palavras)

    def test_rodada2_validacoes_da_trajetoria(self):
        """OPR-036 (X61, X62): cursor negativo e score que não é número saem com 2."""
        cab = "t_ms,cursor,move,score\n"
        self._rejeita(trajetoria=cab + "0,0,Reposition,0.0\n100,-1,Short,0.0\n")
        self._rejeita(trajetoria=cab + "0,0,Reposition,abc\n")

    def test_rodada2_validacoes_do_esperado(self):
        """OPR-036 (X63, X64): fim antes do início e tempo pela metade saem com 2."""
        _t, esperado = _base()
        primeira = "0,ouvir,0,250"
        self.assertIn(primeira, esperado)
        for nome, linha in (("fim antes do início", "0,ouvir,250,0"), ("só o fim", "0,ouvir,,250"),
                            ("só o início", "0,ouvir,0,")):
            with self.subTest(caso=nome):
                self._rejeita(esperado=esperado.replace(primeira, linha, 1))

    def test_rodada2_validacoes_dos_eventos(self):
        """OPR-036 (X60): índice negativo em toque_palavra e pulo_inicio sai com 2."""
        self._rejeita(eventos=[s.eventos_csv([(100, "toque_palavra", "-1")])])
        self._rejeita(eventos=[s.eventos_csv([(100, "pulo_inicio", "-1"), (200, "pulo_fim", "")])])

    def test_rodada2_validacoes_das_hipoteses(self):
        """OPR-036 (X65): kind ou accepted fora dos valores do app saem com 2."""
        cab = "t_ms,session,kind,words,accepted\n"
        self._rejeita(hipoteses=cab + '0,1,lixo,"ouvir",1\n')
        self._rejeita(hipoteses=cab + '0,1,partial,"ouvir",2\n')

    def test_rodada2_campos_a_mais_ou_a_menos_saem_com_2(self):
        """OPR-036 (X69): linha com número de campos diferente do cabeçalho, em cada um dos quatro arquivos."""
        _t, esperado = _base()
        cab_h = "t_ms,session,kind,words,accepted\n"
        casos = {
            "trajetória a mais": {"trajetoria": "t_ms,cursor,move,score\n0,0,Reposition,0.0,x\n"},
            "trajetória a menos": {"trajetoria": "t_ms,cursor,move,score\n0,0,Reposition\n"},
            "esperado a mais": {"esperado": esperado.replace("0,ouvir,0,250", "0,ouvir,0,250,x", 1)},
            "esperado a menos": {"esperado": esperado.replace("0,ouvir,0,250", "0,ouvir,0", 1)},
            "eventos a mais": {"eventos": [s.eventos_csv([]) + "100,improviso_inicio,,x\n"]},
            "eventos a menos": {"eventos": [s.eventos_csv([]) + "100,improviso_inicio\n"]},
            "hipóteses a mais": {"hipoteses": cab_h + '0,1,partial,"ouvir",1,x\n'},
            "hipóteses a menos": {"hipoteses": cab_h + '0,1,partial,"ouvir"\n'},
        }
        for nome, entrada in casos.items():
            with self.subTest(caso=nome):
                self._rejeita(**entrada)

    def test_rodada2_hipoteses_sem_texto_de_teste_correspondente_sai_com_2(self):
        """OPR-037 (X70, A9): as latências 1 e 2 precisam do roteiro, achado pela coluna palavra do esperado."""
        tempos, esperado = _base()
        alterado = esperado.replace("\n1,bem,", "\n1,mal,", 1)
        self.assertNotEqual(esperado, alterado)
        traj = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
        codigo, dados, out, err = s.rodar_metricas(metricas.main, traj, alterado, [s.eventos_csv([])],
                                                   hipoteses="t_ms,session,kind,words,accepted\n")
        self.assertEqual((2, None, ""), (codigo, dados, out))
        self.assertIn("coluna palavra", err)
        codigo, dados, _o, _e = s.rodar_metricas(metricas.main, traj, alterado, [s.eventos_csv([])])
        self.assertEqual(0, codigo, "sem --hipoteses o esperado de outro texto é aceito")

    def test_rodada2_arquivo_ausente_ou_nao_utf8_sai_com_2(self):
        """OPR-036 (X71): sem traceback e sem rc 1 (que seria lido como REPROVA)."""
        tempos, esperado = _base()
        traj = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
        with s.Pasta() as p:
            ok = [p.arquivo("x.trajectory.csv", traj), p.arquivo("x.expected.csv", esperado),
                  p.arquivo("x0.events.csv", s.eventos_csv([]))]
            ausente = str(p.dir / "nao_existe.csv")
            invalido = p.arquivo("ruim.csv", b"t_ms,cursor,move,score\n\xff\xfe\n")
            casos = {"trajetória ausente": [ausente] + ok[1:], "eventos ausente": ok[:2] + [ausente],
                     "esperado não UTF-8": [ok[0], invalido, ok[2]], "eventos não UTF-8": ok[:2] + [invalido]}
            for nome, argv in casos.items():
                with self.subTest(caso=nome):
                    codigo, out, err = s.rodar(metricas.main, argv)
                    self.assertEqual((2, ""), (codigo, out))
                    self.assertIn("metricas.py:", err)


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

    def test_rodada2_tokens_com_fim_nos_3_s_depois_do_pulo_ficam_fora_do_fala_cursor(self):
        """Varredura (metricas.py:304): as latências excluem toque, intervalos E os 3 s depois do pulo (D15)."""
        tempos, esperado = _base()
        linhas = [(0, 0, "Reposition")] + [(tempo[1] + 400, i, "Short") for i, tempo in enumerate(tempos) if i > 0]
        eventos = [(29900, "pulo_inicio", "100"), (30000, "pulo_fim", "")]
        _c, dados, _o, _e = _medir(linhas, esperado, eventos)
        self.assertEqual(N - 1 - 10, dados["latencias"]["fala_cursor"]["n"])  # fins 30250..32950: tokens 100..109

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

    def test_rodada2_percentil_95_em_n_100_e_estatisticas_vazias(self):
        """Varredura (metricas.py:208, :213)."""
        self.assertEqual(95, metricas._percentil95(list(range(1, 101))))
        self.assertEqual(38, metricas._percentil95(list(range(40, 0, -1))))
        self.assertEqual({"n": 0, "mediana": None, "p95": None, "maximo": None}, metricas._estatisticas([]))
        self.assertEqual({"n": 3, "mediana": 3, "p95": 5, "maximo": 5}, metricas._estatisticas([5, 1, 3]))

    def test_rodada2_primeira_amostra_e_o_primeiro_multiplo_de_100(self):
        """Varredura (metricas.py:267): teto de t0 em múltiplos de 100, para t0 de 0 a mais de 10 s."""
        for t0, primeira in ((0, 0), (1, 100), (99, 100), (100, 100), (101, 200), (1550, 1600), (10001, 10100),
                             (10100, 10100), (20001, 20100)):
            with self.subTest(t0=t0):
                m = self._medicao([(0, 1000)], [(t0, 0, "Reposition"), (t0 + 500, 0, "Stay")])
                self.assertEqual(primeira, m.calcular()["amostras"]["primeira_ms"])

    def test_rodada2_ultima_amostra_nunca_passa_do_fim(self):
        """Varredura (metricas.py:269): fim em 1099 ms -> a última amostra é 1000, não 1100."""
        m = self._medicao([(0, 1099)], [(0, 0, "Reposition")])
        am = m.calcular()["amostras"]
        self.assertEqual((1000, 11), (am["ultima_ms"], am["total"]))

    def test_rodada2_limite_de_95_por_cento_com_n_21(self):
        """X21: 19 de 20 é exatamente 95% (passa); 19 de 21 é 90,5% (reprova); 20 de 21 passa."""
        m = self._medicao([(0, 1000)], [(0, 0, "Reposition")])
        for n, ate3, veredito in ((20, 19, "PASSA"), (21, 19, "REPROVA"), (21, 20, "PASSA"), (20, 18, "REPROVA"),
                                  (200, 189, "REPROVA"), (200, 190, "PASSA")):
            with self.subTest(n=n, ate3=ate3):
                m.esperado_em = lambda t, limite=n - ate3: 4 if t < limite else 0
                m.cursor_em = lambda t: 0
                r = m._atraso(list(range(n)))
                self.assertEqual((n, ate3, veredito), (r["amostras"], r["ate_3"], r["veredito"]))

    def test_rodada2_ultima_amostra_inclui_o_fim_quando_e_multiplo_de_100(self):
        """X31: o fim do esperado em 1000 ms entra como amostra (0, 100, ..., 1000)."""
        m = self._medicao([(0, 1000)], [(0, 0, "Reposition")])
        am = m.calcular()["amostras"]
        self.assertEqual((0, 1000, 11), (am["primeira_ms"], am["ultima_ms"], am["total"]))

    def test_rodada2_amostragem_nunca_comeca_antes_de_zero(self):
        """X30: deslocamento negativo deixa a trajetória em -500 ms; as amostras começam em 0 (D12)."""
        m = self._medicao([(0, 1000)], [(-500, 0, "Reposition")])
        am = m.calcular()["amostras"]
        self.assertEqual((0, 11), (am["primeira_ms"], am["total"]))

    def test_percentil95_posto_mais_proximo(self):
        self.assertEqual(19, metricas._percentil95(list(range(1, 21))))
        self.assertEqual(10, metricas._percentil95(list(range(1, 11))))
        self.assertEqual(7, metricas._percentil95([7]))


if __name__ == "__main__":
    unittest.main()
