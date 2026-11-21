package dev.douglasneto.beatside.ui

import dev.douglasneto.beatside.R
import dev.douglasneto.beatside.menu.BeatSideMenu
import dev.douglasneto.beatside.menu.MenuNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every titleKey in BeatSideMenu must have an explicit R.string mapping (no getIdentifier:
 * release builds shrink resources referenced only by name). The iOS twin is
 * MenuStringsTests.swift, which checks the String Catalog.
 */
class MenuStringsTest {

    private fun allNodes(node: MenuNode): List<MenuNode> =
        listOf(node) + ((node as? MenuNode.Submenu)?.children?.flatMap(::allNodes) ?: emptyList())

    @Test fun `every BeatSideMenu titleKey has a string resource mapping`() {
        val nodes = allNodes(BeatSideMenu.root)
        assertEquals("tree size from data-model.md", 17, nodes.size)
        val missing = nodes.map { it.titleKey }.filter { MenuStrings.resId(it) == null }
        assertTrue("titleKeys with no R.string mapping: $missing", missing.isEmpty())
    }

    @Test fun `each mapping points at the string resource with the same name`() {
        for (node in allNodes(BeatSideMenu.root)) {
            val expected = R.string::class.java.getField(node.titleKey).getInt(null)
            assertEquals("mapping for ${node.titleKey}", expected, MenuStrings.resId(node.titleKey))
        }
    }

    @Test fun `unknown keys have no mapping`() {
        assertEquals(null, MenuStrings.resId("no_such_key"))
    }
}
