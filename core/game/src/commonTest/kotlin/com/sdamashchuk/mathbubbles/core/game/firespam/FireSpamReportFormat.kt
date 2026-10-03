package com.sdamashchuk.mathbubbles.core.game.firespam

fun List<FireSpamRunResult>.toMarkdownTable(title: String): String {
    val header =
        "| seed | survival game time (ms) | survival real time (ms) | level reached | score | " +
            "peak multiplier | presses | end reason |"
    val divider = "|---|---|---|---|---|---|---|---|"
    val rows =
        joinToString("\n") { run ->
            "| ${run.seed} | ${run.survivalGameTimeMs} | ${run.survivalRealTimeMs} | " +
                "${run.levelReached} | ${run.score} | " +
                "x${run.peakMultiplier} | ${run.pressesMade} | ${run.endReason} |"
        }
    return "## $title\n\n$header\n$divider\n$rows\n"
}
