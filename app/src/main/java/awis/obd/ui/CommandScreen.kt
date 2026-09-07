package awis.obd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import awis.obd.command.ObdCommand
import awis.obd.config.ObdConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandLogEntry(
    val timestamp: String,
    val command: String,
    val description: String,
    val result: String,
    val isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandScreen(
    onRunCommand: suspend (ObdCommand) -> String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val allCommands = remember { ObdConfig.getAllCommands() }
    var selectedCommand by remember { mutableStateOf(allCommands.firstOrNull()) }
    var customCommandText by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }

    val logEntries = remember { mutableStateListOf<CommandLogEntry>() }
    val scope = rememberCoroutineScope()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ELM327 Command Terminal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Dropdown selector
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCommand?.let { "${it.desc} (${it.cmd ?: "derived"})" } ?: "Select Command",
                onValueChange = {},
                readOnly = true,
                label = { Text("Predefined OBD/AT Command") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                allCommands.forEach { cmd ->
                    DropdownMenuItem(
                        text = { Text("${cmd.desc} (${cmd.cmd ?: "derived"})") },
                        onClick = {
                            selectedCommand = cmd
                            customCommandText = cmd.cmd ?: ""
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Custom command input field
        OutlinedTextField(
            value = customCommandText,
            onValueChange = { customCommandText = it },
            label = { Text("Command string to send (e.g. 010C, 03, ATZ)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Actions: Run & Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (isRunning) return@Button
                    val cmdToRun = if (customCommandText.isNotBlank()) {
                        ObdCommand(cmd = customCommandText.trim(), desc = "Custom: $customCommandText")
                    } else {
                        selectedCommand?.copyCommand() ?: return@Button
                    }
                    isRunning = true
                    scope.launch {
                        try {
                            val result = onRunCommand(cmdToRun)
                            logEntries.add(
                                0,
                                CommandLogEntry(
                                    timestamp = timeFormat.format(Date()),
                                    command = cmdToRun.cmd ?: "",
                                    description = cmdToRun.desc,
                                    result = result
                                )
                            )
                        } catch (e: Exception) {
                            logEntries.add(
                                0,
                                CommandLogEntry(
                                    timestamp = timeFormat.format(Date()),
                                    command = cmdToRun.cmd ?: "",
                                    description = cmdToRun.desc,
                                    result = "Error: ${e.localizedMessage ?: e.message}",
                                    isError = true
                                )
                            )
                        } finally {
                            isRunning = false
                        }
                    }
                },
                enabled = !isRunning && (isConnected || customCommandText.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .height(18.dp)
                            .padding(end = 8.dp)
                    )
                    Text("Running...")
                } else {
                    Text("Send Command")
                }
            }

            OutlinedButton(
                onClick = { logEntries.clear() },
                enabled = logEntries.isNotEmpty()
            ) {
                Text("Clear")
            }
        }

        // Log Viewer
        Text(
            text = "Console Output",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (logEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isConnected) "Send an OBD-II command to see terminal output" else "Connect adapter to send commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logEntries) { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF161B22), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "> [${entry.timestamp}] ${entry.description} (${entry.command})",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF58A6FF)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.result,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = if (entry.isError) Color(0xFFFF7B72) else Color(0xFF7EE787)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
