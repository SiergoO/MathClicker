package com.sdamashchuk.matharcade.core.ui.theme

import androidx.compose.ui.graphics.Color

// MC-84: every value here was sampled from the reference field, not picked by eye. The bubble
// layers were fitted numerically against it - mean absolute channel error 3.4% for the ready
// state, 3.3% for the idle one. See .claude/specs/math-bubbles-ds.html for the derivation.

// The water column: light at the top, a little deeper at the bottom, and the step between them
// deliberately small. An obvious gradient draws the eye to the background, which is the one place
// in this game the eye should never go.
val WaterSurface = Color(0xFF2C5D74)
val WaterDeep = Color(0xFF234C61)

// Drifting motes and bubbles in the water. Barely above the background on purpose - they carry the
// sense of being submerged, not information.
val WaterMote = Color(0xFF9FD8E8)

// The only fully opaque colour in the world: the life stripes and the operation button.
val Accent = Color(0xFF47B9E2)
val AccentSoft = Color(0xFF8ED6EE)
val AccentDeep = Color(0xFF1E6F92)

val BubbleFillReady = Color(0xFF38C6FD)
val BubbleFillIdle = Color(0xFFAECAD2)
val BubbleRimReady = Color(0xFFB6EDF4)
val BubbleRimIdle = Color(0xFF6B8695)

val Ink = Color(0xFFEDFBFF)
val InkHud = Color(0xFFB5BDC3)
val InkDim = Color(0xFF8B989E)

val Success = Color(0xFF6FE3B4)

// Warm on purpose. Readiness is carried by saturation here (cyan against grey), so temperature is
// a free channel - a warning can be amber without colliding with the one meaning the bubble's own
// colour already has.
val Warning = Color(0xFFFFA45C)

// A dark scrim, not the old 40% white: over water, a light scrim washes the field out and leaves
// overlay text with nothing to sit against.
val Scrim = Color(0xA6071620)
