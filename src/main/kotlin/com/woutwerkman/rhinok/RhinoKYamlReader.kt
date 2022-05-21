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
    class IllegalYamlFile(message: String) : Exception(message)

    fun readRhinoContext(yaml: Reader): RhinoContext {
        val parsedYaml: Map<String, *> = createSnakeYamlReader().load(yaml)
            ?: throw IllegalYamlFile("Illegal yaml syntax")
        val contextNode = YamlNode(parsedYaml).nextMap("context") { "yaml MUST have root node: 'context:'" }
        val slots = contextNode.nextMapOrNull("slots")
            ?.asStringMultiMapEntries()
            ?.map { (name, elements) -> Slot(escapeKotlinKeywords(name), elements.map(::escapeKotlinKeywords)) }
            ?.toList()
            ?: emptyList()

        val intents =
            contextNode.nextMap("expressions") { "you MUST define at least one intent in 'expressions:'" }
                .asStringMultiMapEntries()
                .map { (name, expressions) ->
                    val allVariablesPerExpression = expressions.map { expression ->
                        ExpressionReader(expression).asSequence().toList()
                    }
                    val allRequiredVariableNames = allVariablesPerExpression
                        .map { variables -> variables.filterNot { it.isInOptionalContext }.map { it.name }.toSet() }
                        .reduce { acc, variables -> acc intersect variables }
                    val allVariables = allVariablesPerExpression.flatten().distinctBy { it.name }
                    Intent(
                        name,
                        allVariables.map { (variableName, slotType) ->
                            SlotVariable(
                                name = escapeKotlinKeywords(variableName),
                                type = builtInSlotVariableTypeOf(slotType) ?: SlotVariableType.Custom(
                                    slots.firstOrNull { it.name == slotType } ?: throw IllegalYamlFile(
                                        "Variable $variableName has custom slot type $slotType that does not exist",
                                    ),
                                ),
                                isRequired = variableName in allRequiredVariableNames,
                            )
                        },
                    )
                }
        return RhinoContext(intents.toList(), slots)
    }

    private fun builtInSlotVariableTypeOf(slotType: String) = when (slotType) {
        "pv.Alphabetic",
        "pv.Alphanumeric",
        -> SlotVariableType.Char
        "pv.Percent",
        "pv.SingleDigitInteger",
        "pv.SingleDigitOrdinal",
        "pv.TwoDigitInteger",
        "pv.TwoDigitOrdinal",
        -> SlotVariableType.Integer
        else -> null
    }
}

@JvmInline
private value class YamlNode(val yaml: Map<String, *>) {
    fun nextMap(key: String, messageProvider: () -> String): YamlNode =
        nextMapOrNull(key) ?: throw RhinoKYamlReader.IllegalYamlFile(messageProvider())

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

data class VariableInExpression(val name: String, val type: String, val isInOptionalContext: Boolean)

private class ExpressionReader(private val expression: String): Iterator<VariableInExpression> {
    private var currentIndex: Int = 0
    private var currentOptionalDepth: Int = 0
    private val readNames = mutableSetOf<String>()
    private inline fun skipWhile(predicate: (Char) -> Boolean) {
        while (!atEol() && predicate(expression[currentIndex]))
            currentIndex++
    }

    private fun atEol() = currentIndex == expression.length

    override fun hasNext(): Boolean {
        skipWhile { it !in "([$])" }
        while (true) {
            if (atEol())
                return false
            expression[currentIndex].also { currentChar ->
                when (currentChar) {
                    in "([" -> currentOptionalDepth++
                    in "])" -> currentOptionalDepth--
                    else -> return true
                }
            }
            currentIndex++
            skipWhile { it !in "([$])" }
        }
    }

    override fun next(): VariableInExpression {
        val startIndex = ++currentIndex
        skipWhile { it !in "([$, )]" }
        val variable = expression.substring(startIndex, currentIndex)
        if (':' !in variable) throw RhinoKYamlReader.IllegalYamlFile("Illegal variable in expression: $expression, must be $<type>:<name>")
        val (type, name) = variable.split(':')
        if (!readNames.add(name))
            throw RhinoKYamlReader.IllegalYamlFile("Duplicate variable name $name in expression: $expression")
        return VariableInExpression(name, type, currentOptionalDepth > 0)
    }

}

private val keyWords = "as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while"
    .split(' ').toSet()

private fun escapeKotlinKeywords(variableName: String) =
    variableName.takeUnless { it in keyWords } ?: variableName.replaceFirstChar { it.uppercase() }