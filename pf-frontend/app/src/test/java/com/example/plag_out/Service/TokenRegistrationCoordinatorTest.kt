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
}
