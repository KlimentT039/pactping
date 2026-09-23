import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.sqldelight)
}

kotlin {
  androidLibrary {
    namespace = "com.klt.pactping.shared"
    compileSdk = 36
    minSdk = 28
  }

  val xcfName = "PactPingShared"
  val xcf = XCFramework(xcfName)

  iosArm64 {
    binaries.framework {
      baseName = xcfName
      xcf.add(this)
    }
  }
  iosSimulatorArm64 {
    binaries.framework {
      baseName = xcfName
      xcf.add(this)
    }
  }
  iosX64 {
    binaries.framework {
      baseName = xcfName
      xcf.add(this)
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(libs.kotlinx.coroutines.core)
      api(libs.kotlinx.datetime)
      implementation(libs.sqldelight.runtime)
      implementation(libs.sqldelight.coroutines.extensions)
    }
    androidMain.dependencies {
      implementation(libs.sqldelight.android.driver)
    }
    iosMain.dependencies {
      implementation(libs.sqldelight.native.driver)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}

sqldelight {
  databases {
    create("PactPingDatabase") {
      packageName.set("com.klt.pactping.db")
    }
  }
}
