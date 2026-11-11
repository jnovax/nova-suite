-keep public class * extends android.service.dreams.DreamService
-keep public class * extends android.app.Activity
-keepclassmembers class * {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
