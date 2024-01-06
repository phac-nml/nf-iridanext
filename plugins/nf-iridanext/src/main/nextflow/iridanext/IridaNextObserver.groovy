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
import java.net.URI

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
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.SchemaStore

import nextflow.iridanext.IridaNextJSONOutput
import nextflow.iridanext.MetadataParser
import nextflow.iridanext.MetadataParserCSV
import nextflow.iridanext.MetadataParserJSON

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
    private IridaNextJSONOutput iridaNextJSONOutput
    private Map<String,List<PathMatcher>> pathMatchers
    private String filesMetaId
    private List<MetadataParser> samplesMetadataParsers = []
    private Path iridaNextOutputPath
    private Path outputFilesRootDir
    private Boolean outputFileOverwrite
    private Session session

    public IridaNextObserver() {
        pathMatchers = [:]
        addPathMatchers("global", [])
        addPathMatchers("samples", [])
    }

    public IridaNextJSONOutput getIridaNextJSONOutput() {
        return iridaNextJSONOutput
    }

    public List<MetadataParser> getSamplesMetadataParsers() {
        return samplesMetadataParsers
    }

    public String getFilesMetaId() {
        return filesMetaId
    }

    public addPathMatchers(String scope, List<PathMatcher> matchers) {
        if (pathMatchers.containsKey(scope)) {
            pathMatchers[scope].addAll(matchers)
        } else {
            pathMatchers[scope] = matchers
        }
    }

    public List<PathMatcher> getPathMatchers(String scope) {
        return pathMatchers[scope]
    }

    @Override
    void onProcessComplete(TaskHandler handler, TraceRecord trace) {
        log.trace "onProcessComplete: ${handler.task}"
        tasks << handler.task
        traces << trace
    }

    @Override
    void onProcessCached(TaskHandler handler, TraceRecord trace) {
        log.trace "onProcessCached: ${handler.task}"
        tasks << handler.task
        traces << trace
    }

    @Override
    void onFlowCreate(Session session) {
        this.session = session
        Path relativizePath = null

        Boolean relativizeOutputPaths = session.config.navigate('iridanext.output.relativize', true)

        iridaNextOutputPath = session.config.navigate('iridanext.output.path') as Path
        if (iridaNextOutputPath != null) {
            iridaNextOutputPath = Nextflow.file(iridaNextOutputPath) as Path
            if (relativizeOutputPaths) {
                relativizePath = iridaNextOutputPath.getParent()
            }
        }

        outputFileOverwrite = session.config.navigate('iridanext.output.overwrite', false)
        String jsonSchemaPath = session.config.navigate('iridanext.output.schema')
        Schema jsonSchema = null
        if (jsonSchemaPath != null) {
            SchemaStore schemaStore = new SchemaStore()
            Path jsonSchemaPathAsPath = Nextflow.file(jsonSchemaPath) as Path
            jsonSchema = schemaStore.loadSchema(jsonSchemaPathAsPath.toFile().toURI())
        }

        // Used for overriding the "meta.id" key used to define identifiers for a scope
        // (e.g., by default meta.id is used for a sample identifier in a pipeline)
        this.filesMetaId = session.config.navigate('iridanext.output.files.idkey', "id")

        def iridaNextFiles = session.config.navigate('iridanext.output.files')
        if (iridaNextFiles != null) {
            Map<String, Object> iridaNextFilesMap = iridaNextFiles as Map<String,Object>

            iridaNextFilesMap.each {scope, matchers ->
                // "idkey" is a special keyword and isn't used for file matchers
                if (scope != "idkey") {
                    if (matchers instanceof String) {
                        matchers = [matchers]
                    }

                    if (!(matchers instanceof List)) {
                        throw new Exception("Invalid configuration for iridanext.files=${iridaNextFilesMap}")
                    }

                    List<PathMatcher> matchersGlob = matchers.collect {FileSystems.getDefault().getPathMatcher("glob:${it}")}
                    addPathMatchers(scope, matchersGlob)
                }
            }
        }

        Boolean flattenMetadata = session.config.navigate('iridanext.output.metadata.flatten', false)
        def iridaNextMetadata = session.config.navigate('iridanext.output.metadata')
        if (iridaNextMetadata != null) {
            if (!iridaNextMetadata instanceof Map<String,Object>) {
                throw new Exception("Expected a map in config for iridanext.metadata=${iridaNextMetadata}")
            }

            Map<String, Object> samplesMetadata = iridaNextMetadata["samples"] as Map<String,Object>
            this.samplesMetadataParsers = samplesMetadata.collect { type, parserConfig ->
                if (!(parserConfig instanceof Map<String, String>)) {
                    throw new Exception("Invalid config for iridanext.output.metadata.samples: ${samplesMetadata}")
                }

                Map<String, String> parserConfigMap = parserConfig as Map<String, String>
                PathMatcher pathMatcher = createPathMatcher(parserConfigMap?.path)
                String sep = parserConfigMap.get("sep", ",")

                if (type == "csv") {
                    return new MetadataParserCSV(parserConfigMap?.idcol, sep, pathMatcher) as MetadataParser
                } else if (type == "json") {
                    return new MetadataParserJSON(pathMatcher)
                } else {
                    throw new Exception("Invalid config for iridanext.output.metadata.samples: ${samplesMetadata}")
                }
            }
        }

        iridaNextJSONOutput = new IridaNextJSONOutput(relativizePath, flattenMetadata, jsonSchema)
    }

    private PathMatcher createPathMatcher(String pathMatch) {
        if (pathMatch == null) {
            return null
        } else {
            return FileSystems.getDefault().getPathMatcher("glob:${pathMatch}")
        }
    }

    @Override
    void onFilePublish(Path destination, Path source) {
        if (publishedFiles.containsKey(source)) {
            throw new Exception("Error: file with source=${source} was already published")
        }

        publishedFiles[source] = destination
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
                            throw new Exception("iridanext.output.files.idkey=${filesMetaId}, but output for task [${task.getName()}] has map ${outParam.getName()} missing key ${filesMetaId}: ${objectMap}")
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
        samplesMetadataParsers.each { metadataMatcher -> 
            Map samplesMetadataMap = metadataMatcher.matchAndParseMetadata(publishedFiles.values())
            iridaNextJSONOutput.appendMetadata("samples", samplesMetadataMap)
        }

        if (iridaNextOutputPath != null) {
            if (iridaNextOutputPath.exists() && !outputFileOverwrite) {
                throw new Exception("Error: iridanext.output.path=${iridaNextOutputPath} exists and iridanext.output.overwrite=${outputFileOverwrite}")
            } else {
                iridaNextJSONOutput.write(iridaNextOutputPath)
                log.debug "Wrote IRIDA Next output to ${iridaNextOutputPath}"
            }
        }
    }
}
