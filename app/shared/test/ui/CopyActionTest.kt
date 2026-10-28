package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.form.CopyAction
import com.usuallycoding.opensecretsanta.ui.form.CopyStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CopyActionTest {
    @Test
    fun aCopyWritesTheWholeTextShowsCopiedAndGoesBackToIdle() = runTest {
        val written = mutableListOf<String>()
        val action = CopyAction({ written += it }, holdMillis = 1000)
        assertEquals(CopyStatus.Idle, action.status.value)
        val job = launch { action.copy("the whole note") }
        runCurrent()
        assertEquals(listOf("the whole note"), written)
        assertEquals(CopyStatus.Copied, action.status.value)
        advanceTimeBy(999)
        assertEquals(CopyStatus.Copied, action.status.value)
        advanceTimeBy(2)
        job.join()
        assertEquals(CopyStatus.Idle, action.status.value)
    }

    @Test
    fun aRefusedCopyStaysFailedUntilTheNextTry() = runTest {
        var refuse = true
        val action = CopyAction({ if (refuse) error("no clipboard") }, holdMillis = 1000)
        action.copy("x")
        assertEquals(CopyStatus.Failed, action.status.value)
        advanceTimeBy(5000)
        assertEquals(CopyStatus.Failed, action.status.value)
        refuse = false
        val job = launch { action.copy("x") }
        runCurrent()
        assertEquals(CopyStatus.Copied, action.status.value)
        job.join()
        assertEquals(CopyStatus.Idle, action.status.value)
    }
}
