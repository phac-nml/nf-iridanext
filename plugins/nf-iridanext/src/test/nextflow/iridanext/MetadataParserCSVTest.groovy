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
        def csvMap = parser.parseMetadata(csvFile)
        then:
        csvMap != null
    }
}