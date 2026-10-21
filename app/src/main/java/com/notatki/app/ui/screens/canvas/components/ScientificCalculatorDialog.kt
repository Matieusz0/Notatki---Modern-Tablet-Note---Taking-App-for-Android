package com.notatki.app.ui.screens.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.notatki.app.util.ScientificEvaluator

@Composable
fun ScientificCalculatorDialog(
    onDismiss: () -> Unit
) {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isDegreeMode by remember { mutableStateOf(true) }

    fun calculate() {
        if (expression.isBlank()) {
            resultText = ""
            return
        }
        val res = ScientificEvaluator.evaluate(expression, isDegreeMode)
        res.onSuccess {
            resultText = ScientificEvaluator.formatResult(it)
        }.onFailure {
            resultText = "Błąd"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(380.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Nagłówek
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kalkulator Naukowy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Wyświetlacz
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (expression.isEmpty()) "0" else expression,
                            fontSize = 20.sp,
                            maxLines = 2,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (resultText.isEmpty()) " " else "= $resultText",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Klawisze kalkulatora
                val buttonGrid = listOf(
                    listOf("DEG/RAD", "sin", "cos", "tan", "^"),
                    listOf("√", "ln", "log", "(", ")"),
                    listOf("7", "8", "9", "÷", "C"),
                    listOf("4", "5", "6", "×", "DEL"),
                    listOf("1", "2", "3", "-", "π"),
                    listOf("0", ".", "%", "+", "=")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    buttonGrid.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { label ->
                                val isAction = label in listOf("C", "DEL", "=")
                                val isFunction = label in listOf("DEG/RAD", "sin", "cos", "tan", "^", "√", "ln", "log", "(", ")", "%", "π")
                                val isOperator = label in listOf("÷", "×", "-", "+")

                                val btnColor = when {
                                    label == "=" -> MaterialTheme.colorScheme.primary
                                    isAction -> MaterialTheme.colorScheme.errorContainer
                                    isOperator -> MaterialTheme.colorScheme.primaryContainer
                                    isFunction -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }

                                val textColor = when {
                                    label == "=" -> MaterialTheme.colorScheme.onPrimary
                                    isAction -> MaterialTheme.colorScheme.onErrorContainer
                                    isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isFunction -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Button(
                                    onClick = {
                                        when (label) {
                                            "C" -> {
                                                expression = ""
                                                resultText = ""
                                            }
                                            "DEL" -> {
                                                if (expression.isNotEmpty()) {
                                                    expression = expression.dropLast(1)
                                                    calculate()
                                                }
                                            }
                                            "=" -> calculate()
                                            "DEG/RAD" -> {
                                                isDegreeMode = !isDegreeMode
                                                calculate()
                                            }
                                            "sin", "cos", "tan", "ln", "log" -> {
                                                expression += "$label("
                                            }
                                            "√" -> expression += "√("
                                            else -> {
                                                expression += label
                                                calculate()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = btnColor,
                                        contentColor = textColor
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (label == "DEG/RAD") (if (isDegreeMode) "DEG" else "RAD") else label,
                                        fontSize = if (label.length > 3) 11.sp else 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
