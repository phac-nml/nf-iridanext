package nextflow.iridanext

import nextflow.iridanext.MetadataParser
import nextflow.iridanext.MetadataParserCSV
import spock.lang.Specification

import nextflow.iridanext.TestHelper

class MetadataParserCSVTest extends Specification {

    def 'Test parse CSV' () {
        when:
        String csvContent = """a,b,c
                              |1,2,3
                              |4,5,6""".stripMargin()
        def csvFile = TestHelper.createInMemTempFile("temp.csv", csvContent)
        def parser = new MetadataParserCSV("a")
        def csvMapColA = parser.parseMetadata(csvFile)

        parser = new MetadataParserCSV("b")
        def csvMapColB = parser.parseMetadata(csvFile)
        then:
        csvMapColA == [
            "1": ["b": "2", "c": "3"],
            "4": ["b": "5", "c": "6"]
        ]
        csvMapColB == [
            "2": ["a": "1", "c": "3"],
            "5": ["a": "4", "c": "6"]
        ]
    }
}