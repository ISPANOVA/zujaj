-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.smrh.zujaj.**$$serializer { *; }
-keepclassmembers class com.smrh.zujaj.** { *** Companion; }
-keepclasseswithmembers class com.smrh.zujaj.** { kotlinx.serialization.KSerializer serializer(...); }
-keep class com.batoulapps.adhan.** { *; }
-keep class com.android.billingclient.** { *; }

# Harder to reverse-engineer: flatten packages, rename aggressively, strip debug logging.
-repackageclasses 'z'
-allowaccessmodification
-renamesourcefileattribute ''
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
