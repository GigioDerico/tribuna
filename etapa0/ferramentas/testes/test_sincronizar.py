"""sincronizar.py com sinais sintéticos (spec 0005, D3, D4; impl_plan 0005, A3).

Cenário base, como o roteiro do spec: QuickTime liga, 1.370 ms depois o orador toca Ouvir (t_app = 0), palma
2 s depois (WAV 3.370 ms), silêncio até a leitura (WAV 5.370 ms), WAV de 40 s. O level.csv sai do mesmo sinal,
com ganho e piso de outro microfone e jitter de 0 a 30 ms por linha.
"""

import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import sincronizar
from testes import sinal
from testes.apoio import Pasta, rodar

D = 1370
_TMP = None
AMOSTRAS = None
WAV = None


def setUpModule():
    global _TMP, AMOSTRAS, WAV
    _TMP = tempfile.TemporaryDirectory()
    AMOSTRAS = sinal.fala_sintetica(40000, 1, palma_ms=D + 2000, silencio_ate_ms=D + 4000)
    WAV = sinal.escrever_wav(Path(_TMP.name) / "T01_giorgio_sala_t1.wav", AMOSTRAS)


def tearDownModule():
    _TMP.cleanup()


def _rodar(level, *extra, wav=None):
    with Pasta() as p:
        caminho_json = str(p.dir / "s.json")
        codigo, out, err = rodar(sincronizar.main, [wav or WAV, p.arquivo("x.level.csv", level), *extra,
                                                    "--json", caminho_json])
        dados = json.loads(Path(caminho_json).read_text(encoding="utf-8")) if Path(caminho_json).exists() else None
    return codigo, dados, out, err


class DeslocamentoTest(unittest.TestCase):
    def test_D3_acha_o_deslocamento_ao_vivo_a_10_ms(self):
        codigo, dados, out, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25))
        self.assertEqual(0, codigo)
        self.assertLessEqual(abs(dados["deslocamento_ms"] - D), 10)
        self.assertNotEqual(0, dados["deslocamento_ms"] % 100, "a grade fina de 10 ms foi usada")
        self.assertFalse(dados["sensivel"])
        self.assertEqual([], dados["motivos"])
        self.assertEqual(0, dados["linhas_fora_do_wav"])
        self.assertIn("SINCRONIZADO", out)

    def test_D3_ganho_e_piso_de_outro_microfone_nao_mudam_o_deslocamento(self):
        for ganho, piso in ((0.05, 0.0), (1.0, 0.0), (0.05, 0.002)):
            with self.subTest(ganho=ganho, piso=piso):
                codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=ganho, piso=piso))
                self.assertEqual(0, codigo)
                self.assertLessEqual(abs(dados["deslocamento_ms"] - D), 10)

    def test_D3_reproducao_pela_fonte_wav_com_faixa(self):
        level = sinal.level_csv(AMOSTRAS, -40, 41000, ganho=0.5)
        codigo, dados, _, _ = _rodar(level, "--faixa-ms", "-1000", "1000")
        self.assertEqual(0, codigo)
        self.assertLessEqual(abs(dados["deslocamento_ms"] + 40), 10)
        self.assertEqual([-1000, 1000], dados["faixa_ms"])
        self.assertGreater(dados["linhas_fora_do_wav"], 0, "o app seguiu depois do fim do WAV")

    def test_D3_wav_que_nao_cobre_a_gravacao_sai_com_2(self):
        level = sinal.level_csv(AMOSTRAS, -40, 41000, ganho=0.5)
        codigo, dados, out, err = _rodar(level)
        self.assertEqual((2, None, ""), (codigo, dados, out))
        self.assertIn("não cobre", err)

    def test_faixa_padrao_nas_bordas(self):
        self.assertEqual((0, 1000), sincronizar.faixa_padrao(200, 39000, 40000))
        self.assertEqual((10, 10), sincronizar.faixa_padrao(195, 39985, 40000))
        self.assertEqual((0, 0), sincronizar.faixa_padrao(200, 39000, 39000))
        self.assertEqual((-1000, -1000), sincronizar.faixa_padrao(1200, 41000, 40000))
        with self.assertRaises(sincronizar.ErroEntrada):
            sincronizar.faixa_padrao(200, 39001, 39000)

    def test_faixa_invertida_sai_com_2(self):
        codigo, _, _, err = _rodar(sinal.level_csv(AMOSTRAS, D, 38000), "--faixa-ms", "500", "400")
        self.assertEqual(2, codigo)
        self.assertIn("MIN maior que MAX", err)


