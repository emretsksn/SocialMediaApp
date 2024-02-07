buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.huawei.agconnect:agcp:1.5.2.300")
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.google.firebase:firebase-appdistribution-gradle:4.0.1")
    }
    repositories {
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}


// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    id("io.realm.kotlin") version "1.11.0" apply false
}

