package nextflow.iridanext

import java.nio.file.Paths

import nextflow.iridanext.IridaNextJSONOutput
import spock.lang.Specification
import groovy.json.JsonSlurper

import nextflow.iridanext.TestHelper

class IridaNextJSONOutputTest extends Specification {

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
}