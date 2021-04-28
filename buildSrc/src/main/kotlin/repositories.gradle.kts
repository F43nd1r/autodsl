repositories {
    mavenCentral()
    google()
    maven { setUrl("https://jitpack.io") }
    //TODO: Remove when kotlinx.html is available in maven central
    maven { setUrl("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    jcenter()
    mavenLocal()
}