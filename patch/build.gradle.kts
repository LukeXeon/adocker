plugins {
    id("java-library")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "--patch-module", "java.base=${sourceSets.main.get().java.srcDirs.first()}"
    ))
}
