package nextflow.iridanext

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Path

import groovy.transform.CompileStatic

import nextflow.iridanext.MetadataParser
import nextflow.iridanext.MetadataParserCSV


class MetadataParserFactory {

    public static MetadataParser createMetadataParser(String type, PathMatcher pathMatcher, String id) {
        if (type == "csv") {
            return MetadataParserCSV(pathMatcher, id)
        } else {
            throw new Exception("Invalid type=${type} for MetadataParser")
        }
    }
}