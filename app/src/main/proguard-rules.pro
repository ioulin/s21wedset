# 프로가드 룰

# 제미나이 API 응답 모델 유지
-keep class com.example.flogem.data.model.** { *; }

# Gson
-keep class com.example.flogem.data.model.** { <fields>; }
-keep class com.google.gson.** { *; }

# Retrofit
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclasseswithusermemberclass,annotation:retrofit2.*
-keepfields,allowobfuscation,annotation:retrofit2.*
-keepattributes Signature
-keepattributes NoSuchMethodError

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.paging

# Hilt
-keep class dagger.hilt.** { *; }
-keep class com.example.flogem.di.** { *; }
