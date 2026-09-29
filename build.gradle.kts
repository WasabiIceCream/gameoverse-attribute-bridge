plugins {
    id("fabric-loom") version "1.17.20"
    `java-library`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // The mods this bridges, compiled against the installed jars (reference-jars/, not committed) and
    // provided at runtime by those same mods.
    compileOnly(files(
        "reference-jars/apothic-attributes-fabric-3.0.1-fabric.4.jar",
        "reference-jars/critical_strike-fabric-1.0.6+26.1.2.jar",
        "reference-jars/spell_engine-fabric-1.10.5+26.1.2.jar",
        "reference-jars/spell_power-fabric-1.6.2+26.1.2.jar",
        "reference-jars/ranged_weapon_api-fabric-4.0.0+26.1.2.jar"
    ))
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
