// Date (UTC): 2025-01-01 20:44:37
// Author: lefsilva79

package com.lr.lrobozinsta.service

/**
 * Interface que define as operações do serviço principal de monitoramento
 */
interface IMainService {
    /**
     * Define o valor alvo que será monitorado no app de destino
     * @param value O valor a ser monitorado (sem o símbolo $)
     */
    fun setTargetValue(value: String)

    /**
     * Inicia o serviço de monitoramento
     * Deve ser chamado após setTargetValue
     */
    fun start()

    /**
     * Para o serviço de monitoramento e limpa o valor alvo
     */
    fun stop()

    /**
     * Verifica se o serviço está em execução
     * @return true se o serviço está rodando, false caso contrário
     */
    fun isRunning(): Boolean

    /**
     * Verifica se o app atual é o app alvo
     * @return true se estiver no app alvo, false caso contrário
     */
    fun isInTargetApp(): Boolean
}