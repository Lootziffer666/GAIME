package ui.rpg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DialogueLine(val speaker: String, val text: String)

@Composable
fun DialogueOverlay(lines: List<DialogueLine>, currentIndex: Int, onAdvance: () -> Unit) {
    if (lines.isEmpty()) return
    val line = lines.getOrNull(currentIndex) ?: return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = Color(0xF0241E12),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
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
