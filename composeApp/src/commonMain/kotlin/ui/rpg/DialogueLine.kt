package ui.rpg

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gaime.resources.Res
import gaime.resources.hero_brugg
import gaime.resources.hero_nib
import gaime.resources.hero_vellum
import gaime.resources.npc_portrait_barkeep
import gaime.resources.npc_portrait_guard
import gaime.resources.npc_portrait_merchant
import gaime.resources.npc_portrait_patron
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import rpg.bark.audio.BarkAudioPlayer

data class DialogueLine(val speaker: String, val text: String, val audioPath: String? = null)

private fun speakerPortrait(speaker: String): DrawableResource? = when (speaker.lowercase()) {
    "barkeep"        -> Res.drawable.npc_portrait_barkeep
    "patron"         -> Res.drawable.npc_portrait_patron
    "guard"          -> Res.drawable.npc_portrait_guard
    "merchant",
    "tax collector"  -> Res.drawable.npc_portrait_merchant
    "nib"            -> Res.drawable.hero_nib
    "brugg"          -> Res.drawable.hero_brugg
    "vellum"         -> Res.drawable.hero_vellum
    "guildmaster"    -> Res.drawable.npc_portrait_guard   // official authority figure
    "citizen",
    "devotee"        -> Res.drawable.npc_portrait_patron  // generic townsperson
    else             -> null
}

@Composable
fun DialogueOverlay(
    lines: List<DialogueLine>,
    currentIndex: Int,
    onAdvance: () -> Unit,
    barkAudioPlayer: BarkAudioPlayer? = null
) {
    if (lines.isEmpty()) return
    val line = lines.getOrNull(currentIndex) ?: return

    LaunchedEffect(currentIndex) {
        line.audioPath?.let { path -> barkAudioPlayer?.playRawPath(path) }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = Color(0xF0241E12),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .border(2.dp, Color(0xFF5A4020), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .clickable { onAdvance() }
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                // NPC portrait on the left (only when speaker has one)
                val portrait = if (line.speaker.isNotEmpty()) speakerPortrait(line.speaker) else null
                if (portrait != null) {
                    Image(
                        painter = painterResource(portrait),
                        contentDescription = line.speaker,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF8B6914), RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (line.speaker.isNotEmpty()) {
                        Text(
                            line.speaker,
                            color = Color(0xFFE8C170),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(line.text, color = Color(0xFFF5E9C8), fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onAdvance,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A4A6B)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("▶ Continue", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