class PalmaTest(unittest.TestCase):
    def test_D4_palma_confere_e_fica_registrada(self):
        _, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25))
        p = dados["palma"]
        self.assertTrue(p["confere"])
        self.assertTrue(1800 <= p["app_ms"] <= 2230, p)
        self.assertTrue(D + 1800 <= p["wav_pico_ms"] <= D + 2030, p)
        self.assertLessEqual(abs(p["diferenca_ms"]), sincronizar.TOLERANCIA_MS)
        self.assertGreater(p["proeminencia_db"], 20)

    def test_D3_faixa_errada_palma_nao_confere_sai_com_1(self):
        codigo, dados, out, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25), "--faixa-ms", "3000", "3500")
        self.assertEqual(1, codigo)
        self.assertFalse(dados["palma"]["confere"])
        self.assertTrue(dados["sensivel"])
        self.assertTrue(any(m.startswith("palma não confere") for m in dados["motivos"]))
        self.assertIn("SENSÍVEL AO DESLOCAMENTO", out)

    def test_D3_palma_usa_so_as_linhas_em_volta_dela(self):
        # corte de 600 ms no WAV aos 6 s: o resto da gravação puxa o deslocamento global; a palma (2 s) não
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25, salto=(6000, 600)))
        self.assertLessEqual(abs(dados["deslocamento_ms"] - (D + 600)), 10)
        self.assertFalse(dados["palma"]["confere"])
        self.assertLessEqual(abs(dados["palma"]["deslocamento_ms"] - D), 200)
        self.assertEqual(1, codigo)

    def test_sem_linha_ate_o_limite_da_palma_nao_confere(self):
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000), "--palma-ate-ms", "100")
        self.assertEqual(1, codigo)
        self.assertIn("nenhuma linha do level.csv até 100 ms", dados["palma"]["motivo"])

    def test_D4_palma_tardia_ate_4000_ms_entra_no_padrao(self):
        amostras = sinal.fala_sintetica(30000, 2, palma_ms=D + 3700, silencio_ate_ms=D + 5000)
        with Pasta() as p:
            wav = sinal.escrever_wav(p.dir / "T09_giorgio_sala_t1.wav", amostras)
            _, dados, _, _ = _rodar(sinal.level_csv(amostras, D, 28000, ganho=0.25), wav=wav)
        self.assertEqual(4000, dados["palma_ate_ms"])
        self.assertTrue(3700 < dados["palma"]["app_ms"] <= 4000, dados["palma"])
        self.assertTrue(dados["palma"]["confere"])

    def test_D3_palma_confere_ate_200_ms_do_global(self):
        linhas = sincronizar.ler_level("t_ms,rms\n200,0.001\n400,0.5\n600,0.001\n800,0.002\n")
        wav = sincronizar.Wav([1] * 300)
        for d_palma, confere in ((1200, True), (800, True), (1210, False), (790, False)):
            with self.subTest(d_palma=d_palma), mock.patch.object(sincronizar, "buscar", return_value=(d_palma, 0.9)):
                res = sincronizar.palma(linhas, wav, 1000, 4000)
                self.assertEqual((confere, d_palma - 1000), (res["confere"], res["diferenca_ms"]))

    def test_limite_da_palma_inclui_a_linha_no_limite(self):
        level = "t_ms,rms\n200,0.001\n400,0.5\n600,0.001\n800,0.002\n"
        res = sincronizar.palma(sincronizar.ler_level(level), sincronizar.Wav([1] * 200), 0, 400)
        self.assertEqual(400, res["app_ms"])

    def test_motivos_nos_limites(self):
        def p(confere):
            return {"confere": confere, "motivo": None}

        def dv(ms, indeterminada=False):
            return {"deriva_ms": ms, "indeterminada": indeterminada}

        self.assertEqual([], sincronizar.motivos_de(p(True), dv(200)))
        self.assertEqual([], sincronizar.motivos_de(p(True), dv(-200)))
        self.assertEqual(["deriva acima de 200 ms"], sincronizar.motivos_de(p(True), dv(201)))
        self.assertEqual(["deriva acima de 200 ms"], sincronizar.motivos_de(p(True), dv(-201)))
        self.assertEqual(["palma não confere"], sincronizar.motivos_de(p(False), dv(0)))
        self.assertEqual(["deriva indeterminada"], sincronizar.motivos_de(p(True), dv(None, True)))


