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

package com.s390x.gradle.multidocker.tasks

import groovy.json.*

import java.nio.file.Paths

import org.gradle.api.*
import org.gradle.api.tasks.*

import com.s390x.gradle.multidocker.utils.RestCall

/**
 * Retreive from the Docker registry a manifest and associated metadata
 * for a given Docker image.
 */
class GetImageManifest extends AbstractReactiveStreamsTask implements RegistryRestCaller {

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

    void runReactiveStream() {
        def manifest = registry.restGet("${imageName}/manifests/${tag}")

        // TODO - should there be some error detection here?  404, etc.

        logger.debug "Manifest received from get:"
        logger.debug "Class: ${manifest.class}"
        logger.debug "Contents: ${manifest}"

        if (onNext) {
            logger.debug JsonOutput.prettyPrint(new String(manifest.raw, 'UTF-8'))
            //  Send the entire manifest because often the mediaType, length and digest matter
            onNext.call(manifest)
        } else {
            logger.quiet("Manifest retreived from registry (stdout only):")
            logger.quiet(JsonOutput.prettyPrint(new String(manifest.raw, 'UTF-8')))
        }
    }
}
