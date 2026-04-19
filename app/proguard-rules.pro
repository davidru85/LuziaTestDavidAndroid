# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers in release stack traces (cheap; ~1KB APK cost).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization — @Serializable DTOs rely on generated companion-
# object serializers that are only referenced through reflection by the
# runtime. R8 full-mode strips them unless kept explicitly.
# Hilt / Room / Ktor / Compose ship their own consumer-proguard-rules
# and do not need manual entries here.
-keepattributes *Annotation*, InnerClasses

-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
    static **$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable object singletons expose their serializer via INSTANCE.
-keepclasseswithmembers class ** {
    static <any> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
