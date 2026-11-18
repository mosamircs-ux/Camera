# Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-keep public class com.google.android.gms.ads.MobileAds { public *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.**

# Keep custom app classes & view bindings
-keep class com.luminacamera.camera.LuminaApp { *; }
-keep class com.luminacamera.camera.helpers.AdMobManager { *; }
