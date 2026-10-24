# Модели хранятся в SharedPreferences как JSON через Gson.
# Имена полей должны остаться прежними, иначе после сжатия кода
# сохранённые плейлисты перестанут читаться.
-keep class com.example.muzzio.data.models.** { *; }

# Gson должен видеть generic-типы (TypeToken<MutableList<Playlist>>)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Чтобы в отчётах о падениях были номера строк
-keepattributes SourceFile,LineNumberTable
