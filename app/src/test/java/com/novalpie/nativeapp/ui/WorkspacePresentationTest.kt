package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspacePresentationTest {
    @Test
    fun masksApiKeysWithoutExposingFullCredential() {
        assertEquals("sk-s******alue", maskWorkspaceApiKey("sk-secret-value"))
        assertEquals("********", maskWorkspaceApiKey("short"))
        assertEquals("\u672a\u914d\u7f6e", maskWorkspaceApiKey(null))
    }

    @Test
    fun validatesApiDraftAgainstWebsiteRequiredFields() {
        assertNull(validateWorkspaceApiDraft(WorkspaceApiDraft(name = "A", model = "m", endpoint = "https://api.example.com", apiKey = "sk", concurrency = "10")))
        assertEquals("API \u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a", validateWorkspaceApiDraft(WorkspaceApiDraft()))
        assertEquals("API \u7aef\u70b9\u5fc5\u987b\u662f http(s) URL", validateWorkspaceApiDraft(WorkspaceApiDraft(name = "A", model = "m", endpoint = "file://x", apiKey = "sk")))
        assertEquals("\u5e76\u53d1\u6570\u5fc5\u987b\u4ecb\u4e8e 1 \u5230 100", validateWorkspaceApiDraft(WorkspaceApiDraft(name = "A", model = "m", endpoint = "https://api.example.com", apiKey = "sk", concurrency = "0")))
    }

    @Test
    fun validatesCookieDraftAndProxyFormat() {
        assertNull(validateWorkspaceCookieDraft(WorkspaceCookieDraft(configKey = "main", cookieRaw = "a=b", proxyIp = "10.0.2.2:7890")))
        assertEquals("\u914d\u7f6e\u952e\u540d\u4e0d\u80fd\u4e3a\u7a7a", validateWorkspaceCookieDraft(WorkspaceCookieDraft(cookieRaw = "a=b")))
        assertEquals("Cookie \u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a", validateWorkspaceCookieDraft(WorkspaceCookieDraft(configKey = "main")))
        assertEquals("\u4ee3\u7406\u683c\u5f0f\u5e94\u4e3a IP:PORT \u6216 http(s)://...", validateWorkspaceCookieDraft(WorkspaceCookieDraft(configKey = "main", cookieRaw = "a=b", proxyIp = "bad")))
    }
}
