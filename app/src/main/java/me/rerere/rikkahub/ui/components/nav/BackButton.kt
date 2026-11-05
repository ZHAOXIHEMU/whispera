package me.rerere.rikkahub.ui.components.nav

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.wechat.Glyph

@Composable
fun BackButton(modifier: Modifier = Modifier) {
    val nav = LocalNavController.current
    IconButton(onClick = { nav.popBackStack() }, modifier = modifier.size(46.dp)) {
        Glyph("back", Modifier.size(24.dp), MaterialTheme.colorScheme.onSurface)
    }
}
