package com.secondmemory.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.secondmemory.app.ui.theme.Forest

private data class Slide(val kicker: String, val title: String, val body: String)

private val SLIDES = listOf(
    Slide(
        "Share",
        "Send anything into Second Memory.",
        "A link, a photo, a PDF, a video, a contact, a place. Share from any app. It takes a couple of seconds.",
    ),
    Slide(
        "Pin",
        "It stays in your notification shade.",
        "Pull down the shade and it is there. No folders. No schedule. No hoping you remember to open the app.",
    ),
    Slide(
        "Unpin",
        "It stays until you say otherwise.",
        "Done, or unpin. That is the whole loop. Better than a pile of reminders you have to dismiss.",
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val slide = SLIDES[step]
    val last = step == SLIDES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text("Second Memory", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        OnboardingArt(step)
        Text(
            slide.kicker.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Forest,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            slide.title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            slide.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { if (last) onFinished() else step += 1 },
                modifier = Modifier.heightIn(min = 52.dp),
            ) {
                Text(if (last) "Start" else "Continue")
            }
            TextButton(
                onClick = onFinished,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .heightIn(min = 52.dp),
            ) {
                Text("Skip")
            }
        }
        Row(
            modifier = Modifier.padding(top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SLIDES.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .size(if (i == step) 18.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }
        Spacer(Modifier.weight(0.4f))
    }
}

@Composable
private fun OnboardingArt(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (i == step) 80.dp else 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (i == step) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                    ),
            )
        }
    }
}
