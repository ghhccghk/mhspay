package com.mihuashi.paybyfinger.tools

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class DialogViewModelStoreOwner : ViewModelStoreOwner {
    private val store = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = store

    fun clear() {
        store.clear()
    }
}