plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.clientledger.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.clientledger.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 20
        versionName = "0.1.19"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Добавляем задачу для получения SHA-1 отпечатка
    tasks.register("printSha1") {
        group = "other"
        description = "Выводит SHA-1 отпечаток debug keystore"
        doLast {
            val keystoreFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            if (keystoreFile.exists()) {
                println("\n" + "=".repeat(60))
                println("  SHA-1 для Debug APK:")
                println("=".repeat(60))
                exec {
                    commandLine(
                        "keytool",
                        "-list",
                        "-v",
                        "-keystore", keystoreFile.absolutePath,
                        "-alias", "androiddebugkey",
                        "-storepass", "android",
                        "-keypass", "android"
                    )
                }
                println("\n" + "=".repeat(60))
                println("  Данные для Google Cloud Console:")
                println("=".repeat(60))
                println("Package name: com.clientledger.app")
                println("SHA-1: (см. строку SHA1: выше)")
                println("=".repeat(60) + "\n")
            } else {
                println("\n✗ Debug keystore не найден по пути: ${keystoreFile.absolutePath}")
                println("Запустите Android Studio хотя бы один раз или создайте debug keystore вручную.\n")
            }
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // Charts
    implementation("com.patrykandpatrick.vico:compose:1.13.1")
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")
    implementation("com.patrykandpatrick.vico:core:1.13.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // JSON serialization for backup
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Jackson для Google API Client (требуется для JacksonFactory)
    // Версия 2.12.3 совместима с google-api-client-android:1.32.1
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    
    // Google Play Services for authentication
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    
    // Google HTTP Client for Android (нужен для AndroidHttp)
    implementation("com.google.http-client:google-http-client-android:1.43.3")
    
    // Google HTTP Client Gson (нужен для GsonFactory)
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    
    // Google API Client (базовая библиотека)
    implementation("com.google.api-client:google-api-client:1.32.1")
    
    // Google API Client for Android (версия 1.32.1 содержит GoogleAccountCredential)
    implementation("com.google.api-client:google-api-client-android:1.32.1") {
        exclude(group = "com.google.android.gms", module = "play-services-auth")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


