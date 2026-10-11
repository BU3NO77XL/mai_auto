import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signingProperties = Properties().apply {
    val propertiesFile = rootProject.file("app/signing.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use { load(it) }
    }
}
val releaseStoreFile = signingProperties.getProperty("storeFile")
    ?.let { rootProject.file(it) }
    // CI (GitHub Actions): o keystore chega via secret KEYSTORE_BASE64 e é salvo em
    // app/build/ci-upload-keystore.<ext>; o bloco abaixo usa esse arquivo quando existe.
    .let { localFile ->
        val ciStoreFile = rootProject.file("app/build/ci-upload-keystore.p12")
            .takeIf { it.isFile }
            ?: rootProject.file("app/build/ci-upload-keystore.jks")
                .takeIf { it.isFile }
        ciStoreFile ?: localFile
    }
val releaseKeyAlias = signingProperties.getProperty("keyAlias")
    ?.takeIf { it.isNotBlank() }
    ?: providers.environmentVariable("KEY_ALIAS").orNull
val releaseStorePassword = providers.environmentVariable("MAI_KEYSTORE_PASSWORD").orNull
    ?: providers.environmentVariable("LUNA_KEYSTORE_PASSWORD").orNull
    ?: providers.environmentVariable("KEYSTORE_PASSWORD").orNull
val releaseKeyPassword = providers.environmentVariable("MAI_KEY_PASSWORD").orNull
    ?: providers.environmentVariable("LUNA_KEY_PASSWORD").orNull
    ?: providers.environmentVariable("KEYSTORE_PASSWORD").orNull
val releaseSigningConfigured = releaseStoreFile?.isFile == true &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "br.com.hydra.automacao"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.hydra.automacao"
        minSdk = 26
        targetSdk = 35
        // CI: cada run do GitHub Actions gera um versionCode único crescente,
        // senão o Android trata builds diferentes como "mesma versão" e não atualiza.
        // Local: vale o fallback 2.
        versionCode = providers.environmentVariable("GITHUB_RUN_NUMBER")
            .orNull?.toIntOrNull() ?: 2
        versionName = "0.2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    if (releaseSigningConfigured) {
        signingConfigs {
            create("release") {
                storeFile = releaseStoreFile
                // CI salva sempre como .p12 ou .jks; infere o tipo pela extensão
                // (local continua valendo o storeType do signing.properties).
                val ciName = releaseStoreFile?.name.orEmpty().lowercase()
                val inferredType = when {
                    ciName.endsWith(".jks") -> "JKS"
                    ciName.endsWith(".p12") || ciName.endsWith(".pfx") -> "PKCS12"
                    else -> null
                }
                storeType = inferredType
                    ?: signingProperties.getProperty("storeType", "PKCS12")
                keyAlias = releaseKeyAlias
                storePassword = releaseStorePassword
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

tasks.configureEach {
    if (name == "assembleRelease" || name == "bundleRelease") {
        doFirst {
            check(releaseSigningConfigured) {
                "Assinatura de release incompleta. Confira app/signing.properties e defina " +
                    "MAI_KEYSTORE_PASSWORD e MAI_KEY_PASSWORD no ambiente."
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
