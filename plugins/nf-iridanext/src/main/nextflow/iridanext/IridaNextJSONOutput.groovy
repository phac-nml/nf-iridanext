/*
 * Copyright 2023, Government of Canada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nextflow.iridanext

import java.nio.file.Path
import java.nio.file.Paths

import java.util.List
import java.util.Map
import java.nio.file.Path
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

import groovy.transform.CompileStatic
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class IridaNextJSONOutput {
    private Map files = ["global": [], "samples": [:]]
    private Map metadata = ["samples": [:]]
    private Map<String,Set<String>> scopeIds = ["samples": [] as Set<String>]
    // private final Map<String, List<Map<Object, Object>>> files = ["global": [], "samples": []]
    // private final Map<String, Map<Object, Object>> metadata = ["samples": []]
    private Path relativizePath
    private Boolean shouldRelativize
    private Boolean flatten

    public IridaNextJSONOutput(Path relativizePath = null, Boolean flatten = false) {
        this.relativizePath = relativizePath
        this.shouldRelativize = (this.relativizePath != null)
        this.flatten = flatten
    }

    public void appendMetadata(String scope, Map data) {
        if (scope in metadata.keySet()) {
            Map validMetadata = data.collectEntries { k, v ->
                if (k in scopeIds[scope]) {
                    return [(k): v]
                } else {
                    log.trace "scope=${scope}, id=${k} is not a valid identifier. Removing from metadata."
                    return [:]
                }
            }
            metadata[scope] = (metadata[scope] as Map) + validMetadata
        }
    }

    public void addId(String scope, String id) {
        log.trace "Adding scope=${scope} id=${id}"
        scopeIds[scope].add(id)
    }

    public Boolean isValidId(String scope, String id) {
        return id in scopeIds[scope]
    }

    public void addFile(String scope, String subscope, Path path) {
        if (!(scope in files.keySet())) {
            throw new Exception("scope=${scope} not in valid set of scopes: ${files.keySet()}")
        }

        if (shouldRelativize) {
            path = relativizePath.relativize(path)
        }

        // Treat empty string and null as same
        if (subscope == "") {
            subscope = null
        }

        def files_scope = files[scope]
        if (scope == "samples" && subscope == null) {
            throw new Exception("scope=${scope} but subscope is null")
        } else if (scope == "samples" && subscope != null) {
            assert isValidId(scope, subscope)

            def files_scope_map = (Map)files_scope
            if (!files_scope_map.containsKey(subscope)) {
                files_scope_map[subscope] = []
            }
            ((List)files_scope_map[subscope]).push(["path": path.toString()])
        } else if (scope == "global") {
            def files_scope_list = (List)files_scope
            files_scope_list.push(["path": path.toString()])
        } else {
            throw new Exception("scope=${scope} is not valid") 
        }
    }

    public void addFile(String scope, Path path) {
        addFile(scope, null, path)
    }

    private static Map flattenR(def item, String flatName="") {
        if (item instanceof Map) {
            Map flatMap = item.collectEntries { k, v ->
                flattenR(v, "${flatName}.${k}")
            }
            return flatMap
        } else if (item instanceof List) {
            Map flatListAsMap = item.indexed().collectEntries { i, v ->
                flattenR(v, "${flatName}.${i + 1}")
            }
            return flatListAsMap
        } else {
            String nameMinusInitialDot = flatName.substring(1)
            return [(nameMinusInitialDot): item]
        }
    }

    public static Map flattenMap(Map data) {
        return flattenR(data)
    }

    public String toJson() {
        Map outputMetadata = metadata
        if (flatten) {
            outputMetadata = flattenMap(outputMetadata)
        }

        return JsonOutput.toJson(["files": files, "metadata": outputMetadata])
    }

    public void write(Path path) {
        // Documentation for reading/writing to Nextflow files using this method is available at
        // https://www.nextflow.io/docs/latest/script.html#reading-and-writing
        path.withOutputStream {
            OutputStream outputStream = it as OutputStream
            if (path.extension == 'gz') {
                outputStream = new GZIPOutputStream(outputStream)
            }

            outputStream.write(JsonOutput.prettyPrint(toJson()).getBytes("utf-8"))
            outputStream.close()
        }
    }
}