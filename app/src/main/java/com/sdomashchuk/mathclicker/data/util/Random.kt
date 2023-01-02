package com.sdomashchuk.mathclicker.data.util

import kotlin.random.Random

fun IntRange.random() = this.random(Random(System.nanoTime()))

fun <T> Collection<T>.random(): T = this.random(Random(System.nanoTime()))

fun <T> Array<out T>.random(): T = this.random(Random(System.nanoTime()))