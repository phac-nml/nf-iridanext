package nextflow.iridanext

import java.nio.file.FileSystems

import nextflow.iridanext.MetadataParser
import nextflow.iridanext.MetadataParserJSON
import spock.lang.Specification
import spock.lang.Ignore

import nextflow.iridanext.TestHelper

class MetadataParserJSONTest extends Specification {

    private static final jsonContent = '''{
                                    "1": {"b": "2", "c": "3"},
                                    "2": {"b": "3", "c": "4"}
                                }'''.stripMargin()

    private static final jsonContentComplex = '''{
                                    "1": {"coords": {"x": 2, "y": 8}, "coords.x": 3},
                                    "2": {"coords": {"x": 0, "y": 1}, "coords.x": 4}
                                }'''.stripMargin()

    def 'Test parse JSON file' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContent)
        def parser = new MetadataParserJSON()
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["b": "2", "c": "3"],
            "2": ["b": "3", "c": "4"]
        ]
    }

    def 'Test parse JSON file set ignore keys' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContent)
        MetadataParserJSON parser = new MetadataParserJSON()
        parser.setIgnoreKeys(["c"])
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["b": "2"],
            "2": ["b": "3"]
        ]
    }

    def 'Test parse JSON file set keep keys' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContent)
        MetadataParserJSON parser = new MetadataParserJSON()
        parser.setKeepKeys(["b"])
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["b": "2"],
            "2": ["b": "3"]
        ]
    }

    def 'Test parse JSON file rename keys' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContent)
        MetadataParserJSON parser = new MetadataParserJSON()
        parser.setRenameKeys(["b": "brename"])
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["brename": "2", "c": "3"],
            "2": ["brename": "3", "c": "4"]
        ]
    }

    def 'Test parse JSON file complex' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContentComplex)
        MetadataParserJSON parser = new MetadataParserJSON()
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["coords": ["x": 2, "y": 8], "coords.x": 3],
            "2": ["coords": ["x": 0, "y": 1], "coords.x": 4]
        ]
    }

    def 'Test parse JSON file set complex ignore keys' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContentComplex)
        MetadataParserJSON parser = new MetadataParserJSON()
        // "coords.x" is interpreted as the name of a single key and not a hierarchical key
        parser.setIgnoreKeys(["coords.x"])
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["coords": ["x": 2, "y": 8]],
            "2": ["coords": ["x": 0, "y": 1]]
        ]
    }

    @Ignore
    def 'Test parse JSON file set complex ignore keys hierarchical' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContentComplex)
        MetadataParserJSON parser = new MetadataParserJSON()
        parser.setHierarchicalExpression(true)
        parser.setIgnoreKeys(["coords.x"])
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["coords": ["y": 8], "coords.x": 3],
            "2": ["coords": ["y": 1], "coords.x": 4]
        ]
    }

    @Ignore
    def 'Test parse JSON file set complex ignore keys hierarchical, change sep' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContentComplex)
        MetadataParserJSON parser = new MetadataParserJSON()
        parser.setHierarchicalExpression(true)
        parser.setIgnoreKeys(["coords__x"])
        parser.setHierarchicalSeparator('__')
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["coords": ["y": 8], "coords.x": 3],
            "2": ["coords": ["y": 1], "coords.x": 4]
        ]
    }
}