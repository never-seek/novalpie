package com.novalpie.nativeapp.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Self-contained SDK/Java black-box driver: no AndroidX, Kotlin, JUnit or application classes. */
public final class BetaSdkInstrumentation extends Instrumentation {
    private String phase;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        phase = arguments == null ? "" : arguments.getString("phase", "");
        start();
    }
    @Override public void onStart() {
        try {
            if ("prepare".equals(phase)) prepare();
            else if ("verify".equals(phase)) verify();
            else throw new IllegalArgumentException("Unknown black-box phase");
            Bundle result = new Bundle(); result.putString("stream", "BETA7_SDK_OK " + phase); finish(0, result);
        } catch (Throwable failure) {
            StringWriter trace = new StringWriter(); failure.printStackTrace(new PrintWriter(trace));
            Bundle result = new Bundle(); result.putString("stream", "BETA7_SDK_FAILED " + phase + "\n" + trace); finish(1, result);
        }
    }
    @SuppressWarnings("deprecation") private PackageInfo version() throws Exception {
        return getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(), 0);
    }
    private File snapshotFile() { return new File(getTargetContext().getNoBackupFilesDir(), "beta7-upgrade-test/snapshot.json"); }
    private void prepare() throws Exception {
        PackageInfo version = version();
        require(version.versionName != null && version.versionName.startsWith("2.0.0-native-beta6"), "Must prepare on the actual Beta6 package");
        require(version.versionCode == 2026090101, "Wrong baseline code");
        JSONObject snapshot = capture();
        require(!snapshot.isNull("accountId"), "A retained real login is required");
        require(snapshot.getJSONObject("preferences").length() > 0, "Missing existing preference state");
        snapshot.put("baselineVersion", version.versionName).put("baselineCode", version.versionCode);
        write(snapshotFile(), snapshot.toString());
    }
    private void verify() throws Exception {
        PackageInfo version = version(); Context context = getTargetContext();
        require("2.0.0-native-beta7".equals(version.versionName) && version.versionCode > 2026090101, "Wrong upgrade version");
        require((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0, "Beta must not be debuggable");
        JSONObject before = new JSONObject(read(snapshotFile())); JSONObject after = capture();
        JSONObject oldPrefs = before.getJSONObject("preferences"), newPrefs = after.getJSONObject("preferences");
        int keyCount = 0;
        Iterator<String> names = oldPrefs.keys();
        while (names.hasNext()) {
            String name = names.next(); JSONObject old = oldPrefs.getJSONObject(name), current = newPrefs.getJSONObject(name);
            Iterator<String> keys = old.keys();
            while (keys.hasNext()) { String key = keys.next(); require(old.getString(key).equals(current.getString(key)), "Preference changed: " + name + "/" + key); keyCount++; }
        }
        JSONObject oldFiles = before.getJSONObject("files"), newFiles = after.getJSONObject("files");
        int aliases = 0;
        Iterator<String> paths = oldFiles.keys();
        while (paths.hasNext()) {
            String path = paths.next(), lookup = path;
            // Kotlin's initial snapshot relativized /data/data canonical paths against the
            // /data/user/0 alias. Only normalize that exact same-package alias; still compare
            // every original file's bytes/hash and reject all genuinely missing files.
            String legacyPrefix = "../../../../data/" + context.getPackageName() + "/files/";
            if (path.startsWith(legacyPrefix)) { lookup = path.substring(legacyPrefix.length()); aliases++; }
            require(!lookup.contains(".."), "Unexpected snapshot path alias");
            JSONObject old = oldFiles.getJSONObject(path), current = newFiles.getJSONObject(lookup);
            require(old.getLong("bytes") == current.getLong("bytes") && old.getString("sha256").equals(current.getString("sha256")), "Retained file changed: " + path);
        }
        require(before.optLong("accountId") == after.optLong("accountId"), "Retained login identity changed");
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        require(intent != null, "Missing real launcher"); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Activity activity = startActivitySync(intent);
        try {
            waitForIdleSync(); require(!activity.isFinishing(), "Real activity stopped during launch");
            JSONObject report = new JSONObject().put("passed", true).put("from", before.getString("baselineVersion"))
                .put("to", version.versionName).put("versionCode", version.versionCode).put("debuggable", false)
                .put("preservedPreferenceKeys", keyCount).put("preservedFiles", oldFiles.length())
                .put("legacyPathAliasesResolved", aliases)
                .put("retainedAccountIdentity", true).put("launcherStarted", true).put("allBusinessRuntimeVerified", false);
            write(new File(context.getCacheDir(), "beta7-upgrade-test-report/upgrade.json"), report.toString(2));
            Bundle status = new Bundle(); status.putString("beta7UpgradeReport", report.toString()); sendStatus(0, status);
        } finally { runOnMainSync(activity::finish); }
    }
    private JSONObject capture() throws Exception {
        Context context = getTargetContext(); JSONObject prefs = new JSONObject(), files = new JSONObject();
        Set<String> names = new TreeSet<>(Arrays.asList("novalpie_native_reader_settings", "novalpie_native_reader_progress", "novalpie_reader_replacement_rules",
            "novalpie_native_favorites_settings", "novalpie_native_profile_books_settings", "novalpie_native_search_settings",
            "novalpie_native_reader_tts", "novalpie_native_app_theme", "novalpie_native_download_settings"));
        File[] stored = new File(context.getApplicationInfo().dataDir, "shared_prefs").listFiles();
        if (stored != null) for (File file : stored) {
            String name = file.getName().replaceFirst("\\.xml$", "");
            if (name.matches(".*(reader|favorites|search|profile_books|theme|download_settings).*" ) && !name.matches(".*(auth|workspace|cookie).*")) names.add(name);
        }
        for (String name : names) {
            Map<String, ?> values = context.getSharedPreferences(name, Context.MODE_PRIVATE).getAll();
            if (values.isEmpty()) continue;
            JSONObject data = new JSONObject();
            for (String key : new TreeSet<>(values.keySet())) {
                Object value = values.get(key); String canonical;
                if (value instanceof Set) {
                    List<String> items = new ArrayList<>(); for (Object item : (Set<?>) value) items.add(String.valueOf(item)); Collections.sort(items);
                    canonical = join(items);
                } else canonical = String.valueOf(value);
                data.put(key, sha(((value == null ? "null" : value.getClass().getSimpleName()) + ":" + canonical).getBytes(StandardCharsets.UTF_8)));
            }
            prefs.put(name, data);
        }
        File base = context.getFilesDir().getCanonicalFile();
        for (String name : Arrays.asList("novalpie_reader_chapter_cache", "reader-fonts", "reader-backgrounds", "native-downloads", "epub-editor-archives")) {
            File directory = new File(base, name).getCanonicalFile(); require(base.equals(directory.getParentFile()), "Invalid snapshot root"); collectFiles(directory, base, files);
        }
        Long account = accountId(); return new JSONObject().put("preferences", prefs).put("files", files).put("accountId", account == null ? JSONObject.NULL : account);
    }
    private void collectFiles(File file, File base, JSONObject into) throws Exception {
        if (file.isDirectory()) { File[] children = file.listFiles(); if (children != null) for (File child : children) collectFiles(child, base, into); return; }
        if (!file.isFile()) return;
        File canonical = file.getCanonicalFile(); require(canonical.getPath().startsWith(base.getPath() + File.separator), "File outside snapshot scope");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) { byte[] buffer = new byte[65536]; int count; while ((count = input.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count); }
        into.put(canonical.getPath().substring(base.getPath().length() + 1).replace(File.separatorChar, '/'), new JSONObject().put("bytes", file.length()).put("sha256", hex(digest.digest())));
    }
    private Long accountId() throws Exception {
        String token = getTargetContext().getSharedPreferences("novalpie_native_auth", Context.MODE_PRIVATE).getString("auth_token", null);
        if (token == null) return null; String[] parts = token.trim().split("\\."); if (parts.length < 2) return null;
        JSONObject payload = new JSONObject(new String(Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP), StandardCharsets.UTF_8));
        Long id = number(payload, "sub"); if (id != null) return id;
        JSONObject data = payload.optJSONObject("data"); if (data == null) data = payload;
        for (String key : Arrays.asList("id", "user_id", "userId")) { id = number(data, key); if (id != null) return id; }
        return null;
    }
    private static Long number(JSONObject value, String key) { try { return Long.valueOf(value.optString(key)); } catch (NumberFormatException ignored) { return null; } }
    private static String join(List<String> values) { StringBuilder out = new StringBuilder(); boolean first = true; for (String value : values) { if (!first) out.append('\0'); out.append(value); first = false; } return out.toString(); }
    private static String sha(byte[] value) throws Exception { return hex(MessageDigest.getInstance("SHA-256").digest(value)); }
    private static String hex(byte[] value) { StringBuilder out = new StringBuilder(); for (byte b : value) out.append(String.format(java.util.Locale.US, "%02x", b & 255)); return out.toString(); }
    private static String read(File file) throws Exception { StringBuilder out = new StringBuilder(); try (Reader in = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) { char[] buffer = new char[8192]; int count; while ((count = in.read(buffer)) >= 0) out.append(buffer, 0, count); } return out.toString(); }
    private static void write(File file, String value) throws Exception { require(file.getParentFile().isDirectory() || file.getParentFile().mkdirs(), "Cannot create report folder"); try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) { out.write(value); } }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
