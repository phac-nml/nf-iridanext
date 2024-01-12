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
    private Set<String> ignoreKeys = [].toSet()
    private Set<String> keepKeys
    private Map<String, String> renameKeys = [:]
    private Boolean hierarchicalExpression = false
    private String hierarchicalSeparator = '.'

    public MetadataParser(PathMatcher pathMatcher = null) {
        this.pathMatcher = pathMatcher
    }

    public void setHierarchicalExpression(Boolean hierarchicalExpression) {
        this.hierarchicalExpression = hierarchicalExpression
    }

    public void setHierarchicalSeparator(String hierarchicalSeparator) {
        this.hierarchicalSeparator = hierarchicalSeparator
    }

    public void setIgnoreKeys(List<String> ignoreKeys) {
        if (ignoreKeys == null) {
            this.ignoreKeys = [].toSet()
        } else {
            this.ignoreKeys = ignoreKeys.toSet()
        }
    }

    public Set<String> getIgnoreKeys() {
        return this.ignoreKeys
    }

    public void setKeepKeys(List<String> keepKeys) {
        if (keepKeys == null) {
            this.keepKeys = null
        } else {
            this.keepKeys = keepKeys.toSet()
        }
    }

    public Set<String> getKeepKeys() {
        return this.keepKeys
    }

    public void setRenameKeys(Map<String, String> renameKeys) {
        if (renameKeys == null) {
            this.renameKeys = [:]
        } else {
            this.renameKeys = renameKeys
        }
    }

    public Map<String,String> getRenameKeys() {
        return this.renameKeys
    }

    protected Map filterMetadataR(Map data, String keyPrefix="") {
        Map filteredData = data.collectEntries { n ->
            String expandedKey = keyPrefix == "" ? n.key : "${keyPrefix}${hierarchicalSeparator}${n.key}"
            if (expandedKey in this.ignoreKeys) {
                return [:]
            } else if (this.keepKeys != null && !(expandedKey in this.keepKeys)) {
                return [:]
            } else if (expandedKey in this.renameKeys) {
                def renamedKey = this.renameKeys[expandedKey]
                return [(renamedKey): n.value]
            } else if (this.hierarchicalExpression && (n.value instanceof Map)) {
                    return [(n.key): this.filterMetadataR(n.value as Map, n.key as String)]
            } else {
                return n
            }
        }
        return filteredData
    }

    public Map<String, Object> parseMetadata(Path path) {
        Map<String, Object> metadata = doParse(path)

        metadata = metadata.collectEntries { m ->
            if (m.value instanceof Map) {
                return [(m.key): this.filterMetadataR(m.value as Map)]
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
