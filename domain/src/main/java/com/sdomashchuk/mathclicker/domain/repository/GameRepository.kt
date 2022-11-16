package com.sdomashchuk.mathclicker.domain.repository

import com.sdomashchuk.mathclicker.domain.model.game.session.Session
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParam

interface GameRepository {
    suspend fun getSession(): Session
    suspend fun getTargetParams(level: Int): List<TargetParam>
    suspend fun updateSession(session: Session)
    suspend fun updateScore(score: Int)
}