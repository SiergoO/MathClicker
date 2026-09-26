package com.sdamashchuk.matharcade.core.ui.theme

import androidx.compose.ui.graphics.Color

// MC-84: every value here was sampled from the reference field, not picked by eye. The bubble
// layers were fitted numerically against it - mean absolute channel error 3.4% for the ready
// state, 3.3% for the idle one. See .claude/specs/math-bubbles-ds.html for the derivation.

// The water column. Three stops, vertical only - the reference does not vary by a single unit
// horizontally. It is deliberately not a monotonic darkening: the brightest band sits at 62% of
// the height and reads as a shaft of light from above, which is what gives the water its volume.
val WaterSurface = Color(0xFF10202D)
val WaterShaft = Color(0xFF285066)
val WaterDeep = Color(0xFF17303E)

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
