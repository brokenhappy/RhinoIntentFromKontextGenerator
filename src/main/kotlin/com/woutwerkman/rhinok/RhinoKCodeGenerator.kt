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
        context.intents.joinTo(this, "\n") { "    " + it.generateTypeDeclarationCode() }
        appendLine("""
           |    companion object {
           |        fun from(inference: RhinoInference): Intent {
           |            if (!inference.isUnderstood)
           |                return NotUnderstood
           |            class IllegalInferenceException(reason: String): Exception(""${'"'}
           |                Illegal inference, reason: '${'$'}reason'. Based on Rhino's inferred: ${'$'}inference
           |                Please make sure that the .rhn file is created from the same yml as the Intents generated in Kotlin.
           |                If you are sure the set-up is correct, please report the issue 
           |                https://github.com/brokenhappy/RhinoIntentFromKontextGenerator/issues
           |            ""${'"'}.trimIndent())
           |        
           |            val slots = inference.slots
           |            return when (inference.intent) {
        """.trimMargin())
        context.intents.joinTo(this, "\n") { "                " + it.generateInstantiationCodeInWhenBranch() }
        appendLine("""
                           else -> throw IllegalInferenceException("Intent ${'$'}{inference.intent} is not a legal intent kind")
                       }
                   }
               }
           }
        """.trimMargin())
        context.slots.joinTo(this, "\n") { slot -> slot.generateSlotEnum() }
        appendLine("""
            private inline fun <reified T: Enum<T>> Map<String, String>.getSlot(key: String, exception: (String) -> Throwable): T? =
                this[key]?.let {
                    kotlin.runCatching { java.lang.Enum.valueOf(T::class.java, it) }.getOrNull()
                        ?: throw exception("Slot ${'$'}{T::class.simpleName} does not have element ${'$'}it given for variable ${'$'}key")
                }
            
            private inline fun <reified T: Enum<T>> Map<String, String>.getRequiredSlot(key: String, exception: (String) -> Throwable): T =
                getSlot<T>(key, exception) ?: throw exception("Variable ${'$'}key is required by all expressions, but is not present")
        """.trimIndent())
    }
}

