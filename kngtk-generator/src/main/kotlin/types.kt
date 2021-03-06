import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jdom2.Document
import org.jdom2.Namespace

val introspectionNs = Namespace.getNamespace("http://www.gtk.org/introspection/core/1.0")
val cNs = Namespace.getNamespace("http://www.gtk.org/introspection/c/1.0")
val glibNs = Namespace.getNamespace("http://www.gtk.org/introspection/glib/1.0")

const val NS = "gtk3"
const val LIB = "libgtk3"

const val CINTEROP = "kotlinx.cinterop"

val STRING = String::class.asClassName()

val LIST = ClassName("kotlin.collections", "List")
val CPointer = ClassName(CINTEROP, "CPointer")
val ByteVar = ClassName(CINTEROP, "ByteVar")
val ByteVarPtr = CPointer.parameterizedBy(ByteVar)
val CPointed = ClassName(CINTEROP, "CPointed")
val COpaquePointer = ClassName(CINTEROP, "COpaquePointer")
val CFunction = ClassName(CINTEROP, "CFunction")
val TypeName.ptr get() = CPointer.parameterizedBy(this)
val GtkWidget = ClassName(LIB, "GtkWidget")
val Widget = ClassName(NS, "Widget")
val Container = ClassName(NS, "Container")
val Signal = ClassName(NS, "Signal")
val Signal1 = ClassName(NS, "Signal1")
val Signal2 = ClassName(NS, "Signal2")
val Signal3 = ClassName(NS, "Signal3")
val Signal4 = ClassName(NS, "Signal4")
val Signal5 = ClassName(NS, "Signal5")
val Signal6 = ClassName(NS, "Signal6")
val Dsl = ClassName(NS, "GtkDsl")

fun FileSpec.Builder.convertTypeTo(expr: String, type: TypeName) = when {
    type == STRING -> {
        addImport(CINTEROP, "toKString")
        "$expr?.toKString() ?: \"\""
    }
    type == BOOLEAN -> "$expr·!=·0"
    (type as? ParameterizedTypeName)?.rawType == LIST -> "$expr.toList()"
    (type as? ParameterizedTypeName)?.rawType == CPointer -> {
        addImport(CINTEROP, "reinterpret")
        "$expr!!.reinterpret()"
    }
    else -> expr
}

fun FileSpec.Builder.convertTypeFrom(expr: String, type: TypeName?, convertBoolean: Boolean = true) = when {
    type == BOOLEAN && convertBoolean -> {
        addImport(LIB, "gtk_true", "gtk_false")
        "if·($expr)·gtk_true()·else·gtk_false()"
    }
    type == ByteVarPtr -> {
        addImport(CINTEROP, "toKString")
        "$expr.toKString()"
    }
    (type as? ParameterizedTypeName)?.rawType == LIST -> {
        addImport(CINTEROP, "memScoped")
        addImport(CINTEROP, "cstr")
        addImport(CINTEROP, "ptr")
        addImport(CINTEROP, "toCValues")
        "memScoped { ($expr.map { it.cstr.ptr } + listOf(null)).toCValues() }"
    }
    (type as? ParameterizedTypeName)?.rawType == CPointer -> {
        addImport(CINTEROP, "reinterpret")
        "$expr?.reinterpret()"
    }
    (type as? ClassName)?.packageName == NS -> "$expr.widgetPtr?.reinterpret()"
    else -> expr
}

internal fun allEnums(vararg docs: Document) = docs.flatMap {
    it.rootElement.getChild("namespace", introspectionNs)?.children?.filter { it.name == "enumeration" || it.name == "bitfield" } ?: emptyList()
}.map {
    it.getAttribute("name").value to it.getAttribute("type-name", glibNs).value.toTypeName()
}.toMap()
