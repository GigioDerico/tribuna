"""timestamps.py com reconhecedor falso injetado (spec 0004, C1 a C5; D5). Sem o pacote vosk."""

import json
import sys
import unittest
import wave
from unittest import mock

import timestamps
from testes.apoio import Pasta, rodar

RESULTADO = {
    "result": [
        {"word": "ouvir", "start": 1.21, "end": 1.53, "conf": 1.0},
        {"word": "bem", "start": 1.53, "end": 1.79, "conf": 0.98},
    ],
    "text": "ouvir bem",
}
FINAL = {"result": [{"word": "três", "start": 2.0, "end": 2.4, "conf": 0.9}], "text": "três"}


class ReconhecedorFalso:
    """Imita o KaldiRecognizer: o 1º bloco fecha um Result(); o resto só acumula até o FinalResult()."""

    def __init__(self):
        self.bytes_recebidos = 0
        self.blocos = 0

    def AcceptWaveform(self, dados):  # noqa: N802 (nome da API do Vosk)
        self.bytes_recebidos += len(dados)
        self.blocos += 1
        return self.blocos == 1

    def Result(self):  # noqa: N802
        return json.dumps(RESULTADO)

    def FinalResult(self):  # noqa: N802
        return json.dumps(FINAL)


class Fabrica:
    def __init__(self):
        self.taxas = []
        self.reconhecedor = ReconhecedorFalso()

    def __call__(self, taxa):
        self.taxas.append(taxa)
        return self.reconhecedor


def escrever_wav(caminho, taxa=16000, canais=1, largura=2, quadros=10000):
    """WAV sintético de silêncio, só em diretório temporário de teste."""
    with wave.open(caminho, "wb") as w:
        w.setnchannels(canais)
        w.setsampwidth(largura)
        w.setframerate(taxa)
        w.writeframes(b"\x00" * (quadros * canais * largura))
    return caminho


class TimestampsTest(unittest.TestCase):
    def _rodar(self, nome="T02_giorgio_salao_t2.wav", **wav):
        with Pasta() as p:
            caminho = escrever_wav(str(p.dir / nome), **wav)
            fabrica = Fabrica()
            codigo, out, err = rodar(lambda argv: timestamps.main(argv, fabrica=fabrica), [caminho])
        return codigo, out, err, fabrica

    def test_C1_csv_com_cabecalho_e_palavras_em_ordem(self):
        codigo, out, _err, fabrica = self._rodar()
        self.assertEqual(0, codigo)
        self.assertEqual([16000], fabrica.taxas)
        self.assertEqual(
            ["palavra,t_inicio_ms,t_fim_ms,conf", "ouvir,1210,1530,1.0", "bem,1530,1790,0.98", "três,2000,2400,0.9"],
            out.splitlines(),
        )

    def test_C1_ordena_por_tempo(self):
        invertido = {"result": list(reversed(RESULTADO["result"]))}
        with mock.patch.object(ReconhecedorFalso, "Result", lambda self: json.dumps(invertido)):
            codigo, out, _err, _f = self._rodar()
        self.assertEqual(0, codigo)
        self.assertEqual(["ouvir", "bem", "três"], [linha.split(",")[0] for linha in out.splitlines()[1:]])

    def test_C1_milissegundos_arredondados(self):
        """4.02 × 1000 = 4019.9999999999995 em ponto flutuante: round dá 4020 (D5), int daria 4019."""
        quebrado = {"result": [{"word": "bem", "start": 4.02, "end": 4.06, "conf": 1.0}]}
        with mock.patch.object(ReconhecedorFalso, "Result", lambda self: json.dumps(quebrado)):
            _codigo, out, _err, _f = self._rodar()
        self.assertEqual("bem,4020,4060,1.0", out.splitlines()[2])

    def test_C2_palavra_sai_como_o_reconhecedor_devolveu(self):
        _codigo, out, _err, _f = self._rodar()
        self.assertIn("três,2000,2400,0.9", out.splitlines())
        self.assertNotIn("tres", out)

    def test_C3_wav_valido_entrega_todos_os_quadros(self):
        codigo, _out, _err, fabrica = self._rodar(quadros=10001)
        self.assertEqual(0, codigo)
        self.assertEqual(10001 * 2, fabrica.reconhecedor.bytes_recebidos)
        self.assertGreater(fabrica.reconhecedor.blocos, 1, "Q3: mais de um bloco")

    def test_C3_formato_errado_sai_com_2_sem_stdout(self):
        for caso in ({"taxa": 44100}, {"canais": 2}, {"largura": 1}):
            with self.subTest(caso=caso):
                codigo, out, err, fabrica = self._rodar(**caso)
                self.assertEqual(2, codigo)
                self.assertEqual("", out)
                self.assertIn("16 kHz", err)
                self.assertEqual([], fabrica.taxas)

    def test_C4_nome_fora_do_padrao_sai_com_2(self):
        for nome in ("discurso.wav", "X99_giorgio_sala_t1.wav", "T02_giorgio_salao.wav", "T02_giorgio_salao_t2.WAV"):
            with self.subTest(nome=nome):
                codigo, out, _err, _f = self._rodar(nome=nome)
                self.assertEqual(2, codigo)
                self.assertEqual("", out)

    def test_C4_nome_do_protocolo_e_aceito(self):
        for nome in ("T02_giorgio_salao_t2.wav", "L30_voz2_sala_t1.wav"):
            with self.subTest(nome=nome):
                self.assertEqual(0, self._rodar(nome=nome)[0])

    def test_C5_sem_pacote_vosk_sai_com_3(self):
        with Pasta() as p, mock.patch.dict(sys.modules, {"vosk": None}):
            caminho = escrever_wav(str(p.dir / "T01_giorgio_sala_t1.wav"))
            codigo, out, err = rodar(timestamps.main, [caminho, "--modelo", str(p.dir)])
        self.assertEqual(3, codigo)
        self.assertEqual("", out)
        self.assertIn("vosk", err)

    def test_C5_sem_modelo_sai_com_2(self):
        with Pasta() as p:
            caminho = escrever_wav(str(p.dir / "T01_giorgio_sala_t1.wav"))
            codigo, out, err = rodar(timestamps.main, [caminho])
            self.assertEqual(2, codigo)
            self.assertIn("--modelo", err)
            codigo, out, err = rodar(timestamps.main, [caminho, "--modelo", str(p.dir / "nao_existe")])
            self.assertEqual(2, codigo)
        self.assertEqual("", out)

    def test_saida_em_arquivo_com_o(self):
        with Pasta() as p:
            caminho = escrever_wav(str(p.dir / "T01_giorgio_sala_t1.wav"))
            destino = p.dir / "T01_giorgio_sala_t1.words.csv"
            codigo, out, _err = rodar(lambda a: timestamps.main(a, fabrica=Fabrica()), [caminho, "-o", str(destino)])
            self.assertEqual(0, codigo)
            self.assertEqual("", out)
            self.assertTrue(destino.read_text(encoding="utf-8").startswith("palavra,t_inicio_ms,t_fim_ms,conf\n"))


if __name__ == "__main__":
    unittest.main()
