package nextflow.iridanext

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import nextflow.iridanext.MetadataParser
import nextflow.iridanext.MetadataParserCSV

@Slf4j
@CompileStatic
class MetadataParserFactory {

    public static MetadataParser createMetadataParser(String type, PathMatcher pathMatcher, String id) {
        log.info "createMetadataParser, type=${type}, pathMatcher=${pathMatcher}, id=${id}"
        if (type == "csv") {
            return new MetadataParserCSV(pathMatcher, id)
        } else {
            throw new Exception("Invalid type=${type} for MetadataParser")
        }
    }
}