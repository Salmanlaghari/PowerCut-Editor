# PowerCut Pro 2027 8K — ProGuard
-keepattributes Signature, *Annotation*, SourceFile, LineNumberTable, InnerClasses, EnclosingMethod

# JNI native bridge — keep all natives + class names referenced from C++
-keep class com.powercut.export.** { *; }
-keep class com.powercut.model.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Compose
-dontwarn androidx.compose.**

# LevelDB
-keep class org.iq80.leveldb.** { *; }
-keepclassmembers class com.powercut.** {
    public <init>(...);
}
