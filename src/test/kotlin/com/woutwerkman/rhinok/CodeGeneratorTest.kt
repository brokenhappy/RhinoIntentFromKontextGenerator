package com.woutwerkman.rhinok

import com.woutwerkman.rhinok.SlotVariableType.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CodeGeneratorTest {
    @Test
    fun `intent with no variables becomes a object class`() {
        assertThatDeclarationOf(Intent("Foo", listOf()), generates = "object Foo : Intent")
    }

    @Test
    fun `intent with variables becomes an object class`() {
        assertThatDeclarationOf(
            Intent("Foo", listOf(SlotVariable("bar", Custom(Slot("Bar", listOf())), true))),
            generates = "data class Foo(val bar: Bar) : Intent",
        )
    }

    @Test
    fun `intent with no variables is referred instead of instantiated`() {
        assertThatInstantiationOf(
            Intent("Foo", listOf()),
            generates = """Foo""",
        )
    }

    @Test
    fun `when branch is generated based on intent name`() {
        assertEquals(
            "\"Foo\" -> ",
            Intent("Foo", listOf()).generateWhenBranch(),
        )
    }

    @Test
    fun `intent with variables is instantiated`() {
        assertThatInstantiationOf(
            Intent("Foo", listOf(SlotVariable("bar", Custom(Slot("Bar", listOf())), true))),
            generates = """Foo(slots.getSlot("bar", error).require("bar", error))"""
        )
    }

    private fun assertThatDeclarationOf(intent: Intent, @Language("kts") generates: String) {
        assertEquals(generates, intent.generateTypeDeclarationCode())
    }

    private fun assertThatInstantiationOf(intent: Intent, @Language("kts") generates: String) {
        assertEquals(generates, intent.generateInstantiation())
    }

}