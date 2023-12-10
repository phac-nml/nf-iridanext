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
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.json.JsonOutput
import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.processor.TaskHandler
import nextflow.trace.TraceRecord
import nextflow.processor.TaskRun
import nextflow.script.params.FileOutParam
import nextflow.script.params.ValueOutParam

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
    private List<PathMatcher> samplesMatchers
    private List<PathMatcher> globalMatchers

    public HelloObserver() {
        samplesMatchers = [FileSystems.getDefault().getPathMatcher("glob:**/*.assembly.fa.gz")]
    }

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
        log.info "Pipeline complete!"

        // Some of this code derived from https://github.com/nextflow-io/nf-prov/blob/master/plugins/nf-prov
        tasks.each { task ->
            Map<Short,Map<String,String>> outParamInfo = [:]
            def currSubscope = null
            def currScope = "global"
            log.info "\n****\ntask: ${task.getName()}"
            log.info "task.outputs (${task.outputs.getClass()}): ${task.outputs}"
            task.outputs.each { outParam, object -> 
                log.info "outParam (${outParam.getClass()}): ${outParam}"
                log.info "outParm info: name=${outParam.getName()}, index=${outParam.getIndex()}, emitName=${outParam.getChannelEmitName()}"
                log.info "object class: ${object.getClass()}, instanceof map ${object instanceof Map}"
                log.info "object ${object}"
                Short paramIndex = outParam.getIndex()
                if (!outParamInfo.containsKey(paramIndex)) {
                    Map<String,String> currIndexInfo = [:]
                    outParamInfo[paramIndex] = currIndexInfo

                    // case meta map
                    if (outParam instanceof ValueOutParam && object instanceof Map) {
                        Map objectMap = (Map)object
                        if (outParam.getName() == "meta" && "id" in objectMap) {
                            currIndexInfo["scope"] = "samples"
                            currIndexInfo["subscope"] = objectMap["id"].toString()
                        } else {
                            throw new Exception("Found value channel output that doesn't have meta.id: ${objectMap}")
                        }
                    } else {
                        currIndexInfo["scope"] = "global"
                        currIndexInfo["subscope"] = ""
                    }
                }

                Map<String,String> currIndexInfo = outParamInfo[paramIndex]

                if (object instanceof Path) {
                    Path path = (Path)object
                    if (samplesMatchers.any {it.matches(path)}) {
                        iridaNextOutput.addFile(currIndexInfo["scope"], currIndexInfo["subscope"], path)
                    }
                }
            }
        }

        log.info "JSON: ${JsonOutput.prettyPrint(iridaNextOutput.toJson())}"
    }
}
