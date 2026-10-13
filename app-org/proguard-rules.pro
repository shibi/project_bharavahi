# Navigation routes are @Serializable classes.
-keepclassmembers @kotlinx.serialization.Serializable class com.bharavahi.org.navigation.** { *; }

# Gmail SMTP backup email (javax.mail via com.sun.mail:android-mail).
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class myjava.awt.datatransfer.** { *; }
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.security.**
-dontwarn javax.mail.**
