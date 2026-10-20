package com.vastu.compass.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun ItemIcon(
    iconUrl: String?,
    fallbackKey: String,
    contentDescription: String?,
    modifier: Modifier = Modifier.size(36.dp),
    tint: Color = Color(0xFF8D6E63)
) {
    if (!iconUrl.isNullOrBlank()) {
        KamelImage(
            resource = asyncPainterResource(data = iconUrl),
            contentDescription = contentDescription,
            modifier = modifier,
            onLoading = {
                Icon(
                    imageVector = IconMapper.get(fallbackKey),
                    contentDescription = null,
                    tint = tint.copy(alpha = 0.35f),
                    modifier = modifier
                )
            },
            onFailure = {
                Icon(
                    imageVector = IconMapper.get(fallbackKey),
                    contentDescription = null,
                    tint = tint,
                    modifier = modifier
                )
            }
        )
    } else {
        Icon(
            imageVector = IconMapper.get(fallbackKey),
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier
        )
    }
}
