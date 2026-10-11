"""python3 etapa0/ferramentas/testar.py: roda todos os testes das ferramentas (spec 0004, D2 e A1).

Só unittest da biblioteca padrão, sem o pacote vosk e sem rede. Sai com 0 só se todos passarem e nenhum
for pulado (A1: "sem nenhum pulado nem falho"). Argumento opcional: padrão de nome (ex.: test_metricas*.py).
"""

import sys
import unittest
from pathlib import Path

AQUI = Path(__file__).resolve().parent


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    padrao = argv[0] if argv else "test_*.py"
    sys.path.insert(0, str(AQUI))
    suite = unittest.defaultTestLoader.discover(start_dir=str(AQUI / "testes"), pattern=padrao, top_level_dir=str(AQUI))
    resultado = unittest.TextTestRunner(verbosity=2).run(suite)
    print(
        f"testar.py: {resultado.testsRun} testes, {len(resultado.failures)} falhas, "
        f"{len(resultado.errors)} erros, {len(resultado.skipped)} pulados"
    )
    ok = resultado.wasSuccessful() and not resultado.skipped and resultado.testsRun > 0
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