class BuscaTest(unittest.TestCase):
    class _Fixa:
        def __init__(self, valores):
            self.valores = valores

        def correlacao(self, _wav, d):
            return self.valores.get(d)

    def test_empate_global_fica_com_o_menor_e_o_da_palma_com_o_meio(self):
        serie = self._Fixa({10: 0.5, 20: 0.9, 30: 0.9 - 1e-12, 40: 0.9, 50: 0.9, 60: 0.2})
        self.assertEqual(20, sincronizar._melhor(serie, None, range(0, 70, 10))[0])
        self.assertEqual(30, sincronizar._melhor(serie, None, range(0, 70, 10), meio_do_empate=True)[0])
        serie = self._Fixa({10: 0.9, 20: 0.9, 30: 0.9})
        self.assertEqual(20, sincronizar._melhor(serie, None, [10, 20, 30], meio_do_empate=True)[0])
        self.assertEqual((None, None), sincronizar._melhor(self._Fixa({}), None, [10, 20]))

    def test_empate_tolera_so_o_arredondamento(self):
        serie = self._Fixa({10: 0.9, 20: 0.9 - 1e-6, 30: 0.9 - 1e-6})
        self.assertEqual(10, sincronizar._melhor(serie, None, [10, 20, 30], meio_do_empate=True)[0])

    def test_wav_em_silencio_sai_com_2(self):
        with Pasta() as p:
            wav = sinal.escrever_wav(p.dir / "s.wav", [0] * 16000 * 40)
            codigo, _, _, err = _rodar(sinal.level_csv(AMOSTRAS, D, 38000), wav=wav)
        self.assertEqual(2, codigo)
        self.assertIn("sem variância", err)


class DerivaTest(unittest.TestCase):
    def test_D3_deriva_de_1_por_cento_passa_de_200_e_sai_com_1(self):
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, deriva_ppm=10000))
        dv = dados["deriva"]
        self.assertEqual(1, codigo)
        self.assertIn("deriva acima de 200 ms", dados["motivos"])
        self.assertLess(dv["deriva_ms"], -300, "1% de 38 s é ~380 ms, com a extensão à gravação inteira")
        self.assertGreater(dv["deriva_ms"], -450)
        self.assertLess(abs(dv["deriva_ms"]), 2.2 * abs(dv["diferenca_metades_ms"]))

    def test_D3_deriva_pequena_passa(self):
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, deriva_ppm=2000))
        self.assertEqual(0, codigo)
        self.assertLessEqual(abs(dados["deriva"]["deriva_ms"]), 200)
        self.assertGreater(abs(dados["deriva"]["deriva_ms"]), 0)

    def test_D3_corte_na_gravacao_de_referencia_sai_sensivel(self):
        for salto in ((19000, 3000), (19000, -3000)):
            with self.subTest(salto=salto):
                codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, salto=salto))
                self.assertEqual(1, codigo)
                self.assertIn("deriva acima de 200 ms", dados["motivos"])

    def test_D3_metade_na_borda_da_busca_deriva_indeterminada(self):
        linhas = sincronizar.ler_level("t_ms,rms\n200,0.1\n400,0.2\n600,0.1\n800,0.3\n")
        for fim, indeterminada in ((D + 1000, True), (D - 1000, True), (None, True), (D + 990, False),
                                   (D - 990, False)):
            with self.subTest(fim=fim), mock.patch.object(sincronizar, "buscar",
                                                          side_effect=[(D, 0.9), (fim, 0.9 if fim else None)]):
                dv = sincronizar.deriva(linhas, None, D)
                self.assertEqual(indeterminada, dv["indeterminada"])
                self.assertEqual(None if indeterminada else round((D - fim) * 600 / 400), dv["deriva_ms"])

    def test_D3_metade_sem_variancia_deriva_indeterminada(self):
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, constante_depois_de_ms=17000))
        self.assertEqual(1, codigo)
        self.assertTrue(dados["deriva"]["indeterminada"])
        self.assertIsNone(dados["deriva"]["deriva_ms"])
        self.assertIn("deriva indeterminada", dados["motivos"])


