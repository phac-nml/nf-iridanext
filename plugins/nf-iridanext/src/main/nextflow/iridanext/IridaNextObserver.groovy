/*
 * Original file Copyright 2021, Seqera Labs (from nf-hello plugin template)
 * Modifications Copyright 2023, Government of Canada
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
 * IridaNext workflow observer
 *
 * @author Aaron Petkau <aaron.petkau@phac-aspc.gc.ca>
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class IridaNextObserver implements TraceObserver {

    private Map<Path,Path> publishedFiles = [:]
    private List<TaskRun> tasks = []
    private List traces = []
    private IridaNextOutput iridaNextOutput = new IridaNextOutput()
    private Map<String,List<PathMatcher>> pathMatchers
    private List<PathMatcher> samplesMatchers
    private List<PathMatcher> globalMatchers

    public HelloObserver() {
        pathMatchers = [:]
        addPathMatchers("global", [])
        addPathMatchers("samples", [])
    }

    public addPathMatchers(String scope, List<PathMatcher> matchers) {
        if (pathMatchers.containsKey(scope)) {
            pathMatchers[scope].addAll(matchers)
        } else {
            pathMatchers[scope] = matchers
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
        def iridaNextFiles = session.config.navigate('iridanext.files')
        if (iridaNextFiles != null) {
            if (!iridaNextFiles instanceof Map<String,Object>) {
                throw new Exception("Expected a map in config for iridanext.files=${iridaNextFiles}")
            }

            iridaNextFiles = (Map<String,Object>)iridaNextFiles
            iridaNextFiles.each {scope, matchers ->
                if (matchers instanceof String) {
                    matchers = [matchers]
                }

                if (!(matchers instanceof List)) {
                    throw new Exception("Invalid configuration for iridanext.files=${iridaNextFiles}")
                }

                List<PathMatcher> matchersGlob = matchers.collect {FileSystems.getDefault().getPathMatcher("glob:${it}")}
                addPathMatchers(scope, matchersGlob)
            }
        }
    }

    @Override
    void onFilePublish(Path destination, Path source) {
        if (publishedFiles.containsKey(source)) {
            throw new Exception("Error: file with source=${source} was already published")
        }

        publishedFiles[source] = destination
    }

    @Override
    void onFlowComplete() {
        // Some of this code derived from https://github.com/nextflow-io/nf-prov/blob/master/plugins/nf-prov
        tasks.each { task ->
            Map<Short,Map<String,String>> outParamInfo = [:]
            def currSubscope = null
            task.outputs.each { outParam, object -> 
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
                    Path processPath = (Path)object

                    if (publishedFiles.containsKey(processPath)) {
                        Path publishedPath = publishedFiles[processPath]
                        def currScope = currIndexInfo["scope"]
                        
                        if (pathMatchers[currScope].any {it.matches(publishedPath)}) {
                            iridaNextOutput.addFile(currScope, currIndexInfo["subscope"], publishedPath)
                        }
                    }
                }
            }
        }

        log.info "${JsonOutput.prettyPrint(iridaNextOutput.toJson())}"
    }
}
