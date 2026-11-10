# Keep the serialized history and settings models readable by kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep,includedescriptorclasses class dev.amandevelops.solax.**$$serializer { *; }
-keepclassmembers class dev.amandevelops.solax.** {
    *** Companion;
}
-keepclasseswithmembers class dev.amandevelops.solax.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class dev.amandevelops.solax.data.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
