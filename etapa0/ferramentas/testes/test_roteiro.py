"""Paridade com o Kotlin (spec 0004, B1, B3, B4; brainstorm D3 e D4)."""

import hashlib
import unittest

import roteiro


def _linhas_referencia(nome):
    return (roteiro.REFERENCIA / nome).read_text(encoding="utf-8").split("\n")


class ReferenciaTest(unittest.TestCase):
    def test_B1_ha_uma_referencia_por_texto_e_uma_de_numeros(self):
        textos = sorted(p.stem for p in roteiro.TEXTOS.glob("*.txt"))
        self.assertEqual(sorted(roteiro.IDS), textos, "IDS do roteiro.py x etapa0/textos/")
        referencias = sorted(p.name for p in roteiro.REFERENCIA.glob("*.tsv"))
        self.assertEqual(sorted([f"{i}.tsv" for i in roteiro.IDS] + ["numeros.tsv"]), referencias)

    def test_B3_tokens_de_cada_texto_batem_com_a_referencia(self):
        for texto_id in roteiro.IDS:
            with self.subTest(texto=texto_id):
                bruto = (roteiro.TEXTOS / f"{texto_id}.txt").read_bytes()
                texto = bruto.decode("utf-8")
                self.assertTrue(all(ord(c) <= 0xFFFF for c in texto), "fora do BMP: UTF-16 != índice Python")
                ref = _linhas_referencia(f"{texto_id}.tsv")
                cabecalho = dict(campo.split("=", 1) for campo in ref[0].lstrip("#").split(" "))
                self.assertEqual(
                    hashlib.sha256(bruto).hexdigest(),
                    cabecalho["sha256"],
                    f"referência defasada: {texto_id}.txt mudou; regenere (impl_plan 0004, Regenerar as referências)",
                )
                tokens = roteiro.tokenizar(texto)
                self.assertGreater(len(tokens), 50, "Q3")
                self.assertEqual(int(cabecalho["tokens"]), len(tokens))
                obtido = [f"{t.indice}\t{t.inicio}\t{t.fim}\t{roteiro.formatar_formas(t.formas)}" for t in tokens]
                self.assertEqual(ref[1:-1], obtido)
                self.assertEqual("", ref[-1], "a referência termina em \\n")

    def test_B3_formas_dos_numeros_batem_com_a_referencia(self):
        ref = _linhas_referencia("numeros.tsv")
        self.assertEqual(f"#numeros={len(ref) - 2}", ref[0])
        self.assertGreaterEqual(len(ref) - 2, 2101, "Q3: 0 a 2.100 e a lista de milhares")
        for linha in ref[1:-1]:
            n, cardinal, masculina = linha.split("\t")
            with self.subTest(n=n):
                self.assertEqual(cardinal, roteiro.formatar_formas(roteiro.formas_cardinais(int(n))))
                self.assertEqual(masculina, roteiro.formatar_formas(roteiro.formas_masculinas(int(n))))

    def test_B3_numero_fora_da_faixa_lanca_erro(self):
        with self.assertRaises(ValueError):
            roteiro.masculina(roteiro.MAX + 1)
        with self.assertRaises(ValueError):
            roteiro.masculina(-1)


class NormalizadorTest(unittest.TestCase):
    def test_B4_idempotente_em_todos_os_textos(self):
        for texto_id in roteiro.IDS:
            with self.subTest(texto=texto_id):
                uma = roteiro.normalizar(roteiro.ler_texto(texto_id))
                self.assertGreater(len(uma), 100, "Q3")
                self.assertEqual(uma, roteiro.normalizar(uma))

    def test_B4_exemplos_iguais_ao_Normalizer_kt(self):
        self.assertEqual("tres versiculo", roteiro.normalizar("Três, Versículo!"))
        self.assertEqual("bomdia", roteiro.normalizar("bom-dia"))
        self.assertEqual("escrevelos", roteiro.normalizar("escrevê-los"))


class CatalogoTest(unittest.TestCase):
    def test_identifica_so_bytes_identicos(self):
        for texto_id in roteiro.IDS:
            bruto = (roteiro.TEXTOS / f"{texto_id}.txt").read_bytes()
            with self.subTest(texto=texto_id):
                self.assertEqual(texto_id, roteiro.identificar_texto(bruto))
                self.assertIsNone(roteiro.identificar_texto(bruto + b"x"))
                self.assertIsNone(roteiro.identificar_texto(bruto[:-1]))
                self.assertIn(b"\n", bruto, "Q3: o caso CRLF abaixo precisa de fim de linha")
                self.assertIsNone(roteiro.identificar_texto(bruto.replace(b"\n", b"\r\n")))


if __name__ == "__main__":
    unittest.main()
