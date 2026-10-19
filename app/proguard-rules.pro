# Retra release rules.

# Engine: GbaCore's companion declares `external` JNI methods that the native library binds by name.
-keep class app.retra.engine.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# kotlinx-serialization: keep generated serializers and companions of @Serializable classes
# (library.json, settings.json, the catalog, and type-safe navigation routes).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.retra.**$$serializer { *; }
-keepclassmembers class app.retra.** {
    *** Companion;
}
-keepclasseswithmembers class app.retra.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Navigation type-safe routes are looked up by their serializer.
-keep @kotlinx.serialization.Serializable class app.retra.ui.nav.** { *; }
