package com.sdamashchuk.matharcade.core.model

// Field's starting lifeCount and the ceiling FieldMapper's grantLife recovers to - shared so neither
// can drift from the other, and so SessionHelperImpl's finish-spacing invariant (MC-73: spacing
// must survive breakouts - 1 flight times) can reference the real number of lives a session starts
// with instead of a bare 3.
const val INITIAL_LIFE_COUNT = 3
