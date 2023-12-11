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
import groovy.transform.CompileStatic
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class IridaNextOutput {
    private Map files = ["global": [], "samples": [:]]
    private Map metadata = ["samples": [:]]
    // private final Map<String, List<Map<Object, Object>>> files = ["global": [], "samples": []]
    // private final Map<String, Map<Object, Object>> metadata = ["samples": []]

    public void appendMetadata(String key, Map value) {
        metadata[key] = ((Map)metadata[key]) +  value
    }

    public void addFile(String scope, String subscope, Path path) {
        if (!(scope in files.keySet())) {
            throw new Exception("scope=${scope} not in valid set of scopes: ${files.keySet()}")
        }

        // Treat empty string and null as same
        if (subscope == "") {
            subscope = null
        }

        def files_scope = files[scope]
        if (scope == "samples" && subscope == null) {
            throw new Exception("scope=${scope} but subscope is null")
        } else if (scope == "samples" && subscope != null) {
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

    public String toJson() {
        return JsonOutput.toJson(["files": files, "metadata": metadata])
    }
}