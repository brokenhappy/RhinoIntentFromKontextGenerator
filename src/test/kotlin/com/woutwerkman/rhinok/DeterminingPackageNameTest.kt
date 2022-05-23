package com.woutwerkman.rhinok

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeterminingPackageNameTest {
    @Test
    fun `when path is not in source sets, package name is null`() {
        assertNull(
            determinePackageNameBasedOn(
                outputFile = Path.of("foop", "bar", "baz.kt"),
                inside = listOf(Path.of("foo"), Path.of("bar"), Path.of("baz"))
            )
        )
    }

    @Test
    fun `when source set is equal to parent of output, package name is null`() {
        assertNull(determinePackageNameBasedOn(outputFile = Path.of("foo", "bar.kt"), inside = listOf(Path.of("foo"))))
    }

    @Test
    fun `when output file is in a source set, the package name is determined based on the file starting from the source set`() {
        assertEquals(
            "bar.baz",
            determinePackageNameBasedOn(
                outputFile = Path.of("foo", "bar", "baz", "foobs.kt"),
                inside = listOf(Path.of("foo"))
            ),
        )
    }
}