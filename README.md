# CopyInto

This is a (experimental) code generation plugin based on KSP that generates a method for annotated classes that copies
a given instance A into a given instance B of the same class by assigning all properties of B the corresponding
values of A. For example

```kotlin
data class NestedDataClass(val x: String)
@CopyInto
data class SimpleDataClass(var a: String, var b: NestedDataClass, val c: String)

@Test
fun `copyInto works`() {
    val source = SimpleDataClass(a = "sourceA", b = NestedDataClass("foo"), c = "sourceC")
    val target = SimpleDataClass(a = "targetA", b = NestedDataClass("bar"), c = "targetC")

    source.copyInto(target)

    assertThat(target.a).isEqualTo(source.a) // has now a new value
    assertThat(target.b).isEqualTo(source.b) // has now a new value
    assertThat(target.c).isEqualTo("targetC") // has old value, property is read only!
}
```

This is similar to a data class copy method that Kotlin automatically implements.

What for do you ask?

In normal code, you would favour immutability, so read only properties everywhere.
This is also where data classes and their copy method already shine.
But copy creates a new instance of a class, hence always makes an allocation and therefore creates
garbage collection pressure.
When you need to avoid such a scenario, as in game development where you don't want your GC
to eat precious ms of your frame time and normally also do object pooling already, you are out of
luck and need to write such a copy method manually. Or use this plugin.
