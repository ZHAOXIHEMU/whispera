package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun Glyph(name: String, modifier: Modifier = Modifier, tint: Color = Foreground, selected: Boolean = false) {
    Canvas(modifier.semantics { contentDescription = name }) {
        withTransform({ scale(size.width / 48f, size.height / 48f, Offset.Zero) }) {
            val stroke = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun path(block: Path.() -> Unit, color: Color = tint, fill: Boolean = false) {
                drawPath(Path().apply(block), color, style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else stroke)
            }
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(tint, Offset(x,y), Offset(x2,y2), 2.6f, StrokeCap.Round)
            when (name) {
                "more" -> { for (x in listOf(10f,24f,38f)) drawCircle(tint,2.8f,Offset(x,24f)) }
                "wallet" -> {
                    drawRoundRect(tint,Offset(4f,8f),Size(39f,32f),CornerRadius(2f),style=stroke)
                    line(5f,14f,42f,14f)
                    path({moveTo(43f,21f);lineTo(31f,21f);cubicTo(22f,21f,22f,32f,31f,32f);lineTo(43f,32f)})
                    drawCircle(tint,1.8f,Offset(31f,26.5f))
                }
                "chat", "pay" -> {
                    path({ moveTo(24f,5f); cubicTo(11f,5f,3f,13f,3f,23f); cubicTo(3f,30f,7f,35f,13f,38f); lineTo(10f,45f); lineTo(21f,40f); cubicTo(35f,42f,45f,34f,45f,23f); cubicTo(45f,12f,36f,5f,24f,5f); close() }, fill = selected)
                    if (name == "pay") path({ moveTo(11f,23f); lineTo(21f,29f); lineTo(38f,15f) }, if(selected) Green else tint)
                }
                "contacts", "person", "friend" -> {
                    path({ moveTo(23f,3f); cubicTo(12f,3f,13f,18f,17f,23f); lineTo(17f,29f); cubicTo(15f,32f,5f,35f,5f,40f); lineTo(5f,42f); lineTo(42f,42f); lineTo(42f,40f); cubicTo(42f,35f,31f,32f,29f,29f); lineTo(29f,23f); cubicTo(33f,18f,35f,3f,23f,3f); close() }, fill = selected || name == "friend")
                    if (name == "contacts") {
                        drawRect(BottomColor,Offset(34f,6f),Size(14f,24f))
                        line(34f,14f,46f,14f); line(34f,21f,46f,21f); line(34f,28f,43f,28f)
                    }
                    if (name == "friend") { drawCircle(RowColor,9f,Offset(37f,34f)); drawCircle(tint,7f,Offset(37f,34f)); drawLine(RowColor,Offset(33f,34f),Offset(41f,34f),2f);drawLine(RowColor,Offset(37f,30f),Offset(37f,38f),2f) }
                }
                "discover" -> {
                    drawCircle(tint,20f,Offset(24f,24f),style=if(selected) androidx.compose.ui.graphics.drawscope.Fill else stroke)
                    path({ moveTo(32f,14f); lineTo(27f,28f); lineTo(14f,34f); lineTo(20f,20f); close() }, if(selected) BottomColor else tint, selected)
                }
                "search" -> { drawCircle(tint,14f,Offset(20f,20f),style=stroke);line(30f,31f,43f,44f) }
                "plus" -> {drawCircle(tint,21f,Offset(24f,24f),style=stroke);line(24f,11f,24f,37f);line(11f,24f,37f,24f)}
                "back" -> path({moveTo(31f,7f);lineTo(14f,24f);lineTo(31f,41f)})
                "chevron" -> path({moveTo(17f,10f);lineTo(31f,24f);lineTo(17f,38f)})
                "dots" -> { for(x in listOf(10f,24f,38f))drawCircle(tint,2.5f,Offset(x,24f)) }
                "devices" -> {drawRoundRect(tint,Offset(2f,5f),Size(34f,26f),CornerRadius(1f),style=stroke);line(13f,36f,26f,36f);line(20f,31f,20f,36f);drawRoundRect(tint,Offset(28f,16f),Size(15f,27f),CornerRadius(1f),style=stroke)}
                "book" -> {
                    path({moveTo(23f,39f);cubicTo(6f,36f,4f,26f,4f,10f);cubicTo(17f,12f,23f,19f,23f,25f);close()},fill=true)
                    path({moveTo(26f,39f);cubicTo(43f,36f,45f,26f,45f,10f);cubicTo(32f,12f,26f,19f,26f,25f);close()},fill=true)
                }
                "service" -> {path({moveTo(13f,8f);lineTo(24f,24f);lineTo(13f,40f);lineTo(2f,24f);close()},fill=true);path({moveTo(35f,8f);lineTo(46f,24f);lineTo(35f,40f);lineTo(24f,24f);close()},fill=true)}
                "file" -> {path({moveTo(5f,12f);lineTo(19f,12f);lineTo(23f,17f);lineTo(43f,17f);lineTo(43f,38f);lineTo(5f,38f);close()},fill=true);val c=Color(0xff07c160);drawLine(c,Offset(17f,27f),Offset(30f,27f),3f);drawPath(Path().apply{moveTo(26f,21f);lineTo(34f,27f);lineTo(26f,33f);close()},c)}
                "tag" -> {path({moveTo(4f,26f);lineTo(26f,5f);lineTo(43f,5f);lineTo(43f,23f);lineTo(21f,44f);close()},fill=true);drawCircle(Color(0xff2786d7),3f,Offset(34f,14f))}
                "moments" -> {val colors=listOf(0xffee6a39,0xffe7ba22,0xff65b735,0xff07a67a,0xff2488e9,0xff6972cd,0xffcf637d);for(i in 0..6){withTransform({rotate(i*360f/7,Offset(24f,24f))}){path({moveTo(24f,4f);cubicTo(36f,4f,43f,13f,41f,25f);lineTo(20f,15f);close()},Color(colors[i]))}}}
                "qr" -> {for(o in listOf(Offset(4f,4f),Offset(28f,4f),Offset(4f,28f))){drawRect(tint,o,Size(16f,16f),style=stroke);drawRect(tint,o+Offset(5f,5f),Size(6f,6f))};drawRect(tint,Offset(28f,28f),Size(7f,7f));drawRect(tint,Offset(39f,28f),Size(5f,15f));drawRect(tint,Offset(28f,39f),Size(7f,5f))}
                "collect" -> {path({moveTo(24f,3f);lineTo(43f,14f);lineTo(24f,25f);lineTo(5f,14f);close()},Color(0xff2794ef));path({moveTo(5f,14f);lineTo(5f,35f);lineTo(24f,45f);lineTo(24f,25f)},Color(0xffd5a719));path({moveTo(24f,45f);lineTo(43f,35f);lineTo(43f,14f)},Color(0xffdf574d))}
                "photo" -> {drawRect(tint,Offset(3f,8f),Size(42f,33f),style=stroke);path({moveTo(4f,32f);lineTo(17f,20f);lineTo(30f,32f);lineTo(37f,25f);lineTo(44f,31f)})}
                "works" -> {drawRect(tint,Offset(4f,14f),Size(27f,31f),style=stroke);path({moveTo(10f,8f);lineTo(38f,8f);lineTo(38f,37f)});path({moveTo(17f,2f);lineTo(44f,2f);lineTo(44f,30f)})}
                "shop" -> {path({moveTo(9f,7f);lineTo(38f,7f);lineTo(45f,23f);cubicTo(43f,30f,35f,29f,33f,25f);cubicTo(29f,30f,23f,30f,20f,25f);cubicTo(16f,30f,7f,30f,3f,24f);close()});path({moveTo(9f,33f);cubicTo(15f,46f,33f,46f,39f,33f)})}
                "smile" -> {drawCircle(tint,20f,Offset(24f,24f),style=stroke);drawCircle(tint,2.5f,Offset(17f,18f));drawCircle(tint,2.5f,Offset(31f,18f));path({moveTo(12f,27f);lineTo(36f,27f);cubicTo(33f,43f,15f,43f,12f,27f)})}
                "settings" -> {val p=Path();for(i in 0..31){val a=i*Math.PI/16;val r=if(i%4 in 1..2)21f else 16.5f;val x=24f+(kotlin.math.cos(a)*r).toFloat();val y=24f+(kotlin.math.sin(a)*r).toFloat();if(i==0)p.moveTo(x,y)else p.lineTo(x,y)};p.close();drawPath(p,tint,style=stroke);drawCircle(tint,7f,Offset(24f,24f),style=stroke)}
                "mute" -> {path({moveTo(12f,32f);lineTo(15f,27f);lineTo(15f,18f);cubicTo(15f,7f,34f,7f,34f,18f);lineTo(34f,27f);lineTo(37f,32f);close()});line(8f,6f,41f,42f)}
                "voice" -> {drawCircle(tint,20f,Offset(24f,24f),style=stroke);for(x in listOf(17f,24f,31f))drawLine(tint,Offset(x,if(x==24f)13f else 18f),Offset(x,if(x==24f)35f else 30f),2.4f,StrokeCap.Round)}
                else -> {drawRoundRect(tint,Offset(5f,5f),Size(38f,38f),CornerRadius(7f),style=stroke);line(15f,24f,33f,24f)}
            }
        }
    }
}

@Composable
fun BottomTabIcon(index: Int, modifier: Modifier = Modifier, active: Boolean = false) {
    val icon = when (index) {
        0 -> if (active) me.rerere.rikkahub.R.drawable.wx_tab_0 else me.rerere.rikkahub.R.drawable.wx_tab_chat_outline
        1 -> me.rerere.rikkahub.R.drawable.wx_tab_1
        2 -> me.rerere.rikkahub.R.drawable.wx_tab_2
        else -> me.rerere.rikkahub.R.drawable.wx_tab_3
    }
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(icon),
        contentDescription = null,
        modifier = modifier,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(if (active) Green else Foreground),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
    )
}
