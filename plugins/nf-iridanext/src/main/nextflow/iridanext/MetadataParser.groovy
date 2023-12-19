package nextflow.iridanext

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class MetadataParser {
    private PathMatcher pathMather
    private String id

    public MetadataParser(PathMatcher pathMatcher, String id) {
        this.pathMatcher = pathMatcher
        this.id = id
    }

    public Map<String, Object> parseMetadata(Path path) {
        return [:]
    }

    public Map<String, Object> matchAndParseMetadata(List<Path> filesToMatch) {
        List matchedFiles = new ArrayList(filesToMatch.findAll {this.pathMatcher.matches(it)})

        if (!matchedFiles.isEmpty()) {
            log.trace "Matched metadata: ${matchedFiles}"
            return parseMetadata(matchedFiles[0] as Path)
        } else {
            return [:]
        }
    }
}
