package me.rerere.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEmphasisTest {
    @Test
    fun `chat emphasis is upright and keeps explicit colors and bold`() {
        val original = SpanStyle(
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
        )
        val rendered = original.withEmphasis(SpanStyle(fontStyle = FontStyle.Normal, color = Color.Blue))
        assertEquals(FontStyle.Normal, rendered.fontStyle)
        assertEquals(Color.Red, rendered.color)
        assertEquals(FontWeight.Bold, rendered.fontWeight)
        assertEquals(FontStyle.Italic, original.fontStyle)
    }

    @Test
    fun `unspecified style preserves italic for other markdown consumers`() {
        val original = SpanStyle(fontStyle = FontStyle.Italic)
        assertEquals(original, original.withEmphasis(null))
        assertEquals(FontStyle.Italic, original.withEmphasis(SpanStyle(color = Color.Blue)).fontStyle)
        assertEquals(Color.Blue, original.withEmphasis(SpanStyle(color = Color.Blue)).color)
    }
}
