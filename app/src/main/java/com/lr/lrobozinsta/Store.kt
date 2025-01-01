// Date (UTC): 2025-01-01 21:20:49
// Author: lefsilva79

package com.lr.lrobozinsta.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Armazena e gerencia os itens encontrados pelo serviço
 * Implementa um cache para evitar duplicatas em um intervalo de tempo
 */
// Date (UTC): 2025-01-01 21:43:32
// Author: lefsilva79

class Store private constructor() {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    private val cache = ConcurrentHashMap<String, Long>()

    companion object {
        private const val TAG = "Store"
        private const val CACHE_TIMEOUT = 5000L // 5 segundos

        @Volatile
        private var instance: Store? = null

        fun get(): Store {
            return instance ?: synchronized(this) {
                instance ?: Store().also { instance = it }
            }
        }
    }

    fun add(item: Item) {
        Log.d(TAG, "\n----------------------------------------")
        Log.d(TAG, "Store.add() - Iniciando adição de item")
        Log.d(TAG, "Store.add() - Item recebido: ${item.txt}")

        val currentTime = System.currentTimeMillis()
        val lastTime = cache[item.txt]

        Log.d(TAG, "Store.add() - Estado atual do Store:")
        Log.d(TAG, "   • Items atuais: ${_items.value.size}")
        Log.d(TAG, "   • Cache atual: ${cache.size}")
        Log.d(TAG, "   • Último acesso do item: ${if (lastTime != null) "há ${(currentTime - lastTime)}ms" else "nunca"}")

        if (lastTime == null) {
            Log.d(TAG, "Store.add() - NOVO ITEM: ${item.txt}")
            cache[item.txt] = currentTime
            _items.value = _items.value + item
            Log.d(TAG, "Store.add() - Item adicionado com sucesso")
            Log.d(TAG, "Store.add() - Novo total de items: ${_items.value.size}")
        } else if (currentTime - lastTime > CACHE_TIMEOUT) {
            Log.d(TAG, "Store.add() - ITEM EXPIRADO: ${item.txt}")
            Log.d(TAG, "Store.add() - Tempo desde último acesso: ${currentTime - lastTime}ms")
            cache[item.txt] = currentTime
            _items.value = _items.value + item
            Log.d(TAG, "Store.add() - Item atualizado com sucesso")
            Log.d(TAG, "Store.add() - Novo total de items: ${_items.value.size}")
        } else {
            Log.d(TAG, "Store.add() - ITEM IGNORADO: ${item.txt}")
            Log.d(TAG, "Store.add() - Tempo desde último acesso: ${currentTime - lastTime}ms")
            Log.d(TAG, "Store.add() - Ainda dentro do timeout de ${CACHE_TIMEOUT}ms")
        }

        Log.d(TAG, "----------------------------------------\n")
    }

    fun clear() {
        Log.d(TAG, "\n----------------------------------------")
        Log.d(TAG, "Store.clear() - Iniciando limpeza")
        Log.d(TAG, "Store.clear() - Estado atual:")
        Log.d(TAG, "   • Items: ${_items.value.size}")
        Log.d(TAG, "   • Cache: ${cache.size}")

        _items.value = emptyList()
        cache.clear()

        Log.d(TAG, "Store.clear() - Limpeza concluída")
        Log.d(TAG, "Store.clear() - Estado final:")
        Log.d(TAG, "   • Items: ${_items.value.size}")
        Log.d(TAG, "   • Cache: ${cache.size}")
        Log.d(TAG, "----------------------------------------\n")
    }
}