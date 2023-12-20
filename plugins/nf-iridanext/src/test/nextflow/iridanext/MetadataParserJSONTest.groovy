package nextflow.iridanext

import java.nio.file.FileSystems

import nextflow.iridanext.MetadataParser
import nextflow.iridanext.MetadataParserJSON
import spock.lang.Specification

import nextflow.iridanext.TestHelper

class MetadataParserJSONTest extends Specification {

    def 'Test parse JSON file' () {
        when:
        String jsonContent = '''{
                                    "1": {"b": "2", "c": "3"},
                                    "2": {"b": "3", "c": "4"}
                                }'''.stripMargin()
        def jsonFile = TestHelper.createInMemTempFile("temp.json", jsonContent)
        def parser = new MetadataParserJSON("a")
        def outputData = parser.parseMetadata(jsonFile)

        then:
        outputData == [
            "1": ["b": "2", "c": "3"],
            "2": ["b": "3", "c": "4"]
        ]
    }
}