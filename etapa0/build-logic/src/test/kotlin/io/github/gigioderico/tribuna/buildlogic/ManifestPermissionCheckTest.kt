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
}
