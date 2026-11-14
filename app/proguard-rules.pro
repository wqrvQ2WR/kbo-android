-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.hayoonjae.kbo.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.hayoonjae.kbo.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
