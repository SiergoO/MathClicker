package com.sdomashchuk.mathclicker.domain.usecase.game.session

import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.domain.usecase.Param
import com.sdomashchuk.mathclicker.domain.usecase.UseCase

typealias UpdateLevelUseCaseResult = Result<Unit>

class UpdateLevelUseCase(
    private val gameRepository: GameRepository
) : UseCase<UpdateLevelUseCaseParam, UpdateLevelUseCaseResult> {

    override suspend fun execute(param: UpdateLevelUseCaseParam): UpdateLevelUseCaseResult {
        return try {
            gameRepository.updateLevel(level = param.level)
            val gameField = gameRepository.getUnfinishedSession()!!.gameField
            gameRepository.refreshTargetParams(gameField)
            UpdateLevelUseCaseResult.success(Unit)
        } catch (t: Throwable) {
            UpdateLevelUseCaseResult.failure(t)
        }
    }
}

data class UpdateLevelUseCaseParam(val level: Int) : Param