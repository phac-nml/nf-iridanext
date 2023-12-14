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
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

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
import nextflow.Nextflow

import nextflow.iridanext.IridaNextJSONOutput

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
    private IridaNextJSONOutput iridaNextJSONOutput = new IridaNextJSONOutput()
    private Map<String,List<PathMatcher>> pathMatchers
    private List<PathMatcher> samplesMatchers
    private List<PathMatcher> globalMatchers
    private PathMatcher samplesMetadataMatcher
    private String filesMetaId
    private String samplesMetadataId
    private Path iridaNextOutputPath
    private Boolean outputFileOverwrite
    private Session session

    public IridaNextObserver() {
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
        this.session = session

        iridaNextOutputPath = session.config.navigate('iridanext.output.path') as Path
        if (iridaNextOutputPath != null) {
            iridaNextOutputPath = Nextflow.file(iridaNextOutputPath) as Path
        }
        outputFileOverwrite = session.config.navigate('iridanext.output.overwrite', false)

        Map<String,Object> iridaNextFiles = session.config.navigate('iridanext.output.files') as Map<String,Object>
        if (iridaNextFiles != null) {
            // Used for overriding the "meta.id" key used to define identifiers for a scope
            // (e.g., by default meta.id is used for a sample identifier in a pipeline)
            this.filesMetaId = iridaNextFiles?.idkey ?: "id"

            iridaNextFiles.each {scope, matchers ->
                // "id" is a special keyword and isn't used for file matchers
                if (scope != "idkey") {
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

        def iridaNextMetadata = session.config.navigate('iridanext.output.metadata')
        if (iridaNextMetadata != null) {
            if (!iridaNextMetadata instanceof Map<String,Object>) {
                throw new Exception("Expected a map in config for iridanext.metadata=${iridaNextMetadata}")
            }

            Map<String, String> samplesMetadata = iridaNextMetadata["samples"] as Map<String,String>
            samplesMetadataMatcher = FileSystems.getDefault().getPathMatcher("glob:${samplesMetadata['path']}")
            samplesMetadataId = samplesMetadata["id"]
        }
    }

    @Override
    void onFilePublish(Path destination, Path source) {
        if (publishedFiles.containsKey(source)) {
            throw new Exception("Error: file with source=${source} was already published")
        }

        publishedFiles[source] = destination
    }

    private Map<String, Object> csvToJsonById(Path path, String idColumn) {
        path = Nextflow.file(path) as Path
        List rowsList = path.splitCsv(header:true, strip:true, sep:',', quote:'\"')

        Map<String, Object> rowsMap = rowsList.collectEntries { row ->
            if (idColumn !in row) {
                throw new Exception("Error: column with idColumn=${idColumn} not in CSV ${path}")
            } else {
                return [(row[idColumn] as String): (row as Map).findAll { it.key != idColumn }]
            }
        }

        return rowsMap
    }

    private void processOutputPath(Path outputPath, Map<String,String> indexInfo) {
        if (publishedFiles.containsKey(outputPath)) {
            Path publishedPath = publishedFiles[outputPath]
            def currScope = indexInfo["scope"]
            
            if (pathMatchers[currScope].any {it.matches(publishedPath)}) {
                iridaNextJSONOutput.addFile(currScope, indexInfo["subscope"], publishedPath)
            }
        } else {
            log.trace "Not match outputPath: ${outputPath}"
        }
    }

    @Override
    void onFlowComplete() {
        if (!session.isSuccess())
            return

        // Generate files section
        // Some of this code derived from https://github.com/nextflow-io/nf-prov/blob/master/plugins/nf-prov
        tasks.each { task ->
            Map<Short,Map<String,String>> outParamInfo = [:]
            def currSubscope = null
            task.outputs.each { outParam, object -> 
                log.debug "task ${task}, outParam ${outParam}, object ${object}"
                Short paramIndex = outParam.getIndex()
                if (!outParamInfo.containsKey(paramIndex)) {
                    Map<String,String> currIndexInfo = [:]
                    outParamInfo[paramIndex] = currIndexInfo

                    // case meta map
                    if (outParam instanceof ValueOutParam && object instanceof Map) {
                        Map objectMap = (Map)object
                        if (outParam.getName() == "meta" && this.filesMetaId in objectMap) {
                            log.trace "${this.filesMetaId} in ${objectMap}"
                            currIndexInfo["scope"] = "samples"
                            currIndexInfo["subscope"] = objectMap[this.filesMetaId].toString()
                            iridaNextJSONOutput.addId(currIndexInfo["scope"], currIndexInfo["subscope"])
                        } else {
                            throw new Exception("Found value channel output in task [${task.getName()}] that doesn't have meta.${this.filesMetaId}: ${objectMap}")
                        }
                    } else {
                        currIndexInfo["scope"] = "global"
                        currIndexInfo["subscope"] = ""
                    }

                    log.debug "Setup info task [${task.getName()}], outParamInfo[${paramIndex}]: ${outParamInfo[paramIndex]}"
                }

                if (object instanceof Path) {
                    log.debug "outParamInfo[${paramIndex}]: ${outParamInfo[paramIndex]}, object as Path: ${object as Path}"
                    processOutputPath(object as Path, outParamInfo[paramIndex])
                } else if (object instanceof List) {
                    log.debug "outParamInfo[${paramIndex}]: ${outParamInfo[paramIndex]}, object as List: ${object as List}"
                    (object as List).each {
                        if (it instanceof Path) {
                            processOutputPath(it as Path, outParamInfo[paramIndex])
                        }
                    }
                }
            }
        }

        // Generate metadata section
        // some code derived from https://github.com/nextflow-io/nf-validation
        if (samplesMetadataMatcher != null && samplesMetadataId != null) {
            List matchedFiles = new ArrayList(publishedFiles.values().findAll {samplesMetadataMatcher.matches(it)})

            if (!matchedFiles.isEmpty()) {
                log.trace "Matched metadata: ${matchedFiles}"
                Map metadataSamplesMap = csvToJsonById(matchedFiles[0] as Path, samplesMetadataId)
                iridaNextJSONOutput.appendMetadata("samples", metadataSamplesMap)
            }
        }

        if (iridaNextOutputPath != null) {
            if (iridaNextOutputPath.exists() && !outputFileOverwrite) {
                throw new Exception("Error: iridanext.output.path=${iridaNextOutputPath} exists and iridanext.output.overwrite=${outputFileOverwrite}")
            } else {
                // Documentation for reading/writing to Nextflow files using this method is available at
                // https://www.nextflow.io/docs/latest/script.html#reading-and-writing
                iridaNextOutputPath.withOutputStream {
                    OutputStream outputStream = it as OutputStream
                    if (iridaNextOutputPath.extension == 'gz') {
                        outputStream = new GZIPOutputStream(outputStream)
                    }

                    outputStream.write(JsonOutput.prettyPrint(iridaNextJSONOutput.toJson()).getBytes("utf-8"))
                    outputStream.close()
                }
                log.debug "Wrote IRIDA Next output to ${iridaNextOutputPath}"
            }
        }
    }
}
