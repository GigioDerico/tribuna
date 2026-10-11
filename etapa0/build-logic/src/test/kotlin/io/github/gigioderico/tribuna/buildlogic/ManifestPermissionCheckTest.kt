package io.github.gigioderico.tribuna.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestPermissionCheckTest {
    private val recordAudio = ManifestPermissionCheck.RECORD_AUDIO
    private val allowed = setOf(recordAudio)

    private fun perm(
        name: String,
        tag: String = "uses-permission",
    ) = "<$tag android:name=\"$name\" />"

    private fun manifest(vararg elements: String) =
        elements.joinToString(
            separator = "\n",
            prefix = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n",
            postfix = "\n<application android:allowBackup=\"false\" />\n</manifest>",
        )

    private fun problems(xml: String) = ManifestPermissionCheck.problems(xml, allowed = allowed, required = allowed)

    @Test
    fun soRecordAudio_passa() {
        assertEquals(emptyList<String>(), problems(manifest(perm(recordAudio))))
    }

    @Test
    fun internet_falha() {
        assertEquals(
            listOf("permissão não permitida: android.permission.INTERNET"),
            problems(manifest(perm(recordAudio), perm("android.permission.INTERNET"))),
        )
    }

    @Test
    fun accessNetworkState_falha() {
        assertEquals(
            listOf("permissão não permitida: android.permission.ACCESS_NETWORK_STATE"),
            problems(manifest(perm(recordAudio), perm("android.permission.ACCESS_NETWORK_STATE"))),
        )
    }

    @Test
    fun wakeLock_falha() {
        assertEquals(
            listOf("permissão não permitida: android.permission.WAKE_LOCK"),
            problems(manifest(perm(recordAudio), perm("android.permission.WAKE_LOCK"))),
        )
    }

    @Test
    fun qualquerOutraPermissao_falha() {
        assertEquals(
            listOf("permissão não permitida: android.permission.CAMERA"),
            problems(manifest(perm(recordAudio), perm("android.permission.CAMERA"))),
        )
    }

    @Test
    fun usesPermissionSdk23_tambemConta() {
        assertEquals(
            listOf("permissão não permitida: android.permission.INTERNET"),
            problems(manifest(perm(recordAudio), perm("android.permission.INTERNET", tag = "uses-permission-sdk-23"))),
        )
    }

    @Test
    fun semRecordAudio_falha() {
        assertEquals(
            listOf("permissão exigida ausente: android.permission.RECORD_AUDIO"),
            problems(manifest()),
        )
    }

    @Test
    fun listaTodasAsProibidas_emOrdemAlfabetica() {
        assertEquals(
            listOf(
                "permissão não permitida: android.permission.INTERNET",
                "permissão não permitida: android.permission.WAKE_LOCK",
            ),
            problems(manifest(perm("android.permission.WAKE_LOCK"), perm(recordAudio), perm("android.permission.INTERNET"))),
        )
    }

    @Test
    fun declaracaoDePermissaoPropria_falha() {
        // XML de E1 da CIA 0003: o que o androidx.core injeta sem o tools:node="remove".
        val nome = "io.github.gigioderico.tribuna.etapa0.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        assertEquals(
            listOf(
                "permissão não permitida: $nome",
                "declaração de permissão não permitida: $nome",
            ),
            problems(
                manifest(
                    perm(recordAudio),
                    "<permission android:name=\"$nome\" android:protectionLevel=\"signature\" />",
                    perm(nome),
                ),
            ),
        )
    }

    @Test
    fun soADeclaracao_semUsesPermission_tambemFalha() {
        assertEquals(
            listOf("declaração de permissão não permitida: x.y.MINHA"),
            problems(manifest(perm(recordAudio), "<permission android:name=\"x.y.MINHA\" />")),
        )
    }

    @Test
    fun grupoEArvoreDePermissao_falham() {
        assertEquals(
            listOf(
                "declaração de permissão não permitida: x.y.ARVORE",
                "declaração de permissão não permitida: x.y.GRUPO",
            ),
            problems(
                manifest(
                    perm(recordAudio),
                    "<permission-tree android:name=\"x.y.ARVORE\" />",
                    "<permission-group android:name=\"x.y.GRUPO\" />",
                ),
            ),
        )
    }
}
