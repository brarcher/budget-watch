# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/brarcher/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# This keep the class and method names the same, for debugging stack traces
-dontobfuscate

-dontwarn javax.annotation.*
-dontwarn javax.annotation.concurrent.*
-dontwarn sun.misc.Unsafe

# For Guava:
-dontwarn com.google.common.base.**
-keep class com.google.common.base.** {*;}
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** {*;}
-dontwarn com.google.j2objc.annotations.**
-keep class com.google.j2objc.annotations.** { *; }
-dontwarn java.lang.ClassValue
-keep class java.lang.ClassValue { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keep class org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement { *; }

# For SearchView in appcompat
-keep class android.support.v7.widget.SearchView { *; }