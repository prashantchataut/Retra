package app.retra.core.emulation

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionReducerTest {
    @Test
    fun lifecycleRequiresLoadBeforeStart() {
        val initial = SessionSnapshot()
        assertEquals(initial, SessionReducer.reduce(initial, SessionCommand.Start))
        val loading = SessionReducer.reduce(initial, SessionCommand.BeginLoad("a".repeat(64)))
        val ready = SessionReducer.reduce(loading, SessionCommand.LoadSucceeded)
        assertEquals(SessionPhase.RUNNING, SessionReducer.reduce(ready, SessionCommand.Start).phase)
    }
}
