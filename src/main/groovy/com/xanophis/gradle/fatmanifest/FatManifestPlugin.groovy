package com.xanophis.gradle.fatmanifest

import com.xanophis.gradle.fatmanifest.tasks.AbstractHttpBuilderTask

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration

class FatManifestPlugin implements Plugin<Project> {

    static String FAT_MANIFEST_CONFIGURATION_NAME = 'fatManifest'

    @Override
    void apply(Project project) {
        project.configurations.create(FAT_MANIFEST_CONFIGURATION_NAME)
                .setVisible(false)
                .setTransitive(true)
                .setDescription('The libraries to be used for this project to access Docker Registry.')

        // if no repositories were defined fallback to buildscript
        // repositories to resolve dependencies as a last resort
        project.afterEvaluate {
            if (project.repositories.size() == 0) {
                project.repositories.addAll(project.buildscript.repositories.collect())
            }
        }

        //  TODO - can I just move this up to where I create this at the top of the constructor?
        Configuration config = project.configurations[FAT_MANIFEST_CONFIGURATION_NAME]
        config.defaultDependencies { dependencies ->
            dependencies.add(project.dependencies.create('org.codehaus.groovy.modules.http-builder:http-builder:0.7.2'))
        }

        project.extensions.create('registry', FatManifestExtension)

        project.tasks.withType(AbstractHttpBuilderTask) { it.classpath = config }
    }
}
