package io.github.gigioderico.tribuna.buildlogic

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/** Permissões declaradas no manifesto mesclado (CLAUDE.md, regra 1; TRD §Ciclo de vida, Manifesto). */
object ManifestPermissionCheck {
    const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")

    fun declaredPermissions(manifestXml: String): List<String> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(manifestXml)))
        return PERMISSION_TAGS.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NS, "name") }
        }
    }

    fun problems(
        manifestXml: String,
        allowed: Set<String>,
        required: Set<String>,
    ): List<String> {
        val declared = declaredPermissions(manifestXml)
        val extra = declared.filterNot { it in allowed }.distinct().sorted().map { "permissão não permitida: $it" }
        val missing = (required - declared.toSet()).sorted().map { "permissão exigida ausente: $it" }
        return extra + missing
    }
}
