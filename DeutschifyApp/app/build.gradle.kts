plugins {
    id("com.android.application")
    id("jacoco")
}

android {
    namespace = "com.zehranur.deutschifyapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zehranur.deutschifyapp"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
jacoco {
    toolVersion = "0.8.11"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val uiExcludes = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Activity*.*",
        "**/*ViewModel*.*",
        "**/*Adapter*.*"
    )

    // util/ ve model/ paketleri — AGP 8.x class ara dizini için wildcard
    val buildDir = layout.buildDirectory.get().asFile

    val classDir = fileTree("$buildDir/intermediates/javac/debug") {
        include(
            "**/com/zehranur/deutschifyapp/util/**/*.class",
            "**/com/zehranur/deutschifyapp/model/**/*.class"
        )
        exclude(uiExcludes)
    }
    sourceDirectories.setFrom(files("${projectDir}/src/main/java"))
    classDirectories.setFrom(files(classDir))

    // enableUnitTestCoverage=true ile AGP'nin ürettiği .exec dosyası
    executionData.setFrom(fileTree(buildDir) {
        include(
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            "jacoco/testDebugUnitTest.exec"
        )
    })
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
