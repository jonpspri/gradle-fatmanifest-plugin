/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.xanophis.gradle.fatmanifest.tasks

import groovy.json.*
import groovyx.net.http.*

import org.gradle.api.*
import org.gradle.api.tasks.*

import com.xanophis.gradle.fatmanifest.manifest.ImageManifest
import com.xanophis.gradle.fatmanifest.utils.RestCall

/**
 * Create and PUT to the docker repository a multi-architecture manifest
 * (a.k.a. a fat manifest) for the provided simple manifests.
 */
class PutFatManifest extends DefaultTask implements RestCall {

    class Manifest {
        String os
        String architecture
        String mediaType
        int size
        String digest

        Manifest(Object manifest) {
            this.os = manifest.os
            this.architecture = manifest.architecture
            this.mediaType = manifest.mediaType
            this.size = manifest.size
            this.digest = manifest.digest
        }
    }

    /*
        Currently, each element can be either a Closure that returns a
        Map that maps to Manifest, or a Map to Manifest.
     */
    @Input
    def manifests = []

    /**
     *  Target image name, including and prefixes, suffixes, etc.  Usally is
     *  in the form {@code "${library}/${image}"}
     */
    @Input
    String imageName

    /**
     *  Target docker tag.  Usually something like {@code 'latest'}.
     */
    @Input
    String tag

    @TaskAction
    void putFatManifest() {

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
            mediaType: ImageManifest.FAT_MANIFEST_MEDIA_TYPE,
            manifests: manifests.collect() {
                Manifest manifest;
                switch (it) {
                    case Closure:
                        manifest = new Manifest(it.call())
                        break
                    case Map:
                        manifest = new Manifest(it)
                        break
                    case Manifest:
                        manifest = it
                        break
                    default:
                        throw new GradleException("Cannot extract manifest from Object of Class #{it.class}")
                        break
                }
                [
                    mediaType: manifest.mediaType,
                    size: manifest.size,
                    digest: manifest.digest,
                    platform: [
                        architecture: manifest.architecture,
                        os: manifest.os
                    ]
                ]
            }
        ]
        logger.debug JsonOutput.prettyPrint(JsonOutput.toJson(fatManifest))

        def response = put("${imageName}/manifests/${tag}", fatManifest)
        logger.debug "HttpResponse from put received..."
        response.getAllHeaders().each { println it }
    }
}
