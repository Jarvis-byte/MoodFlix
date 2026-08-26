# kotlinx.serialization keeps its generated serializers via annotations
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.arka.moodflix.**$$serializer { *; }
-keepclassmembers class com.arka.moodflix.** {
    *** Companion;
}
-keepclasseswithmembers class com.arka.moodflix.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature, RuntimeVisibleAnnotations
-keep,allowobfuscation interface retrofit2.http.*
