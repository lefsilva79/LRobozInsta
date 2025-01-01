// app/src/main/java/com/lr/lrobozinsta/data/Store.kt
package com.lr.lrobozinsta.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

class Store private constructor() {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    private val cache = ConcurrentHashMap<String, Long>()

    fun add(item: Item) {
        if (!cache.containsKey(item.txt) ||
            System.currentTimeMillis() - cache[item.txt]!! > 5000) {
            cache[item.txt] = item.time
            _items.value = _items.value + item
        }
    }

    fun clear() {
        _items.value = emptyList()
        cache.clear()
    }

    companion object {
        @Volatile
        private var instance: Store? = null

        fun get(): Store {
            return instance ?: synchronized(this) {
                instance ?: Store().also { instance = it }
            }
        }
    }
}