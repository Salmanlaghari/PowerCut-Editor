package com.powercut.ui.components

import com.powercut.ui.editor.EditorViewModel

class EditorPreviewFrameProvider(private val viewModel: EditorViewModel) : PreviewFrameProvider {
    private var isDisposed = false

    override fun pollFrame(): PreviewFrame? {
        return null
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true
    }
}
