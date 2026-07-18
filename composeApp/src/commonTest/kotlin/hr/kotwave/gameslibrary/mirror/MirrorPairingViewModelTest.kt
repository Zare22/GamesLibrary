package hr.kotwave.gameslibrary.mirror

import hr.kotwave.gameslibrary.data.SyncDismissal
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_HOST_ENDPOINT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_NEEDS_REPAIR_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_PAIRED_AT_KEY
import hr.kotwave.gameslibrary.secure.MIRROR_CLIENT_TOKEN_KEY
import hr.kotwave.gameslibrary.secure.SecureStorage
import hr.kotwave.gameslibrary.transfer.LibraryExport
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class MirrorPairingViewModelTest {

    private lateinit var storage: FakeStorage

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        storage = FakeStorage()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        fetchCert: suspend (String, Int) -> String = { _, _ -> FINGERPRINT },
    ): MirrorPairingViewModel =
        MirrorPairingViewModel(MirrorSession(NoopStore(), storage), storage, fetchCert)

    @Test
    fun garbageQrShowsNotPairingCode() = runTest {
        val vm = viewModel()

        vm.onQrScanned("https://example.com/some-other-qr")

        assertEquals(PairingError.NotPairingCode, vm.error)
        assertEquals(PairingStep.Scan, vm.step)
    }

    @Test
    fun foreignVersionQrShowsVersionMismatch() = runTest {
        val vm = viewModel()

        vm.onQrScanned("""{"v":99,"ip":"1.2.3.4","port":56789,"secret":"123456","fp":"$FINGERPRINT"}""")

        assertEquals(PairingError.VersionMismatch, vm.error)
    }

    @Test
    fun scansAreIgnoredWhileAnErrorShows() = runTest {
        val vm = viewModel()
        vm.onQrScanned("garbage")

        vm.onQrScanned("more garbage")

        assertEquals(PairingError.NotPairingCode, vm.error)
    }

    @Test
    fun typedConnectFetchesTheCertAndAdvancesToVerify() = runTest {
        val vm = viewModel()
        vm.startTyped()

        vm.onTypedConnect("192.168.1.24", 56789, "743291")

        val verify = assertIs<PairingStep.Verify>(vm.step)
        assertEquals(FINGERPRINT, verify.fingerprint)
        assertEquals("AB12 · CD34 · EF56", verify.verifyCode)
        assertNull(vm.error)
    }

    @Test
    fun unreachableHostOnTypedConnectShowsE3AndStaysTyped() = runTest {
        val vm = viewModel(fetchCert = { _, _ -> error("refused") })
        vm.startTyped()

        vm.onTypedConnect("192.168.1.24", 56789, "743291")

        assertEquals(PairingError.Unreachable("192.168.1.24:56789"), vm.error)
        assertEquals(PairingStep.Typed, vm.step)
    }

    @Test
    fun verifyRejectionReturnsToTyped() = runTest {
        val vm = viewModel()
        vm.startTyped()
        vm.onTypedConnect("192.168.1.24", 56789, "743291")

        vm.onVerifyRejected()

        assertEquals(PairingStep.Typed, vm.step)
    }

    @Test
    fun pairFailuresMapToTheErrorStates() {
        assertEquals(PairingError.WrongPin(3), MirrorWrongSecretException(3).asPairingError("h:1"))
        assertEquals(PairingError.Locked, MirrorPairingLockedException().asPairingError("h:1"))
        assertEquals(PairingError.VersionMismatch, MirrorProtocolException(99).asPairingError("h:1"))
        assertEquals(PairingError.Unreachable("h:1"), IllegalStateException("refused").asPairingError("h:1"))
    }

    @Test
    fun pairedStateReflectsStorageAndNeedsRepair() = runTest {
        val vm = viewModel()
        vm.refreshPairedState()
        assertNull(vm.pairedState)

        storage.put(MIRROR_CLIENT_TOKEN_KEY, "tok")
        storage.put(MIRROR_CLIENT_HOST_ENDPOINT_KEY, "192.168.1.24:56789")
        storage.put(MIRROR_CLIENT_PAIRED_AT_KEY, "1752800000000")
        vm.refreshPairedState()
        val paired = vm.pairedState
        assertEquals("192.168.1.24:56789", paired?.endpoint)
        assertTrue(paired?.pairedAtLabel?.isNotBlank() == true)
        assertEquals(false, paired?.needsRepair)

        storage.put(MIRROR_CLIENT_NEEDS_REPAIR_KEY, "true")
        vm.refreshPairedState()
        assertEquals(true, vm.pairedState?.needsRepair)
    }

    @Test
    fun unpairClearsThePairedState() = runTest {
        storage.put(MIRROR_CLIENT_TOKEN_KEY, "tok")
        storage.put(MIRROR_CLIENT_HOST_ENDPOINT_KEY, "192.168.1.24:56789")
        val vm = viewModel()
        vm.refreshPairedState()

        vm.unpair()

        assertNull(vm.pairedState)
        assertTrue(storage.values.isEmpty())
    }

    private companion object {
        const val FINGERPRINT = "ab12cd34ef567890ab12cd34ef567890ab12cd34ef567890ab12cd34ef567890"
    }
}

private class FakeStorage : SecureStorage {
    val values = mutableMapOf<String, String>()
    override suspend fun get(key: String): String? = values[key]
    override suspend fun put(key: String, value: String) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}

private class NoopStore : MirrorLocalStore {
    override suspend fun snapshot(): LibraryExport = LibraryExport()
    override suspend fun dismissals(): List<SyncDismissal> = emptyList()
    override suspend fun baseline(pairingId: String): LibraryExport? = null
    override suspend fun apply(pairingId: String, outcome: MirrorOutcome) = Unit
}
