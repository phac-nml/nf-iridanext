package nextflow.iridanext

import java.nio.file.Paths
import java.nio.file.FileSystems
import nextflow.iridanext.IridaNextObserver
import nextflow.iridanext.MetadataParserCSV

import nextflow.Session
import spock.lang.Specification
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.SchemaStore
import groovy.util.logging.Slf4j

import nextflow.iridanext.TestHelper

@Slf4j
class IridaNextObserverTest extends Specification {

    def 'Test relativize paths' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    path: "/tmp/output/output.json.gz",
                    relativize: true
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        iridaNextObserver.getIridaNextJSONOutput().getShouldRelativize()
        iridaNextObserver.getIridaNextJSONOutput().getRelativizePath() == Paths.get("/tmp/output")
    }

    def 'Test no relativize paths' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    path: "/tmp/output/output.json.gz",
                    relativize: false
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        !iridaNextObserver.getIridaNextJSONOutput().getShouldRelativize()
        iridaNextObserver.getIridaNextJSONOutput().getRelativizePath() == null
    }

    def 'Test configure files section' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    files: [
                        idkey: "id1",
                        global: ["**/file1.txt", "**/fastq/file2.fastq"],
                        samples: ["**/assembly/*.contigs.fa.gz"]
                    ]
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        def matcher = { it ->
            FileSystems.getDefault().getPathMatcher(it)
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        iridaNextObserver.getFilesMetaId() == "id1"
        iridaNextObserver.getPathMatchers("global").any {it.matches(Paths.get("folder/file1.txt"))}
        iridaNextObserver.getPathMatchers("global").any {it.matches(Paths.get("/fastq/file2.fastq"))}
        !iridaNextObserver.getPathMatchers("global").any {it.matches(Paths.get("/fastq/not/file2.fastq"))}
        iridaNextObserver.getPathMatchers("samples").any {it.matches(Paths.get("/assembly/sam.contigs.fa.gz"))}
    }

    def 'Test metadata parsing CSV' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    metadata: [
                        flatten: true,
                        ignore: ["col2"],
                        keep: ["col2", "col3"],
                        rename: ["col2": "column_2"],
                        samples: [
                            csv: [
                                path: "**/output.csv",
                                idcol: "col1"
                            ]
                        ]
                    ]
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        IridaNextJSONOutput jsonOutput = iridaNextObserver.getIridaNextJSONOutput()
        jsonOutput.getMetadataPostProcessor().getFlatten()
        jsonOutput.getMetadataPostProcessor().getIgnoreKeys() == ["col2"].toSet()
        jsonOutput.getMetadataPostProcessor().getKeepKeys() == ["col2", "col3"].toSet()
        jsonOutput.getMetadataPostProcessor().getRenameKeys() == ["col2": "column_2"]
        iridaNextObserver.getSamplesMetadataParsers().size() == 1
        iridaNextObserver.getSamplesMetadataParsers()[0] instanceof MetadataParserCSV
        MetadataParserCSV metadataParser = iridaNextObserver.getSamplesMetadataParsers()[0] as MetadataParserCSV
        metadataParser.getIdCol() == "col1"
        metadataParser.getSep() == ","
    }

    def 'Test metadata parsing JSON' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    metadata: [
                        flatten: false,
                        ignore: ["k2"],
                        keep: ["k2", "k3"],
                        rename: ["k2": "key_2"],
                        samples: [
                            json: [
                                path: "**/output.json"
                            ]
                        ]
                    ]
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        IridaNextJSONOutput jsonOutput = iridaNextObserver.getIridaNextJSONOutput()
        !jsonOutput.getMetadataPostProcessor().getFlatten()
        jsonOutput.getMetadataPostProcessor().getIgnoreKeys() == ["k2"].toSet()
        jsonOutput.getMetadataPostProcessor().getKeepKeys() == ["k2", "k3"].toSet()
        jsonOutput.getMetadataPostProcessor().getRenameKeys() == ["k2": "key_2"]
        iridaNextObserver.getSamplesMetadataParsers().size() == 1
        iridaNextObserver.getSamplesMetadataParsers()[0] instanceof MetadataParserJSON
    }

    def 'Test setting JSON schema to validate' () {
        when:
        String schemaString = '''{
            "$schema": "http://json-schema.org/draft-07/schema",
            "type": "object",
            "properties": {
                "files": {
                    "type": "object"
                },
                "metadata": {
                    "type": "object"
                }
            }
        }
        '''
        def schemaFile = TestHelper.createInMemTempFile("test_schema.json", schemaString)
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    schema: "${schemaFile}",
                    validate: true
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        iridaNextObserver.getIridaNextJSONOutput().shouldValidate()
        iridaNextObserver.getIridaNextJSONOutput().getOutputSchema().getUri().toString().endsWith("test_schema.json")
    }

    def 'Test disable JSON schema validation' () {
        when:
        String schemaString = '''{
            "$schema": "http://json-schema.org/draft-07/schema",
            "type": "object",
            "properties": {
                "files": {
                    "type": "object"
                },
                "metadata": {
                    "type": "object"
                }
            }
        }
        '''
        def schemaFile = TestHelper.createInMemTempFile("test_schema.json", schemaString)
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    schema: "${schemaFile}",
                    validate: false
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        !iridaNextObserver.getIridaNextJSONOutput().shouldValidate()
    }

    def 'Test default JSON schema to validate' () {
        when:
        String schemaString = '''{
            "$schema": "http://json-schema.org/draft-07/schema",
            "type": "object",
            "properties": {
                "files": {
                    "type": "object"
                },
                "metadata": {
                    "type": "object"
                }
            }
        }
        '''
        def schemaFile = TestHelper.createInMemTempFile("test_schema.json", schemaString)
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    validate: true
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        iridaNextObserver.getIridaNextJSONOutput().shouldValidate()
        iridaNextObserver.getIridaNextJSONOutput().getOutputSchema().getUri().toString().endsWith("output_schema.json")
    }
}
