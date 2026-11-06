package com.usuallycoding.opensecretsanta.ui.gift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.BadgeGap
import com.usuallycoding.opensecretsanta.ui.form.BadgeSize
import com.usuallycoding.opensecretsanta.ui.form.NumberBadge
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import com.usuallycoding.opensecretsanta.ui.form.TimesLine
import com.usuallycoding.opensecretsanta.ui.form.StatusPill

private val LineGap = 6.dp
private val LineElbow = 8.dp
private val LineWidth = 1.5.dp

/**
 * A question with its answer: the number sits in a small circle at the start of the question, and a thin line runs from the
 * circle down and turns toward the answer, which is indented to line up with the question's text. An unanswered question
 * keeps the line, with [Strings.NOT_ANSWERED] in the answer's place, and with [needsAnswer] also a pill beside the time it was asked, and with [answerIsNew] a "New" pill beside the time of the answer. The line is decoration; the texts keep their own labels
 * for screen readers.
 */
@Composable
internal fun QuestionThread(
    number: Int,
    text: String,
    asked: String,
    answer: String?,
    answered: String?,
    answerChanged: String?,
    modifier: Modifier = Modifier,
    needsAnswer: Boolean = false,
    answerIsNew: Boolean = false,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    var answerTop by remember { mutableFloatStateOf(0f) }
    val indent = BadgeSize + BadgeGap
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind {
                // from the circle's bottom, down its middle to the answer's first line, then across to the answer
                val x = BadgeSize.toPx() / 2
                val y = answerTop + 12.dp.toPx()
                val corner = LineElbow.toPx()
                val path = Path().apply {
                    moveTo(x, BadgeSize.toPx() + 2.dp.toPx())
                    lineTo(x, y - corner)
                    quadraticTo(x, y, x + corner, y)
                    lineTo(indent.toPx() - 4.dp.toPx(), y)
                }
                drawPath(path, lineColor, style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round))
            },
        verticalArrangement = Arrangement.spacedBy(LineGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(BadgeGap)) {
            NumberBadge(number)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ReadOnlyText(text, style = MaterialTheme.typography.titleSmall, label = "${Strings.QUESTION_LABEL} $number")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TimesLine(Strings.asked(asked), modifier = Modifier.weight(1f, fill = false))
                    if (needsAnswer) StatusPill(Strings.UNANSWERED)
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { answerTop = it.positionInParent().y }
                .padding(start = indent),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (answer != null) {
                ReadOnlyText(answer, style = MaterialTheme.typography.bodyMedium, label = Strings.ANSWER)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TimesLine(answered?.let(Strings::answered), answerChanged?.let(Strings::answerChanged), modifier = Modifier.weight(1f, fill = false))
                    // an answer the Santa has not read yet: said in words, as well as by the bar on the card
                    if (answerIsNew) StatusPill(Strings.READ_NEW)
                }
            } else {
                Text(Strings.NOT_ANSWERED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
