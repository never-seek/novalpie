package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.ReaderReplacementRegexFlag
import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.model.ReaderReplacementTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderReplacementWebsiteCodecTest {
    @Test
    fun regexRulesUseTheWebsiteGlossaryWireFormat() {
        val rule = ReaderReplacementRule(
            id = "local-1",
            novelId = 12L,
            source = "(Alice)/(Bob)",
            replacement = "$2-$1",
            isRegex = true,
            regexFlags = setOf(
                ReaderReplacementRegexFlag.IgnoreCase,
                ReaderReplacementRegexFlag.Multiline,
                ReaderReplacementRegexFlag.DotMatchesAll,
            ),
            target = ReaderReplacementTarget.Both,
        )

        assertEquals(
            "re:/(Alice)\\/(Bob)/gims",
            encodeWebsiteReaderReplacementSource(rule),
        )
    }
}
