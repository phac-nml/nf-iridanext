package nextflow.iridanext

import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files

import nextflow.iridanext.IridaNextJSONOutput
import spock.lang.Specification
import groovy.json.JsonSlurper
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.SchemaStore
import net.jimblackler.jsonschemafriend.ValidationException

import nextflow.iridanext.TestHelper
import groovy.util.logging.Slf4j

@Slf4j
class IridaNextJSONOutputTest extends Specification {

    private static final String invalidJSONSchema = '''{
        "$schema": "http://json-schema.org/draft-07/schema",
        "type": "object",
        "properties": {
            "files": {
                "type": "integer"
            },
            "metadata": {
                "type": "object"
            }
        }
    }
    '''

    private static final String validJSONSchema = '''{
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

    def 'Test add ids' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput()
        iridaNextOutput.addId("samples", "1")

        then:
        iridaNextOutput.isValidId("samples", "1")
        !iridaNextOutput.isValidId("samples", "2")
        !iridaNextOutput.isValidId("global", "1")
    }

    def 'Test add global files' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput()
        iridaNextOutput.addFile("global", Paths.get("sample1.fasta"))
        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parseText(iridaNextOutput.toJson())

        then:
        output == [
            "files": [
                "global": [
                    ["path": "sample1.fasta"]
                ],
                "samples": [:]
            ],
            "metadata": [
                "samples": [:]
            ]
        ]
    }

    def 'Test add sample files' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput()
        iridaNextOutput.addId("samples", "1")
        iridaNextOutput.addFile("samples", "1", Paths.get("sample1.fasta"))
        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parseText(iridaNextOutput.toJson())

        then:
        output == [
            "files": [
                "global": [],
                "samples": [
                    "1": [["path": "sample1.fasta"]],
                ],
            ],
            "metadata": [
                "samples": [:]
            ]
        ]
    }

    def 'Test add metadata' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput()
        iridaNextOutput.addId("samples", "1")
        iridaNextOutput.appendMetadata("samples", [
            "1": [
                "colour": "blue",
                "size": "large"
            ]
        ])
        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parseText(iridaNextOutput.toJson())

        then:
        output == [
            "files": [
                "global": [],
                "samples": [:],
            ],
            "metadata": [
                "samples": [
                    "1": [
                        "colour": "blue",
                        "size": "large"
                    ]
                ]
            ]
        ]
    }

    def 'Test add metadata missing id' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput()
        iridaNextOutput.appendMetadata("samples", [
            "1": [
                "colour": "blue",
                "size": "large"
            ]
        ])
        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parseText(iridaNextOutput.toJson())

        then:
        output == [
            "files": [
                "global": [],
                "samples": [:],
            ],
            "metadata": [
                "samples": [:]
            ]
        ]
    }

    def 'Test merge metadata' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput()
        iridaNextOutput.addId("samples", "1")
        iridaNextOutput.addId("samples", "2")
        iridaNextOutput.appendMetadata("samples", [
            "1": [
                "colour": "blue",
                "size": "large"
            ]
        ])
        iridaNextOutput.appendMetadata("samples", [
            "2": [
                "colour": "red",
                "size": "medium"
            ]
        ])
        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parseText(iridaNextOutput.toJson())

        then:
        output == [
            "files": [
                "global": [],
                "samples": [:],
            ],
            "metadata": [
                "samples": [
                    "1": [
                        "colour": "blue",
                        "size": "large"
                    ],
                    "2": [
                        "colour": "red",
                        "size": "medium"
                    ]
                ]
            ]
        ]
    }

    def 'Test flatten map already flat' () {
        when:
        Map flatMap = IridaNextJSONOutput.flattenMap([
            "key1": "value1",
            "key2": "value2"
        ])

        then:
        flatMap == [
            "key1": "value1",
            "key2": "value2"
        ]
    }

    def 'Test flatten map' () {
        when:
        Map flatMap = IridaNextJSONOutput.flattenMap([
            "key1": "value1",
            "key2": ["a": "value2", "b": "value3"],
            "key3": ["value4", "value5"]
        ])

        then:
        flatMap == [
            "key1": "value1",
            "key2.a": "value2",
            "key2.b": "value3",
            "key3.1": "value4",
            "key3.2": "value5"
        ]
    }

    def 'Test flatten metadata' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput(null, true)
        iridaNextOutput.addId("samples", "1")
        iridaNextOutput.appendMetadata("samples", [
            "1": [
                "colour": "blue",
                "sizes": ["small", "medium", "large"],
                "keys": ["a": "1", "b": "2"]
            ]
        ])
        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parseText(iridaNextOutput.toJson())

        then:
        output == [
            "files": [
                "global": [],
                "samples": [:],
            ],
            "metadata": [
                "samples": [
                    "1": [
                        "colour": "blue",
                        "sizes.1": "small",
                        "sizes.2": "medium",
                        "sizes.3": "large",
                        "keys.a": "1",
                        "keys.b": "2"
                    ]
                ]
            ]
        ]
    }

    def 'Test flatten metadata multiple samples' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput(null, true)
        iridaNextOutput.addId("samples", "1")
        iridaNextOutput.addId("samples", "2")
        iridaNextOutput.appendMetadata("samples", [
            "1": [
                "colour": "blue",
                "keys": ["a": "1", "b": "2"]
            ],
            "2": [
                "colour": "red",
                "keys": ["a": "3", "b": "4"]
            ]
        ])
        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parseText(iridaNextOutput.toJson())

        then:
        output == [
            "files": [
                "global": [],
                "samples": [:],
            ],
            "metadata": [
                "samples": [
                    "1": [
                        "colour": "blue",
                        "keys.a": "1",
                        "keys.b": "2"
                    ],
                    "2": [
                        "colour": "red",
                        "keys.a": "3",
                        "keys.b": "4"
                    ]
                ]
            ]
        ]
    }

    def 'Test relativize paths' () {
        when:
        def testFile = Paths.get("/tmp/sample1.fasta")

        def iridaNextOutputNoRelativize = new IridaNextJSONOutput()
        iridaNextOutputNoRelativize.addFile("global", testFile)

        def iridaNextOutputRelativize = new IridaNextJSONOutput(Paths.get("/tmp"))
        iridaNextOutputRelativize.addFile("global", testFile)

        def iridaNextOutputRelativizeOther = new IridaNextJSONOutput(Paths.get("/data"))
        iridaNextOutputRelativizeOther.addFile("global", testFile)
        
        def jsonSlurper = new JsonSlurper()
        def outputNoRelativize = jsonSlurper.parseText(iridaNextOutputNoRelativize.toJson())
        def outputRelativize = jsonSlurper.parseText(iridaNextOutputRelativize.toJson())
        def outputRelativizeOther = jsonSlurper.parseText(iridaNextOutputRelativizeOther.toJson())

        then:
        outputNoRelativize == [
            "files": [
                "global": [
                    ["path": "/tmp/sample1.fasta"]
                ],
                "samples": [:]
            ],
            "metadata": [
                "samples": [:]
            ]
        ]

        outputRelativize == [
            "files": [
                "global": [
                    ["path": "sample1.fasta"]
                ],
                "samples": [:]
            ],
            "metadata": [
                "samples": [:]
            ]
        ]

        outputRelativizeOther == [
            "files": [
                "global": [
                    ["path": "../tmp/sample1.fasta"]
                ],
                "samples": [:]
            ],
            "metadata": [
                "samples": [:]
            ]
        ]
    }

    def 'Test validate output against in-memory schema' () {
        when:
        def schemaStore = new SchemaStore()
        Schema schema = schemaStore.loadSchemaJson(validJSONSchema)
        IridaNextJSONOutput iridaNextOutput = new IridaNextJSONOutput(null, false, schema)

        then:
        iridaNextOutput.validateJson(iridaNextOutput.toJson())
    }

    def 'Test failure with mismatch of schema' () {
        when:
        def schemaStore = new SchemaStore()
        Schema schema = schemaStore.loadSchemaJson(invalidJSONSchema)
        IridaNextJSONOutput iridaNextOutput = new IridaNextJSONOutput(null, false, schema)
        iridaNextOutput.validateJson(iridaNextOutput.toJson())

        then:
        thrown(ValidationException)
    }

    def 'Test validate JSON against packaged schema success' () {
        when:
        def schemaStore = new SchemaStore()
        Schema schema = IridaNextJSONOutput.defaultSchema
        IridaNextJSONOutput iridaNextOutput = new IridaNextJSONOutput(null, false, schema)

        then:
        iridaNextOutput.validateJson('{"files": {}, "metadata": {}}')
    }

    def 'Test validate JSON against packaged schema fail' () {
        when:
        def schemaStore = new SchemaStore()
        Schema schema = IridaNextJSONOutput.defaultSchema
        IridaNextJSONOutput iridaNextOutput = new IridaNextJSONOutput(null, false, schema)
        iridaNextOutput.validateJson('{"files": "string", "metadata": {}}')

        then:
        thrown(ValidationException)
    }

    def 'Test validate output against packaged schema success' () {
        when:
        def schemaStore = new SchemaStore()
        Schema schema = IridaNextJSONOutput.defaultSchema
        IridaNextJSONOutput iridaNextOutput = new IridaNextJSONOutput(null, false, schema)

        then:
        iridaNextOutput.validateJson(iridaNextOutput.toJson())
    }

    def 'Test write JSON file' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput()
        Path tempJsonFile = Files.createTempFile("iridanext.output", ".json")
        iridaNextOutput.write(tempJsonFile)

        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parse(tempJsonFile)

        then:
        output == [
            "files": [
                "global": [],
                "samples": [:]
            ],
            "metadata": [
                "samples": [:]
            ]
        ]
    }

    def 'Test write complex JSON file' () {
        when:
        def iridaNextOutput = new IridaNextJSONOutput()
        iridaNextOutput.addId("samples", "1")
        iridaNextOutput.addFile("global", Paths.get("sample1.fasta"))
        iridaNextOutput.appendMetadata("samples", [
            "1": [
                "colour": "blue",
                "size": "large"
            ]
        ])
        Path tempJsonFile = Files.createTempFile("iridanext.output", ".json")
        iridaNextOutput.write(tempJsonFile)

        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parse(tempJsonFile)

        then:
        output == [
            "files": [
                "global": [
                    ["path": "sample1.fasta"]
                ],
                "samples": [:]
            ],
            "metadata": [
                "samples": [
                    "1": [
                        "colour": "blue",
                        "size": "large"
                    ]
                ]
            ]
        ]
    }

    def 'Test failure to write with mismatch of schema' () {
        when:
        def schemaStore = new SchemaStore()
        Schema schema = schemaStore.loadSchemaJson(invalidJSONSchema)
        IridaNextJSONOutput iridaNextOutput = new IridaNextJSONOutput(null, false, schema, true)
        Path tempJsonFile = Files.createTempFile("iridanext.output", ".json")
        iridaNextOutput.write(tempJsonFile)

        then:
        thrown(ValidationException)
    }

    def 'Test ignore validation with mismatch of schema' () {
        when:
        def schemaStore = new SchemaStore()
        Schema schema = schemaStore.loadSchemaJson(invalidJSONSchema)
        IridaNextJSONOutput iridaNextOutput = new IridaNextJSONOutput(null, false, schema, false)
        Path tempJsonFile = Files.createTempFile("iridanext.output", ".json")
        iridaNextOutput.write(tempJsonFile)

        def jsonSlurper = new JsonSlurper()
        def output = jsonSlurper.parse(tempJsonFile)

        then:
        output == [
            "files": [
                "global": [],
                "samples": [:]
            ],
            "metadata": [
                "samples": [:]
            ]
        ]
    }
}