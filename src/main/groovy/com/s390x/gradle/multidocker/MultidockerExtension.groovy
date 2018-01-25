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

package com.s390x.gradle.multidocker

import com.s390x.gradle.multidocker.model.DockerRegistry
import com.s390x.gradle.multidocker.model.DockerEngine

import org.gradle.api.Project

class MultidockerExtension {

    Project project;

    Map<String,DockerRegistry> dockerRegistries = [:]
	Map<String,DockerEngine> dockerEngines = [:]

    def registry(String name, def registry) {
        dockerRegistries[name] = new DockerRegistry(project, registry)
    }

    def engine(String name, def registry) {
        dockerEngines[name] = new DockerEngine(engine)
    }
}
