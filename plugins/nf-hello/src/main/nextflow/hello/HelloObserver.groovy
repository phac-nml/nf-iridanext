/*
 * Copyright 2021, Seqera Labs
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

package nextflow.hello

import java.nio.file.Path
import java.nio.file.Paths

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.json.JsonOutput
import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.processor.TaskHandler
import nextflow.trace.TraceRecord
import nextflow.processor.TaskRun
import nextflow.script.params.FileOutParam

import nextflow.hello.IridaNextOutput

/**
 * Example workflow events observer
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class HelloObserver implements TraceObserver {

    private List publishedFiles = []
    private List<TaskRun> tasks = []
    private List traces = []
    private IridaNextOutput iridaNextOutput = new IridaNextOutput()

    private String outputSectionName(FileOutParam outParam) {
        if (outParam.getName() == "meta") {
            return "samples"
        } else {
            return "global"
        }
    }

    @Override
    void onProcessComplete(TaskHandler handler, TraceRecord trace) {
        final task = handler.task
        tasks << task
        traces << trace
    }

    @Override
    void onFlowCreate(Session session) {
        log.info "Pipeline is starting! 🚀"
        log.info "session: ${session}"
        log.info "params: ${session.getParams()}"
    }

    @Override
    void onFilePublish(Path destination, Path source) {
        publishedFiles.push(["destination": destination, "source": source])
    }

    @Override
    void onFlowComplete() {
        log.info "Pipeline complete! 👋"
        log.info "Published files: ${publishedFiles}"
        log.info "Traces: ${traces[0]}"

        // Some of this code derived from https://github.com/nextflow-io/nf-prov/blob/master/plugins/nf-prov
        // def outputs = tasks.findResults { task ->
        //     task.outputs.findResults { outParam, object -> 
        //         def outputMap = [
        //             'task': task,
        //             'output_name': outParam.getName(),
        //             'output_object': object,
        //         ] 
        //         outputMap['name'] != '$' ? outputMap : null
        //     }
        // }
        tasks.each { task ->
            def currSubscope = null
            def currScope = "global"
            task.outputs.each { outParam, object -> 
                log.info "object class: ${object.getClass()}, instanceof map ${object instanceof Map}"
                log.info "object ${object}"
                if (object instanceof Map) {
                    Map objectMap = (Map)object
                    log.info "object=${object} is map, name=${outParam.getName()}, id in map ${'id' in objectMap}"
                    if (outParam.getName() == "meta" && "id" in objectMap) {
                        currSubscope = objectMap["id"].toString()
                        currScope = "samples"
                        log.info "currSubscope=${currSubscope}"
                    }
                } else if (object instanceof Path) {
                    Path path = (Path)object
                    iridaNextOutput.addFile(currScope, currSubscope, path)
                }
                // def outputMap = [
                //     'task': task,
                //     'output_name': outParam.getName(),
                //     'output_object': object,
                // ] 
                // outputMap['name'] != '$' ? outputMap : null
            }
        }

        log.info "Tasks: ${tasks[0]}"
        log.info "JSON: ${JsonOutput.prettyPrint(iridaNextOutput.toJson())}"
        // log.info "Outputs: ${outputs[0]}"
        // log.info "Tasks.outputs: ${tasks[0].outputs}"
    }
}