class EntradaTest(unittest.TestCase):
    def test_level_invalido_sai_com_2(self):
        casos = {
            "cabeçalho": "t,rms\n200,0.1\n400,0.1\n600,0.2\n800,0.1\n",
            "campos": "t_ms,rms\n200,0.1,9\n400,0.1\n600,0.2\n800,0.1\n",
            "inteiro": "t_ms,rms\n2x0,0.1\n400,0.1\n600,0.2\n800,0.1\n",
            "negativo": "t_ms,rms\n-200,0.1\n400,0.1\n600,0.2\n800,0.1\n",
            "rms acima de 1": "t_ms,rms\n200,1.5\n400,0.1\n600,0.2\n800,0.1\n",
            "rms negativo": "t_ms,rms\n200,-0.1\n400,0.1\n600,0.2\n800,0.1\n",
            "nan": "t_ms,rms\n200,nan\n400,0.1\n600,0.2\n800,0.1\n",
            "decrescente": "t_ms,rms\n200,0.1\n400,0.1\n300,0.2\n800,0.1\n",
            "poucas linhas": "t_ms,rms\n200,0.1\n400,0.1\n600,0.2\n",
            "inf": "t_ms,rms\n200,inf\n400,0.1\n600,0.2\n800,0.1\n",
        }
        for nome, level in casos.items():
            with self.subTest(caso=nome):
                codigo, dados, out, err = _rodar(level)
                self.assertEqual((2, None, ""), (codigo, dados, out))
                self.assertTrue(err.startswith("sincronizar.py: "))

    def test_tudo_no_mesmo_instante_sai_com_2_sem_variancia(self):
        codigo, _, _, err = _rodar("t_ms,rms\n5000,0.1\n5000,0.2\n5000,0.3\n5000,0.1\n")
        self.assertEqual(2, codigo)
        self.assertIn("sem variância", err)

    def test_level_nos_limites_aceito(self):
        linhas = sincronizar.ler_level("t_ms,rms\n0,0\n0,1\n400,0.5\n600,0.2\n")
        self.assertEqual([(0, 0.0), (0, 1.0), (400, 0.5), (600, 0.2)], linhas)

    def test_wav_fora_do_formato_sai_com_2(self):
        with Pasta() as p:
            estereo = sinal.escrever_wav(p.dir / "e.wav", AMOSTRAS, canais=2)
            taxa = sinal.escrever_wav(p.dir / "t.wav", AMOSTRAS, taxa=44100)
            corrompido = p.arquivo("c.wav", b"RIFF0000WAVEfmt ")
            for caminho in (estereo, taxa, corrompido, str(p.dir / "nao-existe.wav")):
                with self.subTest(wav=Path(caminho).name):
                    codigo, dados, out, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000), wav=caminho)
                    self.assertEqual((2, None, ""), (codigo, dados, out))

    def test_sem_variancia_sai_com_2(self):
        level = "t_ms,rms\n" + "".join(f"{200 * k},0.1\n" for k in range(1, 50))
        codigo, _, _, err = _rodar(level)
        self.assertEqual(2, codigo)
        self.assertIn("sem variância", err)

    def test_energias_do_wav_por_fatia_de_10_ms(self):
        with Pasta() as p:
            caminho = sinal.escrever_wav(p.dir / "u.wav", [1] * 160 + [2] * 160 + [3] * 100)
            self.assertEqual([160, 640], sincronizar.energias_do_wav(caminho))

    def test_bloco_fora_do_wav_e_silencio(self):
        wav = sincronizar.Wav([5] * 30)
        self.assertEqual(100, wav.energia(20))
        self.assertEqual(50, wav.energia(10))
        self.assertEqual(50, wav.energia(40))
        self.assertEqual(0, wav.energia(60))
        self.assertTrue(wav.dentro(20) and wav.dentro(30))
        self.assertFalse(wav.dentro(19) or wav.dentro(31))


class SaidaTest(unittest.TestCase):
    def test_json_e_proveniencia(self):
        level = sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25)
        codigo, dados, out, _ = _rodar(level)
        self.assertEqual(0, codigo)
        for chave in ("deslocamento_ms", "correlacao", "faixa_ms", "linhas", "linhas_fora_do_wav", "duracao_wav_ms",
                      "palma", "deriva", "sensivel", "motivos", "codigo", "entradas", "palma_ate_ms"):
            self.assertIn(chave, dados)
        self.assertEqual(40000, dados["duracao_wav_ms"])
        self.assertEqual(["wav", "level"], [e["papel"] for e in dados["entradas"]])
        self.assertEqual("T01_giorgio_sala_t1.wav", dados["entradas"][0]["arquivo"])
        self.assertEqual(64, len(dados["entradas"][0]["sha256"]))
        self.assertIn("t_wav = t_app + deslocamento", out)
        self.assertIn("sha256=", out)

    def test_deterministico(self):
        level = sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25)
        self.assertEqual(_rodar(level)[1], _rodar(level)[1])

    def test_ajuda(self):
        codigo, out, _ = rodar(sincronizar.main, ["--help"])
        self.assertEqual(0, codigo)
        for termo in ("wav", "level", "--faixa-ms", "--palma-ate-ms", "--json"):
            self.assertIn(termo, out)


if __name__ == "__main__":
    unittest.main()
