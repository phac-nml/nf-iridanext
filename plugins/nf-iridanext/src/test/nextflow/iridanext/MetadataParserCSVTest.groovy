package nextflow.iridanext

import java.nio.file.FileSystems

import nextflow.iridanext.MetadataParser
import nextflow.iridanext.MetadataParserCSV
import spock.lang.Specification
import spock.lang.Ignore

import nextflow.iridanext.TestHelper

class MetadataParserCSVTest extends Specification {

    def 'Test parse CSV file' () {
        when:
        def csvContent = """a,b,c
                           |1,2,3
                           |4,5,6""".stripMargin()
        def csvFile = TestHelper.createInMemTempFile("temp.csv", csvContent)
        def parser = new MetadataParserCSV("a", ",")
        def csvMapColA = parser.parseMetadata(csvFile)

        parser = new MetadataParserCSV("b", ",")
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

    def 'Test parse CSV file alternative separator' () {
        when:
        def csvContent = """a;b;c
                           |1;2;3
                           |4;5;6""".stripMargin()
        def csvFile = TestHelper.createInMemTempFile("temp.csv", csvContent)
        def parser = new MetadataParserCSV("a", ";")
        def csvMap = parser.parseMetadata(csvFile)

        then:
        csvMap == [
            "1": ["b": "2", "c": "3"],
            "4": ["b": "5", "c": "6"]
        ]
    }

    def 'Test matchAndParse CSV file' () {
        when:
        def csvContent = """a,b,c
                           |1,2,3
                           |4,5,6""".stripMargin()
        def csvFile = TestHelper.createInMemTempFile("tempMatchAndParse.csv", csvContent)
        def pathMatcherMatch = FileSystems.getDefault().getPathMatcher("glob:**/tempMatchAndParse.csv")
        def parserMatch = new MetadataParserCSV("a", ",", pathMatcherMatch)
        def csvMapMatch = parserMatch.matchAndParseMetadata([csvFile])

        def pathMatcherUnmatch = FileSystems.getDefault().getPathMatcher("glob:**/tempMatchAndParse-unmatch.csv")
        def parserUnmatch = new MetadataParserCSV("a", ",", pathMatcherUnmatch)
        def csvMapUnmatch = parserUnmatch.matchAndParseMetadata([csvFile])

        then:
        csvMapMatch == [
            "1": ["b": "2", "c": "3"],
            "4": ["b": "5", "c": "6"]
        ]
        csvMapUnmatch == [:]
    }

    def 'Test parse CSV file with missing values' () {
        when:
        def csvContent = """a,b,c
                           |1,2,
                           |4,,""".stripMargin()
        def csvFile = TestHelper.createInMemTempFile("temp.csv", csvContent)
        def parser = new MetadataParserCSV("a", ",")
        def csvMapColA = parser.parseMetadata(csvFile)

        then:
        csvMapColA == [
            "1": ["b": "2", "c": ""],
            "4": ["b": "", "c": ""]
        ]
    }

    @Ignore
    def 'Test parse CSV file with missing ids' () {
        when:
        def csvContent = """a,b,c
                           |1,2,
                           |4,,6""".stripMargin()
        def csvFile = TestHelper.createInMemTempFile("temp.csv", csvContent)

        parser = new MetadataParserCSV("b", ",")
        def csvMapColB = parser.parseMetadata(csvFile)

        then:
        csvMapColB == [
            "2": ["a": "1", "c": ""]
        ]
    }
}