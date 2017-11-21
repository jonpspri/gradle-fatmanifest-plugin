package com.xanophis.gradle.fatmanifest.manifest

import groovy.json.JsonSlurper

class ImageManifest {

    static final String PUT_CONTENT_TYPE_V1 = 'application/vnd.docker.image.manifest.v1+json'
    static final String PUT_CONTENT_TYPE_V2 = 'application/vnd.docker.image.manifest.v2+json'

    static final String GET_CONTENT_TYPE_V1_JWS = 'application/vnd.docker.distribution.manifest.v1+prettyjws'
    static final String GET_CONTENT_TYPE_V2_JSON = 'application/vnd.docker.distribution.manifest.v2+json'

    static final String[] PUT_CONTENT_TYPES = [ PUT_CONTENT_TYPE_V1, PUT_CONTENT_TYPE_V2 ]
    static final String[] GET_CONTENT_TYPES = [ GET_CONTENT_TYPE_V1_JWS, GET_CONTENT_TYPE_V2_JSON ]

    //  Extracted from Message Headers
    int size
    String digest
    String mediaType

    //  Message content
    private byte[] raw;
    private Map parsed;

    //  Expected to be populated independently
    //  TODO - Should this be a 'platform' class used elsewhere?
    String architecture
    String os

    //  Pull this basic info from the parsed manifest
    int schemaVersion

    ImageManifest(resp) {
        mediaType = resp.getHeaders().'Content-Type'
        size = resp.getHeaders().'Content-Length' as int
        digest = resp.getHeaders().'Docker-Content-Digest'

        raw = resp.getEntity().getContent().getBytes()
        parsed = new JsonSlurper().parse(raw, 'UTF-8')

        schemaVersion = parsed.schemaVersion as int
        if (schemaVersion == 1) { architecture = parsed.architecture }
    }

    String toString() {
        return "size: ${size}\n" \
            + "digest: '${digest}'\n" \
            + "mediaType: '${mediaType}'\n" \
            + "architecture: '${architecture}'\n" \
            + "os: '${os}'" \
            + "schemaVersion: '${schemaVersion}'"
    }
}
