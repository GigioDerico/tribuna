package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** C10: cópia do modelo de assets por `uuid` (TRD Motor de voz §Instalação do modelo 1–3; spec 0003, D12). */
class ModelInstallerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** Assets falsos sobre um diretório; `failOn` simula falha de leitura (sem espaço, arquivo corrompido). */
    private class DirAssetTree(
        private val root: File,
        private val failOn: String? = null,
    ) : AssetTree {
        override fun list(path: String): List<String> = File(root, path).list()?.sorted().orEmpty()

        override fun open(path: String): InputStream {
            if (path == failOn) throw IOException("falha simulada em $path")
            return FileInputStream(File(root, path))
        }
    }

    private fun assets(uuid: String?): File {
        val root = tmp.newFolder("assets")
        val model = File(root, "model-pt").apply { mkdirs() }
        File(model, "final.mdl").writeText("modelo")
        File(model, "ivector").mkdirs()
        File(model, "ivector/final.ie").writeText("ivector")
        if (uuid != null) File(model, "uuid").writeText("$uuid\n")
        return root
    }

    @Test
    fun semModeloNoDestino_copiaTudo_comUuidPorUltimo() {
        val files = tmp.newFolder("files")
        val installed = ModelInstaller(DirAssetTree(assets("v1 sha256=abc")), files).install()
        assertTrue(installed.copied)
        assertEquals("v1 sha256=abc", installed.id)
        assertEquals(File(files, "model-pt"), installed.dir)
        assertEquals("modelo", File(installed.dir, "final.mdl").readText())
        assertEquals("ivector", File(installed.dir, "ivector/final.ie").readText())
        assertEquals("v1 sha256=abc\n", File(installed.dir, "uuid").readText())
        assertFalse(File(files, "model-pt.tmp").exists())
    }

    @Test
    fun uuidIgualNoDestino_naoCopia() {
        val files = tmp.newFolder("files")
        val tree = DirAssetTree(assets("v1"))
        ModelInstaller(tree, files).install()
        val marker = File(files, "model-pt/marca").apply { writeText("fica") }
        val again = ModelInstaller(tree, files).install()
        assertFalse(again.copied)
        assertTrue(marker.exists())
    }

    @Test
    fun uuidDiferenteNoDestino_copiaDeNovo() {
        val files = tmp.newFolder("files")
        File(files, "model-pt").mkdirs()
        File(files, "model-pt/uuid").writeText("v0\n")
        val marker = File(files, "model-pt/marca").apply { writeText("velho") }
        val installed = ModelInstaller(DirAssetTree(assets("v1")), files).install()
        assertTrue(installed.copied)
        assertFalse(marker.exists())
        assertEquals("v1\n", File(files, "model-pt/uuid").readText())
    }

    @Test
    fun uuidAusenteNoDestino_copia() {
        val files = tmp.newFolder("files")
        File(files, "model-pt").mkdirs()
        File(files, "model-pt/final.mdl").writeText("pela metade")
        val installed = ModelInstaller(DirAssetTree(assets("v1")), files).install()
        assertTrue(installed.copied)
        assertEquals("modelo", File(files, "model-pt/final.mdl").readText())
    }

    @Test
    fun falhaNaCopia_devolveModelCorrupt_eNaoDeixaCopiaPelaMetade() {
        val files = tmp.newFolder("files")
        File(files, "model-pt").mkdirs()
        File(files, "model-pt/uuid").writeText("v0\n")
        val error =
            runCatching { ModelInstaller(DirAssetTree(assets("v1"), failOn = "model-pt/ivector/final.ie"), files).install() }
                .exceptionOrNull() as ModelLoadException
        assertEquals(EngineError.ModelCorrupt, error.error)
        assertFalse(File(files, "model-pt.tmp").exists())
        assertEquals("v0\n", File(files, "model-pt/uuid").readText())
    }

    @Test
    fun modeloAusenteNosAssets_devolveModelMissing() {
        val files = tmp.newFolder("files")
        val error =
            runCatching {
                ModelInstaller(
                    DirAssetTree(assets(uuid = null)),
                    files,
                ).install()
            }.exceptionOrNull() as ModelLoadException
        assertEquals(EngineError.ModelMissing, error.error)
    }

    @Test
    fun mesmaVersao_naoCopia_diferenteOuAusente_copia() {
        assertFalse(ModelVersion.needsCopy("v1", "v1"))
        assertFalse(ModelVersion.needsCopy("v1", " v1 "))
        assertTrue(ModelVersion.needsCopy("v1", "v0"))
        assertTrue(ModelVersion.needsCopy("v1", null))
    }
}
