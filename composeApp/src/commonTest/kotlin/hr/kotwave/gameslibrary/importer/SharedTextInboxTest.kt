package hr.kotwave.gameslibrary.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedTextInboxTest {

    @Test
    fun starts_empty() {
        assertNull(SharedTextInbox().pending.value)
    }

    @Test
    fun offer_holds_the_shared_text() {
        val inbox = SharedTextInbox()
        inbox.offer("Persona 5 Royal\nGod of War")
        assertEquals("Persona 5 Royal\nGod of War", inbox.pending.value)
    }

    @Test
    fun offer_replaces_a_prior_unconsumed_share() {
        val inbox = SharedTextInbox()
        inbox.offer("first list")
        inbox.offer("second list")
        assertEquals("second list", inbox.pending.value)
    }

    @Test
    fun clear_empties_the_inbox() {
        val inbox = SharedTextInbox()
        inbox.offer("a list")
        inbox.clear()
        assertNull(inbox.pending.value)
    }
}
