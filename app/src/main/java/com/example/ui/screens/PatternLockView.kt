package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

@Composable
fun PatternLockView(
    onPatternComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    clearTrigger: Any? = null,
    isError: Boolean = false,
    lineColor: Color = Color(0xFF38BDF8),
    errorColor: Color = Color(0xFFEF4444),
    dotColor: Color = Color(0xFF94A3B8)
) {
    val selectedNodes = remember(clearTrigger) { mutableStateListOf<Int>() }
    var currentTouchPos by remember(clearTrigger) { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .size(280.dp)
            .pointerInput(clearTrigger) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startNode = findHitNode(down.position, size.width.toFloat(), size.height.toFloat())
                    if (startNode != null && !selectedNodes.contains(startNode)) {
                        selectedNodes.add(startNode)
                        onPatternComplete(selectedNodes.joinToString(","))
                    }
                    currentTouchPos = down.position

                    var isDragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            change.consume()
                            isDragging = true
                            currentTouchPos = change.position
                            val node = findHitNode(change.position, size.width.toFloat(), size.height.toFloat())
                            if (node != null && !selectedNodes.contains(node)) {
                                selectedNodes.add(node)
                                onPatternComplete(selectedNodes.joinToString(","))
                            }
                        } else {
                            break
                        }
                    }

                    currentTouchPos = null
                    if (selectedNodes.isNotEmpty()) {
                        onPatternComplete(selectedNodes.joinToString(","))
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / 3f
            val cellHeight = size.height / 3f
            val nodeRadius = 10.dp.toPx()
            val outerRadius = 24.dp.toPx()
            val activeColor = if (isError) errorColor else lineColor

            fun getNodeCenter(index: Int): Offset {
                val row = index / 3
                val col = index % 3
                return Offset(
                    col * cellWidth + cellWidth / 2f,
                    row * cellHeight + cellHeight / 2f
                )
            }

            // Draw connecting lines between consecutive selected nodes
            if (selectedNodes.size > 1) {
                for (i in 0 until selectedNodes.size - 1) {
                    val start = getNodeCenter(selectedNodes[i])
                    val end = getNodeCenter(selectedNodes[i + 1])
                    drawLine(
                        color = activeColor,
                        start = start,
                        end = end,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw connecting line to current drag touch point
            if (selectedNodes.isNotEmpty() && currentTouchPos != null) {
                val lastNodeCenter = getNodeCenter(selectedNodes.last())
                drawLine(
                    color = activeColor.copy(alpha = 0.7f),
                    start = lastNodeCenter,
                    end = currentTouchPos!!,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw 3x3 Grid Dots and Selection Rings
            for (i in 0 until 9) {
                val center = getNodeCenter(i)
                val isSelected = selectedNodes.contains(i)

                if (isSelected) {
                    // Outer ring
                    drawCircle(
                        color = activeColor.copy(alpha = 0.25f),
                        radius = outerRadius,
                        center = center
                    )
                    drawCircle(
                        color = activeColor,
                        radius = nodeRadius + 3.dp.toPx(),
                        center = center
                    )
                } else {
                    // Regular inactive dot
                    drawCircle(
                        color = dotColor,
                        radius = nodeRadius,
                        center = center
                    )
                }
            }
        }
    }
}

private fun findHitNode(touch: Offset, width: Float, height: Float): Int? {
    val cellWidth = width / 3f
    val cellHeight = height / 3f
    val hitRadius = cellWidth * 0.40f

    for (i in 0 until 9) {
        val row = i / 3
        val col = i % 3
        val centerX = col * cellWidth + cellWidth / 2f
        val centerY = row * cellHeight + cellHeight / 2f
        val distance = hypot((touch.x - centerX).toDouble(), (touch.y - centerY).toDouble()).toFloat()
        if (distance <= hitRadius) {
            return i
        }
    }
    return null
}
