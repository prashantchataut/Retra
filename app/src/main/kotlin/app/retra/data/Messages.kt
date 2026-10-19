package app.retra.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class MessageTone { INFO, SUCCESS, ERROR }

data class AppMessage(val text: String, val tone: MessageTone = MessageTone.INFO, val id: Long = System.nanoTime())

/** App-wide toast bus. */
class Messages {
    private val _flow = MutableSharedFlow<AppMessage>(extraBufferCapacity = 8)
    val flow: SharedFlow<AppMessage> = _flow
    fun show(text: String, tone: MessageTone = MessageTone.INFO) {
        _flow.tryEmit(AppMessage(text, tone))
    }
}

/** A file handed to Retra by another app (VIEW / SEND), waiting to be handled by the UI. */
sealed interface IncomingFile {
    data class Rom(val gameId: String) : IncomingFile
    data class Patch(val path: String, val name: String) : IncomingFile
}

class IncomingFiles {
    private val _pending = MutableStateFlow<IncomingFile?>(null)
    val pending: StateFlow<IncomingFile?> = _pending
    fun offer(file: IncomingFile) { _pending.value = file }
    fun consume() { _pending.value = null }
}
