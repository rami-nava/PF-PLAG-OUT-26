package com.example.plag_out.Service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TokenRegistrationCoordinatorTest {
    @Test fun conflictRenewsExactlyOnce() = runTest {
        var token = "old"
        val calls = mutableListOf<String>()
        var rotations = 0
        val coordinator = TokenRegistrationCoordinator({ "A" }, { true }, { token },
            { rotations++; token = "new" }, { calls += it; if (it == "old") 409 else 200 }, {})
        coordinator.register()
        assertEquals(listOf("old", "new"), calls)
        assertEquals(1, rotations)
        assertFalse(coordinator.pending)
    }

    @Test fun secondConflictRemainsPendingWithoutLoop() = runTest {
        var rotations = 0
        var calls = 0
        val coordinator = TokenRegistrationCoordinator({ "A" }, { true }, { "token" },
            { rotations++ }, { calls++; 409 }, {})
        coordinator.register()
        assertEquals(2, calls)
        assertEquals(1, rotations)
        assertTrue(coordinator.pending)
    }

    @Test fun oldSessionCallbackCannotRegisterForNewOwner() = runTest {
        var owner = "A"
        val token = CompletableDeferred<String>()
        val calls = mutableListOf<String>()
        val coordinator = TokenRegistrationCoordinator({ owner }, { true }, { token.await() }, {},
            { calls += owner; 200 }, {})
        val job = launch { coordinator.register() }
        testScheduler.runCurrent()
        coordinator.invalidateSession()
        owner = "B"
        coordinator.beginSession()
        token.complete("token")
        job.join()
        assertTrue(calls.isEmpty())
        coordinator.register()
        assertEquals(listOf("B"), calls)
    }

    @Test fun disabledPreferencePreventsRegistration() = runTest {
        val coordinator = TokenRegistrationCoordinator({ "A" }, { false }, { error("disabled") }, {},
            { error("disabled") }, {})
        coordinator.register()
        assertFalse(coordinator.pending)
    }

    @Test fun offlineWorkRetriesOnlyForCurrentSession() = runTest {
        var offline = true
        val calls = mutableListOf<String>()
        var owner = "A"
        val coordinator = TokenRegistrationCoordinator({ owner }, { true }, { "token" }, {},
            { if (offline) throw java.io.IOException(); calls += owner; 200 }, {})
        try { coordinator.register(); fail() } catch (_: java.io.IOException) { }
        assertTrue(coordinator.pending)
        coordinator.invalidateSession()
        owner = "B"
        coordinator.beginSession()
        offline = false
        coordinator.register()
        assertEquals(listOf("B"), calls)
        assertFalse(coordinator.pending)
    }

    @Test fun slowRotationDoesNotHoldTheLogoutNorLeakTheOldFid() = runTest {
        var owner = "A"
        var token = "old"
        val rotation = CompletableDeferred<Unit>()
        val retired = mutableListOf<String>()
        val registered = mutableListOf<String>()
        val coordinator = TokenRegistrationCoordinator({ owner }, { true }, { token },
            { rotation.await(); token = "new" }, { registered += it; 200 }, { retired += it })

        val finished = coordinator.unregister(remote = true, scope = backgroundScope, waitMs = 5_000)

        assertFalse("logout must not wait for Firebase", finished)
        assertEquals(5_000, testScheduler.currentTime)
        assertEquals(listOf("old"), retired)
        // B signs in while the rotation is still running: its registration waits for it.
        owner = "B"
        coordinator.beginSession()
        val registration = launch { coordinator.register() }
        testScheduler.runCurrent()
        assertTrue(registered.isEmpty())
        rotation.complete(Unit)
        registration.join()
        assertEquals(listOf("new"), registered)
    }

    @Test fun fastRetirementFinishesBeforeTheLogoutContinues() = runTest {
        var rotations = 0
        val retired = mutableListOf<String>()
        val coordinator = TokenRegistrationCoordinator({ "A" }, { true }, { "fid" },
            { rotations++ }, { 200 }, { retired += it })

        assertTrue(coordinator.unregister(remote = true, scope = backgroundScope, waitMs = 5_000))
        assertEquals(listOf("fid"), retired)
        assertEquals(1, rotations)
    }

    @Test fun aFailedBackendRetirementStillRotatesTheFid() = runTest {
        var rotations = 0
        val failures = mutableListOf<Throwable>()
        val coordinator = TokenRegistrationCoordinator({ "A" }, { true }, { "fid" },
            { rotations++ }, { 200 }, { throw java.io.IOException("offline") })

        assertTrue(coordinator.unregister(true, backgroundScope, 5_000) { failures += it })
        assertEquals(1, rotations)
        assertEquals(1, failures.size)
    }

    @Test fun accountDeletionSkipsTheBackendButRotates() = runTest {
        var rotations = 0
        val coordinator = TokenRegistrationCoordinator({ "A" }, { true }, { error("no token needed") },
            { rotations++ }, { 200 }, { error("no backend call") })

        assertTrue(coordinator.unregister(remote = false, scope = backgroundScope, waitMs = 5_000))
        assertEquals(1, rotations)
    }
}
