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

package com.s390x.gradle.multidocker.model

import groovy.json.JsonSlurper

class ImageManifest {

    static final String PUT_CONTENT_TYPE_V1 = 'application/vnd.docker.image.manifest.v1+json'
    static final String PUT_CONTENT_TYPE_V2 = 'application/vnd.docker.image.manifest.v2+json'

    static final String GET_CONTENT_TYPE_V1_JWS = 'application/vnd.docker.distribution.manifest.v1+prettyjws'
    static final String GET_CONTENT_TYPE_V2_JSON = 'application/vnd.docker.distribution.manifest.v2+json'

    static final String MANIFEST_LIST_MEDIA_TYPE = 'application/vnd.docker.distribution.manifest.list.v2+json'

    static final String[] PUT_CONTENT_TYPES = [ this.PUT_CONTENT_TYPE_V1, this.PUT_CONTENT_TYPE_V2 ]
    static final String[] GET_CONTENT_TYPES = [ this.GET_CONTENT_TYPE_V1_JWS, this.GET_CONTENT_TYPE_V2_JSON ]

    //  Extracted from Message Headers
    int size
    String digest
    String mediaType

    //  Message content
    private byte[] raw;
    private Map parsed;

    ImageManifest(resp) {
        mediaType = resp.getHeaders().'Content-Type'
        size = resp.getHeaders().'Content-Length' as int
        digest = resp.getHeaders().'Docker-Content-Digest'

        raw = resp.getEntity().getContent().getBytes()
        parsed = new JsonSlurper().parse(raw, 'UTF-8')
    }
}
