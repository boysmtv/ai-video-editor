package com.changecut.core.editor

interface EditorCommand {
    fun execute()
    fun undo()
    fun description(): String
}

class AddClipCommand(
    private val trackManager: TrackManager,
    private val trackIndex: Int,
    private val clip: EditorClip
) : EditorCommand {
    override fun execute() { trackManager.addClip(trackIndex, clip) }
    override fun undo() { trackManager.removeClip(trackIndex, clip.id) }
    override fun description() = "Add clip ${clip.label}"
}

class RemoveClipCommand(
    private val trackManager: TrackManager,
    private val trackIndex: Int,
    private val clip: EditorClip
) : EditorCommand {
    override fun execute() { trackManager.removeClip(trackIndex, clip.id) }
    override fun undo() { trackManager.addClip(trackIndex, clip) }
    override fun description() = "Remove clip ${clip.label}"
}

class SplitClipCommand(
    private val trackManager: TrackManager,
    private val trackIndex: Int,
    private val clipId: String,
    private val splitTimeUs: Long
) : EditorCommand {
    private var newClipId: String? = null
    override fun execute() {
        val result = trackManager.splitClip(trackIndex, clipId, splitTimeUs)
        newClipId = result
    }
    override fun undo() {
        newClipId?.let { trackManager.mergeClips(trackIndex, clipId, it) }
    }
    override fun description() = "Split clip at ${splitTimeUs}us"
}

class ReorderClipCommand(
    private val trackManager: TrackManager,
    private val trackIndex: Int,
    private val clipId: String,
    private val oldIndex: Int,
    private val newIndex: Int
) : EditorCommand {
    override fun execute() { trackManager.moveClip(trackIndex, clipId, newIndex) }
    override fun undo() { trackManager.moveClip(trackIndex, clipId, oldIndex) }
    override fun description() = "Reorder clip"
}

class ModifyClipCommand(
    private val trackManager: TrackManager,
    private val trackIndex: Int,
    private val oldClip: EditorClip,
    private val newClip: EditorClip
) : EditorCommand {
    override fun execute() { trackManager.updateClip(trackIndex, newClip) }
    override fun undo() { trackManager.updateClip(trackIndex, oldClip) }
    override fun description() = "Modify clip ${oldClip.label}"
}

class CompositeCommand(
    private val commands: List<EditorCommand>,
    private val label: String = "Composite action"
) : EditorCommand {
    override fun execute() { commands.forEach { it.execute() } }
    override fun undo() { commands.reversed().forEach { it.undo() } }
    override fun description() = label
}

class SnapshotCommand(
    private val trackManager: TrackManager,
    private val label: String,
    private val action: () -> Unit
) : EditorCommand {
    private val beforeSnapshot = trackManager.snapshot()
    private var afterSnapshot: TimelineSnapshot? = null

    override fun execute() {
        if (afterSnapshot == null) {
            action()
            afterSnapshot = trackManager.snapshot()
        } else {
            trackManager.restoreSnapshot(afterSnapshot!!)
        }
    }

    override fun undo() {
        trackManager.restoreSnapshot(beforeSnapshot)
    }

    override fun description() = label
}
