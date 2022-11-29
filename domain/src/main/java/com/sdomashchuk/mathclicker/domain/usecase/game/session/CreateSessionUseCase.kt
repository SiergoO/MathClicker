package com.sdomashchuk.mathclicker.domain.usecase.game.session

import com.sdomashchuk.mathclicker.domain.model.game.session.GameSession
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.domain.usecase.Param
import com.sdomashchuk.mathclicker.domain.usecase.UseCase

typealias CreateSessionUseCaseResult = Result<GameSession>

class CreateSessionUseCase(
    private val gameRepository: GameRepository
) : UseCase<CreateSessionUseCaseParam, CreateSessionUseCaseResult> {

    override suspend fun execute(param: CreateSessionUseCaseParam): CreateSessionUseCaseResult {
        return try {
           val gameSession = gameRepository.getUnfinishedSession().let {
                it ?: with(gameRepository) {
                    val newGameFieldId = getSessionCount() + 1
                    val gameField = initGameField(newGameFieldId)
                    deleteTargetParams()
                    initTargetParams(gameField)
                    getUnfinishedSession()!!
                }
            }
            CreateSessionUseCaseResult.success(gameSession)
        } catch (t: Throwable) {
            CreateSessionUseCaseResult.failure(t)
        }
    }
}

object CreateSessionUseCaseParam : Param