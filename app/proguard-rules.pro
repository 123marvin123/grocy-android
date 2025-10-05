# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keep,allowobfuscation interface com.google.gson.annotations.SerializedName
-keepattributes InnerClasses,Signature,SourceFile,LineNumberTable

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# For Glide library
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# This was generated automatically by the Android Gradle plugin to hide warnings
# Only has effected these pre-KitKat two compatibility classes
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl

# Google Gemini AI SDK
-keep class com.google.genai.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }
-keepclassmembers class com.google.genai.** { *; }

# Jackson for Gemini AI
-keep class com.fasterxml.jackson.** { *; }
-keep class org.codehaus.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}
-keepclassmembers public final enum com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility {
    public static final com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility *;
}

# Keep Gemini types and builders
-keep class com.google.genai.types.** { *; }
-keepclassmembers class com.google.genai.types.**$Builder { *; }
-keep class * extends com.google.genai.JsonSerializable { *; }

# Keep all constructors and creators for Jackson deserialization
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.JsonCreator <init>(...);
}
-keep @com.fasterxml.jackson.annotation.JsonIgnoreProperties class * { *; }
-keep class * {
    @com.fasterxml.jackson.annotation.JsonProperty *;
}

# Kotlin metadata for Jackson Kotlin support
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# Suppress warnings for optional dependencies
-dontwarn javax.naming.**
-dontwarn javax.servlet.**
-dontwarn org.apache.avalon.framework.**
-dontwarn org.apache.log.**
-dontwarn org.apache.log4j.**
-dontwarn org.commonmark.ext.gfm.strikethrough.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.logging.**
