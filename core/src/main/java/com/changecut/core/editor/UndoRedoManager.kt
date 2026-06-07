package com.changecut.core.editor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UndoRedoManager @Inject constructor() {
    private val maxHistory: Int = 50
    
    private val undoStack = mutableListOf<EditorCommand>()
    private val redoStack = mutableListOf<EditorCommand>()
    private val _undoHistory = MutableStateFlow<List<String>>(emptyList())
    val undoHistory: StateFlow<List<String>> = _undoHistory.asStateFlow()
    private val _redoHistory = MutableStateFlow<List<String>>(emptyList())
    val redoHistory: StateFlow<List<String>> = _redoHistory.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun execute(command: EditorCommand) {
        command.execute()
        undoStack.add(command)
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
        updateState()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val command = undoStack.removeLast()
        command.undo()
        redoStack.add(command)
        updateState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val command = redoStack.removeLast()
        command.execute()
        undoStack.add(command)
        updateState()
    }

    fun undo(count: Int) {
        repeat(count.coerceAtLeast(1)) {
            if (undoStack.isEmpty()) return
            val command = undoStack.removeLast()
            command.undo()
            redoStack.add(command)
        }
        updateState()
    }

    fun redo(count: Int) {
        repeat(count.coerceAtLeast(1)) {
            if (redoStack.isEmpty()) return
            val command = redoStack.removeLast()
            command.execute()
            undoStack.add(command)
        }
        updateState()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateState()
    }

    private fun updateState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _undoHistory.value = undoStack.asReversed().map { it.description() }
        _redoHistory.value = redoStack.asReversed().map { it.description() }
    }
}
