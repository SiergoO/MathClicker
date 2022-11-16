package com.sdomashchuk.mathclicker.domain.usecase

interface UseCase<P : Param, R : Result<Any>> {

    suspend fun execute(param: P): R
}