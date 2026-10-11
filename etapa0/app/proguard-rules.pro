# TRD §Distribuição: classes do Vosk e da JNA são acessadas por código nativo; os nomes ficam.
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
# A JNA referencia AWT (desktop), ausente no Android; o caminho não é usado.
-dontwarn java.awt.**

# TRD §Onde o texto vive: em release, o R8 remove as chamadas de Log.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
}
