package nextflow.iridanext

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class MetadataParser {
    private PathMatcher pathMatcher
    private Set<String> ignoreKeys
    private Set<String> keepKeys

    public MetadataParser(PathMatcher pathMatcher = null) {
        this.pathMatcher = pathMatcher
    }

    public void setIgnoreKeys(List<String> ignoreKeys) {
        this.ignoreKeys = ignoreKeys.toSet()
    }

    public void setKeepKeys(List<String> keepKeys) {
        this.keepKeys = keepKeys.toSet()
    }

    public Map<String, Object> parseMetadata(Path path) {
        Map<String, Object> metadata = doParse(path)

        metadata = metadata.collectEntries { m ->
            if (m.value instanceof Map) {
                Map keepValues = (m.value as Map).collectEntries { n ->
                    if (this.ignoreKeys != null && n.key in this.ignoreKeys) {
                        return [:]
                    } else if (this.keepKeys != null && !(n.key in this.keepKeys)) {
                        return [:]
                    } else {
                        return n
                    }
                }
                return [(m.key): keepValues]
            } else {
                return m
            }
        }

        return metadata
    }

    protected Map<String, Object> doParse(Path path) {
        return [:]
    }

    public Map<String, Object> matchAndParseMetadata(Collection<Path> filesToMatch) {
        List matchedFiles
        if (pathMatcher == null) {
            matchedFiles = new ArrayList(filesToMatch)
        } else {
            matchedFiles = new ArrayList(filesToMatch.findAll {this.pathMatcher.matches(it)})
        }

        if (!matchedFiles.isEmpty()) {
            log.trace "Matched metadata: ${matchedFiles}"
            return parseMetadata(matchedFiles[0] as Path)
        } else {
            return [:]
        }
    }
}
