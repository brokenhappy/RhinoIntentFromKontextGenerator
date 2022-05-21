package com.woutwerkman.rhinok

data class Slot(val name: String, val elements: List<String>) {
    internal fun generateSlotEnum(): String = elements.joinToString(prefix = "enum class $name {", postfix = "}")
}

data class SlotVariable(val name: String, val slotType: Slot, val isRequired: Boolean) {
    internal fun generateValDeclaration() = "val $name: ${slotType.name}${if (isRequired) "" else "?"}"
    internal fun generateInstantiatingCall() =
        "${if (isRequired) "getRequiredSlot" else "getSlot"}(\"$name\", ::IllegalInferenceException)"
}

data class Intent(val name: String, val variables: List<SlotVariable>) {
    internal fun generateInstantiationCodeInWhenBranch(): String = generateWhenBranch() + generateInstantiation()

    internal fun generateInstantiation() = when {
        variables.isEmpty() -> name
        else -> variables.joinToString(prefix = "$name(", postfix = ")") {
            "slots.${it.generateInstantiatingCall()}"
        }
    }

    internal fun generateWhenBranch() = "\"$name\" -> "

    internal fun generateTypeDeclarationCode(): String = when {
        variables.isEmpty() -> "object $name : Intent"
        else -> variables.joinToString(prefix = "data class $name(", postfix = ") : Intent") {
            it.generateValDeclaration()
        }
    }
}

data class RhinoContext(val intents: List<Intent>, val slots: List<Slot>)