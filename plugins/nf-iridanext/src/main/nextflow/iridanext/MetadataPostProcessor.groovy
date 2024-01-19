package nextflow.iridanext

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class MetadataPostProcessor {
    private Set<String> ignoreKeys = [].toSet()
    private Set<String> keepKeys
    private Map<String, String> renameKeys = [:]
    private Boolean hierarchicalExpression = false
    private String hierarchicalSeparator = '.'
    private Boolean flatten = false

    public MetadataPostProcessor() {
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

    public Boolean getFlatten() {
        return flatten
    }

    public void setFlatten(Boolean flatten) {
        this.flatten = flatten
    }

    private static Map flattenR(def item, String flatName="") {
        if (item instanceof Map) {
            Map flatMap = item.collectEntries { k, v ->
                flattenR(v, "${flatName}.${k}")
            }
            return flatMap
        } else if (item instanceof List) {
            Map flatListAsMap = item.indexed().collectEntries { i, v ->
                flattenR(v, "${flatName}.${i + 1}")
            }
            return flatListAsMap
        } else {
            String nameMinusInitialDot = flatName.substring(1)
            return [(nameMinusInitialDot): item]
        }
    }

    public static Map flattenMap(Map data) {
        return flattenR(data)
    }

    private Map filterMetadataR(Map data, String keyPrefix="") {
        Set dataKeys = data.keySet()
        Map filteredData = data.collectEntries { n ->
            String expandedKey = keyPrefix == "" ? n.key : "${keyPrefix}${hierarchicalSeparator}${n.key}"
            if (expandedKey in this.ignoreKeys) {
                return [:]
            } else if (this.keepKeys != null && !(expandedKey in this.keepKeys)) {
                return [:]
            } else if (expandedKey in this.renameKeys) {
                def renamedKey = this.renameKeys[expandedKey]
                if (renamedKey in dataKeys) {
                    throw new Exception("Cannot rename metadata key [${expandedKey}] to [${renamedKey}]" +
                                        ", key [${renamedKey}] already exists")
                }
                return [(renamedKey): n.value]
            } else if (this.hierarchicalExpression && (n.value instanceof Map)) {
                    return [(n.key): this.filterMetadataR(n.value as Map, n.key as String)]
            } else {
                return n
            }
        }
        return filteredData
    }

    public Map<String, Object> process(Map<String, Object> metadata) {
        if (flatten) {
            // Flattens only data underneath a sample entry in the samples map
            metadata = metadata.collectEntries { k, v ->
                [(k): flattenMap(v as Map)]
            }
        }

        metadata = metadata.collectEntries { m ->
            if (m.value instanceof Map) {
                return [(m.key): this.filterMetadataR(m.value as Map)]
            } else {
                return m
            }
        }

        return metadata
    }
}
