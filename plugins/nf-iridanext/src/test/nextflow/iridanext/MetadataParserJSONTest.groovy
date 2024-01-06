package nextflow.iridanext

import java.nio.file.FileSystems

import nextflow.iridanext.MetadataParser
import nextflow.iridanext.MetadataParserJSON
import spock.lang.Specification

import nextflow.iridanext.TestHelper

class MetadataParserJSONTest extends Specification {

    private static final jsonContent = '''{
                                    "1": {"b": "2", "c": "3"},
                                    "2": {"b": "3", "c": "4"}
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

    // def 'Test parse JSON file set ignore keys' () {
    //     when:
    //     def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContent)
    //     MetadataParserJSON parser = new MetadataParserJSON()
    //     parser.setIgnoreKeys("c")
    //     def outputData = parser.parseMetadata(jsonFile)

    //     then:
    //     outputData == [
    //         "1": ["b": "2"],
    //         "2": ["b": "3"]
    //     ]
    // }

    // def 'Test parse JSON file set keep keys' () {
    //     when:
    //     def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContent)
    //     MetadataParserJSON parser = new MetadataParserJSON()
    //     parser.setKeepKeys("b")
    //     def outputData = parser.parseMetadata(jsonFile)

    //     then:
    //     outputData == [
    //         "1": ["b": "2"],
    //         "2": ["b": "3"]
    //     ]
    // }
}