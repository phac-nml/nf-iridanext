package nextflow.hello

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

    public void addFile(String scope, String subscope, Path path) {
        if (!(scope in files.keySet())) {
            throw new Exception("scope=${scope} not in valid set of scopes: ${files.keySet()}")
        }

        log.info "Calling addFile(${scope}, ${subscope}, ${path})"
        def files_scope = files[scope]
        if (scope == "samples" && subscope == null) {
            throw new Exception("scope=${scope} but subscope is null")
        } else if (scope == "samples" && subscope != null) {
            def files_scope_map = (Map)files_scope
            if (!files_scope_map.containsKey(subscope)) {
                files_scope_map[subscope] = []
            }
            log.info "before"
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