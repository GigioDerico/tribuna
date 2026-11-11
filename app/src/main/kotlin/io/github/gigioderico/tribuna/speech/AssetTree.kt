package io.github.gigioderico.tribuna.speech

import java.io.InputStream

/** Leitura dos assets empacotados (costura para a JVM; em Android, o `AssetManager`). */
interface AssetTree {
    /** Nomes dentro de `path`; lista vazia = arquivo ou caminho inexistente (como `AssetManager.list`). */
    fun list(path: String): List<String>

    /** Abre um arquivo; lanÃƒÂ§a `IOException` se nÃƒÂ£o existir. */
    fun open(path: String): InputStream
}
