package com.xanophis.gradle.fatmanifest.tasks

import groovy.json.*
import groovyx.net.http.*

import org.gradle.api.*
import org.gradle.api.tasks.*

class PutFatManifest extends AbstractHttpBuilderTask {

    static String FAT_MANIFEST_MEDIA_TYPE = 'application/vnd.docker.distribution.manifest.list.v2+json'

    /**
     *  Image name, including and prefixes, suffixes, etc.  A Closure,
     *  or an object that resolves to a String.
     */
    @Input
    String imageName

    @Input
    String tag
    
    /**
     *   Per gradle docs, this is any thing processable by Project.files -- which is quite a lot
     */
    @InputFiles
    def metadataFiles

    public static final String FAT_MANIFEST_MEDIA_TYPE = 'application/vnd.docker.distribution.manifest.list.v2+json'

    PutFatManifest() {
        super()
    }

    @TaskAction
    def putMultiArchManifest() {

        //  TODO - Eventually, there need to be provisions to move blobs and
        //         manifests to the target library.  For now, we're going to
        //         assume they're in the same library.

        //  Copy all the manifests into the current ID.
        // project.manifests.each() { manifest ->
        //     response = put("${myImageName}/manifests/${manifest.architecture}",
        //         manifest.parsed.subMap(['schemaVersion','mediaType','layers']))
        //     println ("Pushed manifest and got response...")
        //     response.getAllHeaders().each() { println it }
        //     println ("...with the above headers")
        // }

        def fatManifest = [
            schemaVersion: 2,
            mediaType: FAT_MANIFEST_MEDIA_TYPE,
            manifests: project.files(metadataFiles).getFiles().collect() { metadataFile -> 
                def metadata = new JsonSlurper().parse(metadataFile)
                [
                    mediaType: metadata.mediaType,
                    size: metadata.size,
                    digest: metadata.digest,
                    platform: [
                        architecture: metadata.architecture,
                        os: metadata.os
                    ]
                ]
            }
        ]
        println JsonOutput.prettyPrint(JsonOutput.toJson(fatManifest))

        def response = put("${imageName}/manifests/${tag}", fatManifest)
        println "HttpResponse from put received..."
        response.getAllHeaders().each() { println it }
    }
}
