package io.github.gigioderico.tribuna.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ModelInstallerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private class DirAssetTree(
        private val root: File,
        private val delayPerOpenMs: Long = 0L,
    ) : AssetTree {
        override fun list(path: String): List<String> = File(root, path).list()?.sorted().orEmpty()

        override fun open(path: String): InputStream {
            if (delayPerOpenMs > 0L) {
                Thread.sleep(delayPerOpenMs)
            }
            return FileInputStream(File(root, path))
        }
    }

    private fun createAssets(uuid: String = "v1"): File {
        val root = tmp.newFolder("assets")
        val model = File(root, "model-pt").apply { mkdirs() }
        File(model, "final.mdl").writeText("conteudo-final-mdl")
        File(model, "Gr.fst").writeText("conteudo-gr-fst")
        File(model, "ivector").mkdirs()
        File(model, "ivector/final.ie").writeText("conteudo-ivector")
        File(model, "uuid").writeText("$uuid\n")
        return root
    }

    @Test
    fun concurrentInstall_doesNotCorruptTargetDirectory() {
        val assetsRoot = createAssets("v1-concurrent")
        val filesDir = tmp.newFolder("files")
        val startGate = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val f1 =
                executor.submit<InstalledModel> {
                    startGate.await(5, TimeUnit.SECONDS)
                    ModelInstaller(DirAssetTree(assetsRoot, delayPerOpenMs = 15L), filesDir).install()
                }
            val f2 =
                executor.submit<InstalledModel> {
                    startGate.await(5, TimeUnit.SECONDS)
                    ModelInstaller(DirAssetTree(assetsRoot, delayPerOpenMs = 15L), filesDir).install()
                }

            startGate.countDown()
            val r1 = f1.get(10, TimeUnit.SECONDS)
            val r2 = f2.get(10, TimeUnit.SECONDS)

            assertEquals("v1-concurrent", r1.id)
            assertEquals("v1-concurrent", r2.id)
            assertTrue(r1.copied || r2.copied)
            assertEquals("conteudo-final-mdl", File(filesDir, "model-pt/final.mdl").readText())
            assertEquals("conteudo-gr-fst", File(filesDir, "model-pt/Gr.fst").readText())
            assertEquals("conteudo-ivector", File(filesDir, "model-pt/ivector/final.ie").readText())
            assertEquals("v1-concurrent\n", File(filesDir, "model-pt/uuid").readText())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun incompleteTargetWithMatchingUuid_recopiesMissingFiles() {
        val assetsRoot = createAssets("v1")
        val filesDir = tmp.newFolder("files")
        val target = File(filesDir, "model-pt").apply { mkdirs() }
        // Simula diretório corrompido: uuid existe, mas faltam os modelos (.mdl, .fst)
        File(target, "uuid").writeText("v1\n")

        val installed = ModelInstaller(DirAssetTree(assetsRoot), filesDir).install()

        assertTrue(installed.copied)
        assertTrue(File(target, "final.mdl").isFile)
        assertTrue(File(target, "Gr.fst").isFile)
        assertTrue(File(target, "ivector/final.ie").isFile)

        val secondRun = ModelInstaller(DirAssetTree(assetsRoot), filesDir).install()
        assertFalse(secondRun.copied)
    }
}
