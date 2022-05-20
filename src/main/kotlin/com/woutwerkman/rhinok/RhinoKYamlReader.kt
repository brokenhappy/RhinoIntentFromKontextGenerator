package com.woutwerkman.rhinok

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver
import java.io.Reader
import java.util.regex.Pattern

class RhinoKYamlReader {
     fun readRhinoContext(yaml: Reader): RhinoContext {
        val parsedYaml: Map<String, *> =
            createSnakeYamlReader().load(yaml) ?: throw RhinoKCodeGenerator.IllegalYamlFile("Illegal yaml syntax")
        val contextNode = YamlNode(parsedYaml).nextMap("context") { "yaml MUST have root node: 'context:'" }
        val slots = contextNode.nextMapOrNull("slots")
            ?.asStringMultiMapEntries()
            ?.map { (name, elements) -> Slot(escapeKotlinKeywords(name), elements.map(::escapeKotlinKeywords)) }?.toList()
            ?: emptyList()

        val intents =
            contextNode.nextMap("expressions") { "you MUST define at least one intent in 'expressions:'" }
                .asStringMultiMapEntries()
                .map { (name, expressions) ->
                    val allVariablesPerExpression = expressions.map { expression ->
                        findAllSlotsIn(expression).mapValues { (variableName, slotType) ->
                            slots.firstOrNull { it.name == slotType }
                                ?: throw RhinoKCodeGenerator.IllegalYamlFile("expression: '$expression' has variable $variableName with slot type $slotType that does not exist")
                        }
                    }
                    val allRequiredVariableNames = allVariablesPerExpression
                        .map { it.keys }
                        .reduce { acc, allVariableNames -> acc.intersect(allVariableNames) }
                    val allVariables = allVariablesPerExpression.reduce { acc, it -> acc + it }
                    Intent(
                        name,
                        allVariables.map { (variableName, slotType) ->
                            SlotVariable(
                                name = escapeKotlinKeywords(variableName),
                                slotType = slotType,
                                isRequired = variableName in allRequiredVariableNames,
                            )
                        },
                    )
                }
        return RhinoContext(intents.toList(), slots)
    }
}

data class Slot(val name: String, val elements: List<String>)
data class SlotVariable(val name: String, val slotType: Slot, val isRequired: Boolean)
data class Intent(val name: String, val variables: List<SlotVariable>)
data class RhinoContext(val intents: List<Intent>, val slots: List<Slot>)

@JvmInline
private value class YamlNode(val yaml: Map<String, *>) {
    fun nextMap(key: String, messageProvider: () -> String): YamlNode =
        nextMapOrNull(key) ?: throw RhinoKCodeGenerator.IllegalYamlFile(messageProvider())

    @Suppress("UNCHECKED_CAST")
    fun nextMapOrNull(key: String): YamlNode? =
        (yaml[key] as? Map<String, *>)?.let(::YamlNode)

    fun asStringMultiMapEntries(): Sequence<Pair<String, List<String>>> =
        yaml.asSequence().map { (key, value) -> key to (value as List<*>).map { it.toString() } }
}

private fun createSnakeYamlReader() = Yaml(
    Constructor(),
    Representer(),
    DumperOptions(),
    LoaderOptions(),
    object : Resolver() {
        override fun addImplicitResolver(tag: Tag?, regexp: Pattern?, first: String?) {
            if (tag == Tag.BOOL)
                super.addImplicitResolver(tag, Pattern.compile(""), "")
            else
                super.addImplicitResolver(tag, regexp, first)
        }
    },
)

private val slotInExpression = "\\\$[A-z\\d]+:[A-z\\d]+".toRegex()
private fun findAllSlotsIn(expression: String): Map<String, String> = mutableMapOf<String, String>().apply {
    slotInExpression.findAll(expression)
        .map { it.value.split(":") }
        .forEach { (slot, variableName) ->
            val safeVariableName = escapeKotlinKeywords(variableName)
            if (safeVariableName in this)
                throw RhinoKCodeGenerator.IllegalYamlFile("expression $expression contains duplicate variable $variableName")
            this[safeVariableName] = slot.drop(1)
        }
}

private val keyWords =
    "as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while"
        .split(' ').toSet()

private fun escapeKotlinKeywords(variableName: String) =
    variableName.takeUnless { it in keyWords } ?: variableName.replaceFirstChar { it.uppercase() }