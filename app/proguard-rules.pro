# R8 rules for NovalPie 2.0 native.
#
# Release builds previously ran with minifyEnabled false, so this file was a stub.
# R8 is now on, which means every reflective or generated access path has to be
# declared here or it breaks only in release. Keeps are grouped by why they exist,
# so a future reader can tell which are load-bearing.

# ---------------------------------------------------------------------------
# Crash-report readability. Without these, release stack traces are unusable.
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# ---------------------------------------------------------------------------
# org.json ships in the Android platform, not the APK. The app hand-normalises
# every API response through ~90 normalize*() functions built on it.
# ---------------------------------------------------------------------------
-dontwarn org.json.**

# ---------------------------------------------------------------------------
# OkHttp / Okio reference optional JVM-only TLS providers that are absent on
# Android. These are the upstream-recommended suppressions.
# ---------------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembers class okhttp3.internal.publicsuffix.PublicSuffixDatabase { *; }

# ---------------------------------------------------------------------------
# Coil resolves fetchers and decoders via service loading and reflection.
# ---------------------------------------------------------------------------
-dontwarn coil.**
-keep class coil.** { *; }

# ---------------------------------------------------------------------------
# WebView JS bridge. WebFallbackScreen reads auth_token out of the page's
# localStorage/cookies through an @JavascriptInterface object. R8 cannot see that
# call -- it originates in JavaScript -- so the members must be kept explicitly or
# sign-in silently stops working in release only.
# ---------------------------------------------------------------------------
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---------------------------------------------------------------------------
# Reader signed-session crypto and the API/store layer. The reader protocol is
# reverse-engineered from the live site and depends on exact JCE algorithm strings
# ("AES/GCM/NoPadding", "HmacSHA1", "SHA-256", "MD5") plus a custom base64
# alphabet. Kept whole so a protocol failure in release stays diagnosable.
# ---------------------------------------------------------------------------
-keep class com.novalpie.nativeapp.data.** { *; }

# ---------------------------------------------------------------------------
# Compose. AGP contributes the required rules; the runtime reflects over
# Composer internals, so suppress the known warnings.
# ---------------------------------------------------------------------------
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# Kotlin metadata, needed for reflection over data classes and enums.
# ---------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class **$WhenMappings { <fields>; }

# Parcelable / Serializable contracts.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
