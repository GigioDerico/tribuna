"""vizinhas.py: a folha de d nos relógios das corridas d − 200 e d + 200 (spec 0005, D3, E1; impl_plan 0005,
Correções da revisão rodada 1, C1)."""

import unittest

import vizinhas
from testes import sintetico as s
from testes.apoio import Pasta, rodar

FOLHA = [(1000, "pulo_inicio", "47"), (2500, "pulo_fim", ""), (30000, "silencio_inicio", ""),
         (60000, "silencio_fim", "")]


def _linhas(caminho):
    return caminho.read_text(encoding="utf-8").splitlines()


class VizinhasTest(unittest.TestCase):
    def test_D3_m200_soma_200_e_p200_tira_200(self):
        with Pasta() as p:
            entrada = p.arquivo("T05_giorgio_sala_t1.folha.events.csv", s.eventos_csv(FOLHA))
            codigo, out, err = rodar(vizinhas.main, [entrada])
            self.assertEqual(0, codigo, err)
            self.assertEqual(["t_ms,evento,detalhe", "1200,pulo_inicio,47", "2700,pulo_fim,", "30200,silencio_inicio,",
                              "60200,silencio_fim,"], _linhas(p.dir / "T05_giorgio_sala_t1.m200.folha.events.csv"))
            self.assertEqual(["t_ms,evento,detalhe", "800,pulo_inicio,47", "2300,pulo_fim,", "29800,silencio_inicio,",
                              "59800,silencio_fim,"], _linhas(p.dir / "T05_giorgio_sala_t1.p200.folha.events.csv"))
            self.assertEqual(s.eventos_csv(FOLHA), (p.dir / "T05_giorgio_sala_t1.folha.events.csv").read_text(
                encoding="utf-8"), "a folha de d não muda")
        self.assertIn("T05_giorgio_sala_t1.m200.folha.events.csv: +200 ms", out)
        self.assertIn("T05_giorgio_sala_t1.p200.folha.events.csv: -200 ms", out)

    def test_D7_sobrescreve_as_vizinhas_velhas(self):
        with Pasta() as p:
            entrada = p.arquivo("T06_giorgio_sala_t1.folha.events.csv", s.eventos_csv(FOLHA[2:]))
            p.arquivo("T06_giorgio_sala_t1.m200.folha.events.csv", "velha\n")
            codigo, _, _ = rodar(vizinhas.main, [entrada])
            self.assertEqual(0, codigo)
            self.assertEqual("30200,silencio_inicio,", _linhas(p.dir / "T06_giorgio_sala_t1.m200.folha.events.csv")[1])

    def test_t_zero_na_p200_aceito_e_negativo_sai_com_2(self):
        with Pasta() as p:
            entrada = p.arquivo("T06_giorgio_sala_t1.folha.events.csv",
                                s.eventos_csv([(200, "silencio_inicio", ""), (900, "silencio_fim", "")]))
            codigo, _, _ = rodar(vizinhas.main, [entrada])
            self.assertEqual(0, codigo)
            self.assertEqual("0,silencio_inicio,", _linhas(p.dir / "T06_giorgio_sala_t1.p200.folha.events.csv")[1])
        with Pasta() as p:
            entrada = p.arquivo("T06_giorgio_sala_t1.folha.events.csv",
                                s.eventos_csv([(199, "silencio_inicio", ""), (900, "silencio_fim", "")]))
            codigo, out, err = rodar(vizinhas.main, [entrada])
            self.assertEqual((2, ""), (codigo, out))
            self.assertIn("negativo", err)
            self.assertFalse((p.dir / "T06_giorgio_sala_t1.m200.folha.events.csv").exists(), "nada é escrito")

    def test_nome_fora_do_padrao_sai_com_2(self):
        for nome in ("T06_giorgio_sala_t1.events.csv", "T06_giorgio_sala_t1.m200.folha.events.csv",
                     "T06_giorgio_sala_t1.p200.folha.events.csv", ".folha.events.csv"):
            with self.subTest(nome=nome), Pasta() as p:
                codigo, out, err = rodar(vizinhas.main, [p.arquivo(nome, s.eventos_csv(FOLHA))])
                self.assertEqual((2, ""), (codigo, out))
                self.assertTrue(err.startswith("vizinhas.py: "))
                self.assertEqual([nome], [c.name for c in p.dir.iterdir()], "nada é escrito")

    def test_folha_invalida_sai_com_2(self):
        casos = {
            "cabeçalho": "t,evento,detalhe\n1000,silencio_inicio,\n",
            "evento fora da lista": s.eventos_csv([(1000, "palma", "")]),
            "par sem fim": s.eventos_csv([(1000, "silencio_inicio", "")]),
            "pulo sem índice": s.eventos_csv([(1000, "pulo_inicio", ""), (2000, "pulo_fim", "")]),
        }
        for nome, conteudo in casos.items():
            with self.subTest(caso=nome), Pasta() as p:
                codigo, out, _ = rodar(vizinhas.main, [p.arquivo("T06_giorgio_sala_t1.folha.events.csv", conteudo)])
                self.assertEqual((2, ""), (codigo, out))
                self.assertEqual(1, len(list(p.dir.iterdir())), "nada é escrito")

    def test_arquivo_ausente_sai_com_2(self):
        with Pasta() as p:
            codigo, _, _ = rodar(vizinhas.main, [str(p.dir / "T06_giorgio_sala_t1.folha.events.csv")])
        self.assertEqual(2, codigo)

    def test_ajuda(self):
        codigo, out, _ = rodar(vizinhas.main, ["--help"])
        self.assertEqual(0, codigo)
        self.assertIn("folha", out)


if __name__ == "__main__":
    unittest.main()
