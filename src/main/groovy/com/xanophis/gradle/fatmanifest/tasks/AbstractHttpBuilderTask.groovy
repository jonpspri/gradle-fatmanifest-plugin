package com.xanophis.gradle.fatmanifest.tasks

import com.xanophis.gradle.fatmanifest.manifest.ImageManifest

import java.lang.reflect.Constructor
import groovy.json.*

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection

abstract class AbstractHttpBuilderTask extends DefaultTask {

    // TODO - Much of this should be refactored into a DockerRepositoryFacade
    //        that is injected back into the various tasks

    FileCollection classpath

    private def withRestClient(Closure closure) {
        ClassLoader originalClassLoader = getClass().classLoader
        ClassLoader newClassLoader =
            new URLClassLoader(
                classpath.collect(){it.toURI().toURL()} as URL[],
                originalClassLoader)

        try {
            Thread.currentThread().contextClassLoader = newClassLoader
            Class restClientClass = newClassLoader.loadClass('groovyx.net.http.RESTClient')
            Constructor restClassConstructor = restClientClass.getConstructor(Object)
            def restClient = restClassConstructor.newInstance(project.registry.url)
            restClient.auth.basic(project.registry.username, project.registry.password)

            //  Configure parsers for potentially received content types
            Closure schemaParser = { new ImageManifest(it) }
            ImageManifest.GET_CONTENT_TYPES.each() { contentType ->
                restClient.parser[contentType] = schemaParser
            }

            //  Configure encoders for potentially sent content types
            Closure jsonEncoder =  { json, contentType ->
                restClient.encoder.encodeJSON(json, contentType)
            }
            ImageManifest.PUT_CONTENT_TYPES.each() { contentType ->
                restClient.encoder[contentType] = jsonEncoder
            }
            restClient.encoder[PutFatManifest.FAT_MANIFEST_MEDIA_TYPE] = jsonEncoder

            closure.call (restClient)
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    def get(String path) {
        return withRestClient() { restClient ->
            def result
            try {
                restClient.autoAcceptHeader = false
                restClient.headers['Accept'] = [
                    ImageManifest.GET_CONTENT_TYPE_V2_JSON,
                    'application/vnd.docker.distribution.manifest.list.v2+json'
                ].join(', ')
                result = restClient.get path: path

                logger.debug "Result received from get: ${result} with data ${result.data}"
            } catch (Exception e) {
                logger.quiet "Get exception received: ${e}"
                if (e.metaClass.hasProperty(e, 'response')) {
                    logger.debug JsonOutput.prettyPrint(
                        JsonOutput.toJson(e.response.data as Object))
                } else {
                    logger.quiet "Exception received without HttpResponseException semantics."
                }
                throw e
            }
            result.data
        }
    }

    def put(String path, Object body) {
        return withRestClient() { restClient ->
            try {
                restClient.put(
                    path: path,
                    body: body?.raw ? new String(body.raw, 'UTF-8') : body,
                    requestContentType: PutFatManifest.FAT_MANIFEST_MEDIA_TYPE
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
