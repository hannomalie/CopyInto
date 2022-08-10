import de.hanno.copyinto.api.CopyInto

data class NestedDataClass(val x: String)
@CopyInto
data class SimpleDataClass(var a: String, var b: NestedDataClass, val c: String)