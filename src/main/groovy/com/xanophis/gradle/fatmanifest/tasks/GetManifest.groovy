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

import java.nio.file.Paths

import org.gradle.api.*
import org.gradle.api.tasks.*

/**
 * Retreive from the Docker registry a manafest and associated metadata
 * for a given Docker image.
 */
class GetManifest extends AbstractHttpBuilderTask {

    /**
     *  Source image name, including and prefixes, suffixes, etc.  Usally is in the
     *  form {@code "${library}/${image}"}
     */
    @Input
    String imageName

    /**
     *  Target docker tag.  Usually something like {@code "latest-${arch}"}.
     */
    @Input
    String tag

    /**
     * The architecture of under which the image is intended to run.  
     * Usually {@code 'amd64'}
     */
    @Input
    String architecture

    /**
     * The operating system under which the image is intended to run.  
     * Usually {@code 'linux'}
     */
    @Input
    String os

    /**
     * The directory into which the {@link manifestFile} and {@link metadataFile}
     * will be placed.  This is interpreted relative to {@code Project.projectDir}
     * 
     * @depricated
     */
    @OutputDirectory
    File manifestDirectory
    
    /**
     * The path of the manifestFile.  This JSON file will hold the manifest passed
     * back by the docker registry.
     */
    @OutputFile
    File manifestFile
    
    /**
     * The path of the metadataFile.  This file will hold other information not
     * in the core manifest, including:
     *
     * <ul>
     * <li><b>imageName:</b> The image name provided in the task configuration</li>
     * <li><b>os:</b> The intended operating system provided in the task configuration</li>
     * <li><b>architecture:</b> The intended architecture provided in the task configuration.</li>
     * <li><b>mediaType:</b> The MIME media type specified by the docker registry for the
     * manifest.</li>
     * <li><b>size:</b> size of the manifest reported by Docker in the REST response headers</li>
     * <li><b>digest:</b> Docker-computed crytographic digest for the manifest</li>
     * </ul>
     */
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
    void getManifest() {

        def registryHost = new URI(project.registry.url).authority
        def manifest = get("${imageName}/manifests/${tag}")

        logger.info "request manifest ${imageName}/manifests/${tag} from ${registryHost}"
        
        manifest.architecture = architecture
        manifest.os = os

        logger.debug "Manifest received from get:"
        logger.debug "Class: ${manifest.class}"
        logger.debug "Contents: ${manifest}"

        logger.debug JsonOutput.prettyPrint(new String(manifest.raw, 'UTF-8'))

        if (!project.findProperty('manifests')) {
            project.ext.manifests = new ArrayList<Map>()
        }
        project.manifests.push(manifest)

        // TODO - This isn't really needed; the responsiblity for making the target directories
        //        may lie elsewhere, or perhaps they should be extracted from the individual
        //        files.
        //logger.quiet "Will write manifests and metadata to directory '${manifestDirectory.path}'"
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
