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
        self.assertEqual(sorted([f"{i}.tsv" for i in roteiro.IDS] + ["numeros.tsv", "fronteiras.tsv"]), referencias)

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

    def test_rodada2_juncao_de_apostrofo_reto_e_tipografico(self):
        """W03 (e KK5 no Kotlin, NormalizerTest): os dois apóstrofos entre letras unem as partes; fora de letras, não."""
        for texto, esperado in (("d'água", "dagua"), ("d’água", "dagua"), ("’oi", "oi"), ("oi’", "oi"),
                                ("bom-dia", "bomdia"), ("a - b", "a b")):
            with self.subTest(texto=texto):
                self.assertEqual(esperado, roteiro.normalizar(texto))


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

    def test_rodada2_tokens_por_palavras_acha_o_texto_certo(self):
        """X84 (A9): cada texto de teste volta com os seus tokens; coluna que não é de nenhum texto dá None."""
        for texto_id in roteiro.IDS:
            with self.subTest(texto=texto_id):
                texto = roteiro.ler_texto(texto_id)
                tokens = roteiro.tokenizar(texto)
                coluna = [roteiro.normalizar(texto[t.inicio:t.fim + 1]) for t in tokens]
                achados = roteiro.tokens_por_palavras(coluna)
                self.assertEqual([t.formas for t in tokens], [t.formas for t in achados])  # só índice e formas importam
        colunas = {i: len(roteiro.tokenizar(roteiro.ler_texto(i))) for i in roteiro.IDS}
        self.assertGreaterEqual(len(set(colunas.values())), 3, "Q3: há textos com tamanhos diferentes")
        self.assertIsNone(roteiro.tokens_por_palavras(["ouvir", "bem"]))
        self.assertIsNone(roteiro.tokens_por_palavras([]))


class FronteirasTest(unittest.TestCase):
    """Fronteiras da tokenização que nenhum texto de teste exercita (varredura da rodada 2; lê o mesmo
    referencia/fronteiras.tsv que o FronteirasDaTokenizacaoTest, onde o Kotlin é a autoridade)."""

    @staticmethod
    def _render(texto):
        return " ; ".join(f"{t.inicio}-{t.fim}={roteiro.formatar_formas(t.formas)}" for t in roteiro.tokenizar(texto))

    def test_rodada2_fronteiras_batem_com_a_referencia(self):
        ref = _linhas_referencia("fronteiras.tsv")
        casos = [linha.split("\t", 1) for linha in ref[1:-1]]
        self.assertEqual(f"#fronteiras={len(casos)}", ref[0])
        self.assertEqual("", ref[-1])
        self.assertGreaterEqual(len(casos), 60, "Q3")
        for texto, esperado in casos:
            with self.subTest(texto=texto):
                self.assertEqual(esperado, self._render(texto))

    def test_rodada2_o_arquivo_cobre_os_limites_e_os_livros(self):
        """Q3: sem estes casos o arquivo não prova nada sobre o limite de D14, os livros 2 e 3 e o fim da linha."""
        ref = dict(linha.split("\t", 1) for linha in _linhas_referencia("fronteiras.tsv")[1:-1])
        for obrigatorio in ("999999", "1000000", "13:999999", "13:1000000", "0,1000000", "2 Coríntios 5:17",
                            "3 João 1:4", "1 Coríntios", "13:4", "13:4 a", "13:4 a 7", "13:4, 5,", "— a", "50%", "3,5%"):
            self.assertIn(obrigatorio, ref)

    def test_rodada2_limite_de_999999_nos_numeros_por_extenso(self):
        """D14 com os valores à mão: 999.999 tem forma falada; 1.000.000 não (fica o dígito)."""
        self.assertEqual("novecentos e noventa e nove mil novecentos e noventa e nove",
                         " ".join(roteiro.masculina(999999)))
        with self.assertRaises(ValueError):
            roteiro.masculina(1000000)
        self.assertEqual([["1000000"]], [[p for f in t.formas for p in f] for t in roteiro.tokenizar("1000000")])


if __name__ == "__main__":
    unittest.main()
