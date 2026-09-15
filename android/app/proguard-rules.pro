# Aturan R8/ProGuard untuk Siaga Padang

# Jaga seluruh DTO API darurat dan model lokal dari penamaan ulang/obfuscation
-keep class com.akusukaproject.siagapadang.data.remote.model.** { *; }
-keep class com.akusukaproject.siagapadang.data.model.** { *; }
-keep class com.akusukaproject.siagapadang.data.local.** { *; }

# Jaga Room Database & DAO
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Jaga MapLibre Native SDK
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**
