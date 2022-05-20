package com.woutwerkman.rhinok

class RhinoKCodeGenerator {
    class IllegalYamlFile(message: String) : Exception(message)

    fun generateFrom(context: RhinoContext): CharSequence = buildString {
        appendLine("""
            // This is a generated class
            import ai.picovoice.rhino.RhinoInference
            
            sealed interface Intent {
                object NotUnderstood : Intent
        """.trimIndent())
        context.intents.joinTo(this, "\n") { intent -> generateDataClassDeclarationCode(intent) }
        appendLine("""
           |    companion object {
           |        fun from(inference: RhinoInference): Intent {
           |            if (!inference.isUnderstood)
           |                return NotUnderstood
           |            class IllegalInferenceException(reason: String): Exception(""${'"'}
           |                Illegal inference, reason: '${'$'}reason'. Based on Rhino's inferred: ${'$'}inference
           |                Please make sure that the .rhn file is created from the same yml as the Intents generated in Kotlin.
           |                If you are sure the set-up is correct, please file an issue
           |            ""${'"'}.trimIndent())
           |        
           |            val slots = inference.slots
           |            return when (inference.intent) {
        """.trimMargin())
        context.intents.joinTo(this, "\n") { intent -> generateInstantiationCodeInWhenBranch(intent) }
        appendLine("""
                           else -> throw IllegalInferenceException("Intent ${'$'}{inference.intent} is not a legal intent kind")
                       }
                   }
               }
           }
        """.trimMargin())
        context.slots.joinTo(this, "\n") { slot -> generateSlotEnum(slot) }
        appendLine(
            """
            private inline fun <reified T: Enum<T>> Map<String, String>.getSlot(key: String, exception: (String) -> Throwable, ): T? =
                this[key]?.let {
                    kotlin.runCatching { java.lang.Enum.valueOf(T::class.java, it) }.getOrNull()
                        ?: throw exception("Slot ${'$'}{T::class.simpleName} does not have element ${'$'}it given for variable ${'$'}key")
                }
            
            private inline fun <reified T: Enum<T>> Map<String, String>.getRequiredSlot(key: String, exception: (String) -> Throwable, ): T =
                getSlot<T>(key, exception) ?: throw exception("Variable ${'$'}key is required by all expressions, but is not present")
        """.trimIndent()
        )
    }

    private fun generateSlotEnum(slot: Slot): String =
        slot.elements.joinToString(prefix = "enum class ${slot.name} {", postfix = "}")

    private fun generateInstantiationCodeInWhenBranch(intent: Intent): String =
        intent.variables.joinToString(prefix = "                \"${intent.name}\" -> ${intent.name}(", postfix = ")") {
            "slots.${if (it.isRequired) "getRequiredSlot" else "getSlot"}(\"${it.name}\", ::IllegalInferenceException)"
        }

    private fun generateDataClassDeclarationCode(intent: Intent): String = when {
        intent.variables.isEmpty() -> "    object ${intent.name} : Intent"
        else -> intent.variables.joinToString(prefix = "    data class (" + intent.name, postfix = ") : Intent") {
            "val " + it.name + ": " + it.slotType.name + if (it.isRequired) "" else "?"
        }
    }
}

