package dev.douglasneto.beatside

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * SC-005 / FR-029: every translatable string has a Brazilian Portuguese value with the same
 * format placeholders, and pt-BR has no stray keys. Reads the resource files directly (unit tests
 * run from the app module directory).
 */
class TranslationsTest {

    private fun strings(path: String): Map<String, Pair<String, Boolean>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val e = nodes.item(i) as org.w3c.dom.Element
            e.getAttribute("name") to (e.textContent to (e.getAttribute("translatable") != "false"))
        }
    }

    private val english = strings("src/main/res/values/strings.xml")
    private val portuguese = strings("src/main/res/values-pt-rBR/strings.xml")
    private val translatable = english.filterValues { it.second }.keys

    private fun placeholders(text: String) = Regex("""%\d+\$[sd]|%[sd]""").findAll(text).map { it.value }.sorted().toList()

    @Test fun `every translatable key has a pt-BR value`() {
        val missing = translatable.filter { portuguese[it]?.first.isNullOrBlank() }
        assertTrue("keys with no pt-BR value: $missing", missing.isEmpty())
    }

    @Test fun `pt-BR has no keys that English lacks or marks untranslatable`() {
        val extra = portuguese.keys - translatable
        assertTrue("pt-BR keys not translatable in English: $extra", extra.isEmpty())
    }

    @Test fun `pt-BR keeps the same format placeholders`() {
        for (key in translatable.filter { it in portuguese }) {
            assertEquals("placeholders of $key", placeholders(english.getValue(key).first), placeholders(portuguese.getValue(key).first))
        }
    }
}
