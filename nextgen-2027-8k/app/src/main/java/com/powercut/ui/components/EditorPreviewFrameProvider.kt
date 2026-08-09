package com.powercut.ui.components

import com.powercut.ui.editor.EditorViewModel

class EditorPreviewFrameProvider(private val viewModel: EditorViewModel) {
    private var isDisposed = false

    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        // Cancel any coroutines if you have them
        // Release native resources if you have them
        // Unregister observers if you have them
    }
}
