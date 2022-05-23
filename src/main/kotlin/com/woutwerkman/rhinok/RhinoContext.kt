package com.woutwerkman.rhinok

data class Slot(val name: String, val elements: List<String>) {
    internal fun generateSlotEnum(): String = elements.joinToString(prefix = "enum class $name { ", postfix = " }")
}


private fun String.orEmptyIf(condition: Boolean): String = if (condition) "" else this

data class SlotVariable(internal val name: String, internal val type: SlotVariableType, internal val isRequired: Boolean) {

    internal fun generateValDeclaration(): String = "val $name: ${type.dataType}${"?".orEmptyIf(isRequired)}"
    internal fun generateInstantiatingCall(): String =
        "slots.${type.functionCall}(\"$name\", error)${".require(\"$name\", error)".orEmptyIf(!isRequired)}"
}

sealed interface SlotVariableType {
    val dataType: String
    val functionCall: String
    data class Custom(val slot: Slot) : SlotVariableType {
        override val dataType: String get() = slot.name
        override val functionCall: String get() = "getSlot<$dataType>"
    }
    object Integer : SlotVariableType {
        override val dataType: String get() = "Int"
        override val functionCall: String get() = "getInt"
    }
    object Char : SlotVariableType {
        override val dataType: String get() = "Char"
        override val functionCall: String get() = "getChar"
    }
}

data class Intent(val name: String, val variables: List<SlotVariable>) {
    internal fun generateInstantiationCodeInWhenBranch(): String = generateWhenBranch() + generateInstantiation()

    internal fun generateInstantiation() = when {
        variables.isEmpty() -> name
        else -> variables.joinToString(prefix = "$name(", postfix = ")") { it.generateInstantiatingCall() }
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