package com.powercut.editor.core.base

sealed class Resource<out T> {
    object Idle : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()
    data class SuccessWithWarning<out T>(val data: T, val message: String) : Resource<T>()
}
