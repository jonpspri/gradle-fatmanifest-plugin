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

package com.xanophis.gradle.fatmanifest

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration

import com.xanophis.gradle.fatmanifest.utils.RestCall

class FatManifestPlugin implements Plugin<Project> {

    static String FAT_MANIFEST_CONFIGURATION_NAME = 'fatManifest'

    @Override
    void apply(Project project) {
        project.configurations.create(FAT_MANIFEST_CONFIGURATION_NAME)
                .setVisible(false)
                .setTransitive(true)
                .setDescription('The libraries to be used for this project to access Docker Registry.')

        // if no repositories were defined fallback to buildscript
        // repositories to resolve dependencies as a last resort
        project.afterEvaluate {
            if (project.repositories.size() == 0) {
                project.repositories.addAll(project.buildscript.repositories.collect())
            }
        }

        //  TODO - can I just move this up to where I create this at the top of the constructor?
        Configuration config = project.configurations[FAT_MANIFEST_CONFIGURATION_NAME]
        config.defaultDependencies { dependencies ->
            dependencies.add(project.dependencies.create('org.codehaus.groovy.modules.http-builder:http-builder:0.7.2'))
        }

        project.extensions.create('registry', FatManifestExtension)
    }
}
