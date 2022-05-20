package com.woutwerkman.rhinok

import org.gradle.internal.impldep.junit.framework.TestCase.assertFalse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ReadYamlTest {

    private fun readContext(@Language("yaml") yaml: String): RhinoContext =
        RhinoKYamlReader().readRhinoContext(yaml.reader())

    private fun assertIllegalYaml(@Language("yaml") yaml: String) {
        assertThrows<RhinoKCodeGenerator.IllegalYamlFile> {
            RhinoKYamlReader().readRhinoContext(yaml.reader())
        }
    }

    @Test
    fun `empty yaml files are not allowed`() {
        assertIllegalYaml("")
    }

    @Test
    fun `context must contain at least one expression`() {
        assertIllegalYaml("context:")
    }

    @Test
    fun `expression with slots must refer to an existing slot`() {
        assertIllegalYaml(
            """
                context:
                  expressions:
                    Foo:
                      - let's ${'$'}slotThatDoesNotExist:bla
            """.trimIndent(),
        )
    }

    @Test
    fun `expression slot names must be unique`() {
        assertIllegalYaml(
            """
                context:
                  expressions:
                    Foo:
                      - let's ${'$'}Slot:bla ${'$'}OtherType:bla
                  slots:
                    Slot:
                      - foo
                      - bar
                    OtherType:
                      - foo
                      - bar
            """.trimIndent(),
        )
    }

    @Test
    fun `if slot is used multiple times, it is still only created once`() {
        val slot = Slot("Slot", listOf("One", "Two"))
        assertEquals(
            RhinoContext(
                listOf(
                    Intent("Foo", listOf(SlotVariable("bla", slot, true), SlotVariable("bloo", slot, true))),
                    Intent("Bar", listOf(SlotVariable("bla", slot, true))),
                ),
                listOf(slot),
            ),
            readContext(
                """
                    context:
                      expressions:
                        Foo:
                          - do ${'$'}Slot:bla ${'$'}Slot:bloo
                        Bar:
                          - do that ${'$'}Slot:bla
                      slots:
                        Slot:
                          - One
                          - Two
                """.trimIndent(),
            )
        )
    }

    @Test
    fun `if variable doesn't occur in all expressions of an intent, it is optional`() {
        assertFalse(
            readContext(
                """
                    context:
                      expressions:
                        Foo:
                          - do ${'$'}Slot:bla
                          - do all
                      slots:
                        Slot:
                          - One
                          - Two
                """.trimIndent(),
            ).intents.single().variables.single().isRequired
        )
    }

    @Test
    fun `if variable is Kotlin keyword, it gets capitalized`() {
        assertEquals(
            "True",
            readContext(
                """
                    context:
                      expressions:
                        Foo:
                          - do ${'$'}Slot:true
                      slots:
                        Slot:
                          - One
                          - Two
                """.trimIndent(),
            ).intents.single().variables.single().name
        )
    }

    @Test
    fun `if slot element is Kotlin keyword, it gets capitalized`() {
        assertEquals(
            "True",
            readContext(
                """
                    context:
                      expressions:
                        Foo:
                          - do ${'$'}Slot:bla
                      slots:
                        Slot:
                          - true
                          - Two
                """.trimIndent(),
            ).slots.single().elements.minOrNull()
        )
    }
}