package com.novalpie.nativeapp.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsColdStartPolicyTest {
    @Test fun initializationBudgetAllowsObservedTenSecondColdStartButRemainsBounded() {
        assertTrue("MuMu实测引擎冷启动超过10秒，不能5秒即丢弃请求", READER_TTS_INITIALIZATION_TIMEOUT_MS >= 15_000L)
        assertTrue("无引擎仍需明确超时", READER_TTS_INITIALIZATION_TIMEOUT_MS <= 30_000L)
    }
}
