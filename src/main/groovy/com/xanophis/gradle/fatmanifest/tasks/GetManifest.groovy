package com.xanophis.gradle.fatmanifest.tasks

import groovy.json.*

import java.nio.file.Paths

import org.gradle.api.*
import org.gradle.api.tasks.*

class GetManifest extends AbstractHttpBuilderTask {

    /**
     *  Image name, including and prefixes, suffixes, etc.  A Closure,
     *  or an object that resolves to a String.
     */
    @Input
    String imageName

    @Input
    String tag

    @Input
    String architecture

    @Input
    String os

    @OutputDirectory
    File manifestDirectory
    
    @OutputFile
    File manifestFile
    
    @OutputFile
    File metadataFile

    GetManifest() {
        super()
        // Set default targets
        project.afterEvaluate() { project ->
            if (!manifestDirectory) {
                manifestDirectory = project.file(Paths.get(
                project.buildDir.path, project.registry.manifestDirectory, 
                project.registry.name, imageName, 'manifests'))
            }
            if (!manifestFile) {
                manifestFile = new File(manifestDirectory, tag+'.json')
            }
            if (!metadataFile) {
                metadataFile = new File(manifestDirectory, tag+'.metadata.json')
            }
        }
    }
    
    @TaskAction
    def getManifest() {

        def registryHost = new URI(project.registry.url).authority
        def manifest = get("${imageName}/manifests/${tag}")

        logger.quiet "request manifest ${imageName}/manifests/${tag} from ${registryHost}"
        
        manifest.architecture = architecture
        manifest.os = os

        logger.quiet "Manifest received from get:"
        logger.quiet "Class: ${manifest.class}"
        logger.quiet "Contents: ${manifest}"

        println JsonOutput.prettyPrint(new String(manifest.raw, 'UTF-8'))

        if (!project.findProperty('manifests')) {
            project.ext.manifests = new ArrayList<Map>()
        }
        project.manifests.push(manifest)
        
        logger.quiet "Will write manifests and metadata to directory '${manifestDirectory.path}'"
        manifestDirectory.mkdirs()
        manifestFile.setBytes(manifest.raw)
        def metadata = [
            imageName: imageName,
            mediaType: manifest.mediaType,
            size: manifest.size,
            digest: manifest.digest
        ]
        if (manifest.os) metadata.os = manifest.os
        if (manifest.architecture) metadata.architecture = manifest.architecture
        
        metadataFile.write(JsonOutput.toJson(metadata))
    }
}
