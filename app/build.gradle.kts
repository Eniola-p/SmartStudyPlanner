plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.eniola.smartstudyplanner"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.eniola.smartstudyplanner"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // Firebase setup
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage")

    // Firebase AI Logic for Gemini
    implementation("com.google.firebase:firebase-ai")

    // Firebase App Check for local development
    implementation("com.google.firebase:firebase-appcheck-debug")

    // Glide for profile pictures
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // MPAndroidChart for statistics graph
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Enables ViewModel support for fragments.
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

