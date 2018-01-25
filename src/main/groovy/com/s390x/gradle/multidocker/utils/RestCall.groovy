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

package com.s390x.gradle.multidocker.utils

import com.s390x.gradle.multidocker.MultidockerPlugin
import com.s390x.gradle.multidocker.model.ImageManifest

import java.lang.reflect.Constructor
import groovy.json.*

//import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

// TODO - This is a candidate to factor out of the specific gradle plugin
trait RestCall {

    Project project

    private def withRestClient(Closure closure) {

        FileCollection classpath =
            project.configurations[MultidockerPlugin.MULTIDOCKER_CONFIGURATION_NAME]

        ClassLoader originalClassLoader = getClass().classLoader
        ClassLoader newClassLoader = new URLClassLoader(
                classpath.collect(){it.toURI().toURL()} as URL[],
                originalClassLoader
            )

        try {
            Thread.currentThread().contextClassLoader = newClassLoader
            Class restClientClass = newClassLoader.loadClass('groovyx.net.http.RESTClient')
            Constructor restClassConstructor = restClientClass.getConstructor(Object)
            def restClient = restClassConstructor.newInstance(this.url)
            restClient.auth.basic(this.username, this.password)

            //  Configure parsers for potentially received content types
            Closure schemaParser = { new ImageManifest(it) }
            ImageManifest.GET_CONTENT_TYPES.each { contentType ->
                restClient.parser[contentType] = schemaParser
            }

            //  Configure encoders for potentially sent content types
            Closure jsonEncoder =  { json, contentType ->
                restClient.encoder.encodeJSON(json, contentType)
            }
            ImageManifest.PUT_CONTENT_TYPES.each() { contentType ->
                restClient.encoder[contentType] = jsonEncoder
            }
            restClient.encoder[ImageManifest.MANIFEST_LIST_MEDIA_TYPE] = jsonEncoder

            closure.call (restClient)
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    def restGet(def path) {
        return withRestClient() { restClient ->
            def result
            try {
                restClient.autoAcceptHeader = false
                restClient.headers['Accept'] = [
                    ImageManifest.GET_CONTENT_TYPE_V2_JSON,
                    'application/vnd.docker.distribution.manifest.list.v2+json'
                ].join(', ')
                result = restClient.get path: path

                project.logger.debug "Result received from get: ${result} with data ${result.data}"
            } catch (Exception e) {
                project.logger.quiet "Get exception received: ${e}"
                if (e.metaClass.hasProperty(e, 'response')) {
                    project.logger.debug JsonOutput.prettyPrint(
                        JsonOutput.toJson(e.response.data as Object))
                } else {
                    project.logger.quiet "Exception received without HttpResponseException semantics."
                }
                throw e
            }
            result.data
        }
    }

    def restPut(def path, Object body) {
        return withRestClient() { restClient ->
            try {
                restClient.put(
                    path: path,
                    body: body?.raw ? new String(body.raw, 'UTF-8') : body,
                    requestContentType: ImageManifest.MANIFEST_LIST_MEDIA_TYPE
                )
            } catch (Exception e) {
                logger.quiet "Put exception received: ${e}"
                if (e.metaClass.hasProperty(e, 'response')) {
                    logger.quiet JsonOutput.prettyPrint(
                        JsonOutput.toJson(e.response.data as Object))
                } else {
                    logger.quiet "Exception received without HttpResponseException semantics."
                }
                throw e
            }
        }
    }
}
