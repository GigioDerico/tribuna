"""Apoio dos testes sem dependência das ferramentas: rodar main() no processo e pasta temporária."""

import contextlib
import io
import tempfile
from pathlib import Path


def rodar(main, argv):
    """Roda main(argv) no processo; devolve (código, stdout, stderr)."""
    out, err = io.StringIO(), io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        try:
            codigo = main(argv)
        except SystemExit as e:  # argparse
            codigo = e.code
    return codigo, out.getvalue(), err.getvalue()


class Pasta:
    """Diretório temporário com escrita de arquivos de texto."""

    def __enter__(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.dir = Path(self._tmp.name)
        return self

    def __exit__(self, *exc):
        self._tmp.cleanup()

    def arquivo(self, nome, conteudo):
        caminho = self.dir / nome
        if isinstance(conteudo, bytes):
            caminho.write_bytes(conteudo)
        else:
            caminho.write_text(conteudo, encoding="utf-8")
        return str(caminho)
