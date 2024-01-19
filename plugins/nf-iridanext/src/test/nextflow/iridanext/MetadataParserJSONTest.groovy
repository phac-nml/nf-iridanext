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

    def 'Test parse JSON file complex' () {
        when:
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContentComplex)
        def parser = new MetadataParserJSON()
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["coords": ["x": 2, "y": 8], "coords.x": 3],
            "2": ["coords": ["x": 0, "y": 1], "coords.x": 4]
        ]
    }
}