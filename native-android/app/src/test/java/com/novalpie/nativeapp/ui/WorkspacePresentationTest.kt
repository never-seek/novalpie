package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspacePresentationTest {
    @Test
    fun masksApiKeysWithoutExposingFullCredential() {
        assertEquals("sk-s******alue", maskWorkspaceApiKey("sk-secret-value"))
        assertEquals("********", maskWorkspaceApiKey("short"))
        assertEquals("未配置", maskWorkspaceApiKey(null))
    }

    @Test
    fun validatesApiDraftAgainstWebsiteRequiredFields() {
        assertNull(validateWorkspaceApiDraft(WorkspaceApiDraft(name = "A", model = "m", endpoint = "https://api.example.com", apiKey = "sk", concurrency = "10")))
        assertEquals("API 名称不能为空", validateWorkspaceApiDraft(WorkspaceApiDraft()))
        assertEquals("API 端点必须是 http(s) URL", validateWorkspaceApiDraft(WorkspaceApiDraft(name = "A", model = "m", endpoint = "file://x", apiKey = "sk")))
        assertEquals("并发数必须介于 1 到 100", validateWorkspaceApiDraft(WorkspaceApiDraft(name = "A", model = "m", endpoint = "https://api.example.com", apiKey = "sk", concurrency = "0")))
    }

    @Test
    fun validatesCookieDraftAndProxyFormat() {
        assertNull(validateWorkspaceCookieDraft(WorkspaceCookieDraft(configKey = "main", cookieRaw = "a=b", proxyIp = "10.0.2.2:7890")))
        assertEquals("配置键名不能为空", validateWorkspaceCookieDraft(WorkspaceCookieDraft(cookieRaw = "a=b")))
        assertEquals("Cookie 内容不能为空", validateWorkspaceCookieDraft(WorkspaceCookieDraft(configKey = "main")))
        assertEquals("代理格式应为 IP:PORT 或 http(s)://...", validateWorkspaceCookieDraft(WorkspaceCookieDraft(configKey = "main", cookieRaw = "a=b", proxyIp = "bad")))
    }

    @Test
    fun sourceApiActivationStatusHasReadableLabelsAndAction() {
        assertEquals("已激活", workspaceApiStatusLabel("active", fallbackActive = false))
        assertEquals("已禁用", workspaceApiStatusLabel("disabled", fallbackActive = true))
        assertEquals("已停用", workspaceApiStatusLabel(null, fallbackActive = false))
        assertEquals("停用", workspaceApiToggleActionLabel(isActive = true))
        assertEquals("启用", workspaceApiToggleActionLabel(isActive = false))
    }
}
