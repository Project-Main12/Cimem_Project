package com.digitalkalkan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalkalkan.app.domain.model.RiskCategory

/** Renkli risk etiketi: "Tehdit · Yüksek" gibi. */
@Composable
fun RiskChip(category: RiskCategory, modifier: Modifier = Modifier, showLevel: Boolean = true) {
    val text = if (showLevel && category != RiskCategory.NORMAL)
        "${category.label} · ${category.level.label}" else category.label
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(category.color))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
