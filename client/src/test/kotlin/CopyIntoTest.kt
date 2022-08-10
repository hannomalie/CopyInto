import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CopyIntoTest {
    @Test
    fun `copyInto works`() {
        val source = SimpleDataClass(a = "sourceA", b = NestedDataClass("foo"), c = "sourceC")
        val target = SimpleDataClass(a = "targetA", b = NestedDataClass("bar"), c = "targetC")

        source.copyInto(target)

        assertThat(target.a).isEqualTo(source.a)
        assertThat(target.b).isEqualTo(source.b)
        assertThat(target.c).isEqualTo("targetC")
    }
}