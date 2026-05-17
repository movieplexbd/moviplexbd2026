# Add project specific ProGuard rules here.

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.database.** { *; }
-keep class com.google.firebase.auth.** { *; }

# Firebase Realtime Database - keep model classes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keep class com.ottapp.moviestream.data.model.** { *; }
-keepclassmembers class com.ottapp.moviestream.data.model.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES; public *;
}
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# Shimmer
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

# CircleImageView
-keep class de.hdodenhof.circleimageview.** { *; }
-dontwarn de.hdodenhof.circleimageview.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ViewBinding - keep all generated binding classes
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}
-keep class com.ottapp.moviestream.databinding.** { *; }

# Keep all Fragment subclasses
-keep class * extends androidx.fragment.app.Fragment { <init>(); }
-keep class com.ottapp.moviestream.ui.** { *; }

# Keep ViewModel subclasses
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep RecyclerView Adapter subclasses
-keep class com.ottapp.moviestream.adapter.** { *; }

# Keep Application class
-keep class com.ottapp.moviestream.OTTApplication { *; }

# Keep Activity/Service classes
-keep class com.ottapp.moviestream.SplashActivity { *; }
-keep class com.ottapp.moviestream.LoginActivity { *; }
-keep class com.ottapp.moviestream.MainActivity { *; }
-keep class com.ottapp.moviestream.service.DownloadService { *; }
-keep class com.ottapp.moviestream.ui.player.PlayerActivity { *; }
-keep class com.ottapp.moviestream.ui.admin.AdminActivity { *; }
-keep class com.ottapp.moviestream.ui.admin.AddEditMovieActivity { *; }

# Keep all repositories
-keep class com.ottapp.moviestream.data.repository.** { *; }

# Keep util classes
-keep class com.ottapp.moviestream.util.** { *; }

# Keep BottomSheetDialogFragment subclasses
-keep class * extends com.google.android.material.bottomsheet.BottomSheetDialogFragment { *; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# StateFlow / LiveData
-keep class kotlinx.coroutines.flow.** { *; }
-keepclassmembers class kotlinx.coroutines.flow.** { *; }

# Prevent stripping of Lifecycle observers
-keep class * implements androidx.lifecycle.LifecycleObserver { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver { *; }

# Keep Enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable/Serializable
-keep class * implements android.os.Parcelable { *; }
-keep class * implements java.io.Serializable { *; }

# Room Database
-keep class com.ottapp.moviestream.data.local.** { *; }
-dontwarn androidx.room.**

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
