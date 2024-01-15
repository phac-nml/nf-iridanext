package nextflow.iridanext

import java.nio.file.FileSystems

import nextflow.iridanext.MetadataPostProcessor
import spock.lang.Specification
import spock.lang.Ignore

import nextflow.iridanext.TestHelper

class MetadataPostProcessorTest extends Specification {

    private static final simpleMetadata = [
        "1": [
            "b": "2",
            "c": "3"
        ],
        "2": [
            "b": "3",
            "c": "4"
        ]
    ]

    private static final complexMetadata = [
        "1": [
            "coords": [
                "x": 2,
                "y": 8
            ],
            "coords.x": 3
        ],
        "2": [
            "coords": [
                "x": 0,
                "y": 1
            ],
            "coords.x": 4
        ],
    ]

    def 'Test simple post process' () {
        when:
        MetadataPostProcessor processor = new MetadataPostProcessor()
        def outputData = processor.process(simpleMetadata)

        then:
        outputData == [
            "1": [
                "b": "2",
                "c": "3"
            ],
            "2": [
                "b": "3",
                "c": "4"
            ]
        ]
    }

    def 'Test complex post process' () {
        when:
        MetadataPostProcessor processor = new MetadataPostProcessor()
        def outputData = processor.process(complexMetadata)

        then:
        outputData == [
            "1": [
                "coords": [
                    "x": 2,
                    "y": 8
                ],
                "coords.x": 3
            ],
            "2": [
                "coords": [
                    "x": 0,
                    "y": 1
                ],
                "coords.x": 4
            ],
        ]
    }

    def 'Test simple ignore simple keys' () {
        when:
        MetadataPostProcessor processor = new MetadataPostProcessor()
        processor.setIgnoreKeys(["c"])
        def outputData = processor.process(simpleMetadata)

        then:
        outputData == [
            "1": [
                "b": "2"
            ],
            "2": [
                "b": "3"
            ]
        ]
    }

    def 'Test simple keep simple keys' () {
        when:
        MetadataPostProcessor processor = new MetadataPostProcessor()
        processor.setKeepKeys(["c"])
        def outputData = processor.process(simpleMetadata)

        then:
        outputData == [
            "1": [
                "c": "3"
            ],
            "2": [
                "c": "4"
            ]
        ]
    }

    def 'Test simple rename keys' () {
        when:
        MetadataPostProcessor processor = new MetadataPostProcessor()
        processor.setRenameKeys(["b": "brename"])
        def outputData = processor.process(simpleMetadata)

        then:
        outputData == [
            "1": [
                "brename": "2",
                "c": "3"
            ],
            "2": [
                "brename": "3",
                "c": "4"
            ]
        ]
    }

    def 'Test complex ignore simple keys' () {
        when:
        MetadataPostProcessor processor = new MetadataPostProcessor()
        // "coords.x" is interpreted as the name of a single key and not a hierarchical key
        processor.setIgnoreKeys(["coords.x"])
        def outputData = processor.process(complexMetadata)

        then:
        outputData == [
            "1": [
                "coords": [
                    "x": 2,
                    "y": 8
                ],
            ],
            "2": [
                "coords": [
                    "x": 0,
                    "y": 1
                ],
            ],
        ]
    }

    def 'Test complex ignore hierarchical keys' () {
        when:
        MetadataPostProcessor processor = new MetadataPostProcessor()
        processor.setIgnoreKeys(["coords.x"])
        processor.setHierarchicalExpression(true)
        def outputData = processor.process(complexMetadata)

        then:
        outputData == [
            "1": [
                "coords": [
                    "y": 8
                ],
            ],
            "2": [
                "coords": [
                    "y": 1
                ],
            ],
        ]
    }
}