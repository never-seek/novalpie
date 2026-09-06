package com.novalpie.nativeapp.data

import org.junit.Assert.*
import org.junit.Test

class ReaderServerClockTest {
    @Test fun observedServerTimeRemainsStableWhenThePhoneClockChanges() {
        val clock=ReaderServerClock()
        assertEquals(10L,clock.epochSeconds(10000,0))
        assertTrue(clock.observe("Sun, 06 Sep 2026 02:00:00 GMT",100))
        val baseline=clock.epochSeconds(0,100)
        assertEquals(baseline+5,clock.epochSeconds(9_000_000_000_000,5100))
        assertEquals(1234L,clock.epochSeconds(1_234_000,22_000_100))
    }
    @Test fun invalidDatesNeverReplaceAValidSample() {
        val clock=ReaderServerClock()
        clock.observe("Sun, 06 Sep 2026 02:00:00 GMT",0)
        val before=clock.epochSeconds(0,0)
        assertFalse(clock.observe("not a timestamp",10))
        assertFalse(clock.observe("Thu, 01 Jan 1970 00:00:00 GMT",10))
        assertEquals(before,clock.epochSeconds(0,10))
    }
}
