package nextflow.iridanext

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Path
import groovy.json.JsonSlurper

import groovy.transform.CompileStatic

import nextflow.Nextflow
import nextflow.iridanext.MetadataParser

@CompileStatic
class MetadataParserJSON extends MetadataParser {

    private JsonSlurper jsonSlurper
    
    public MetadataParserJSON(PathMatcher pathMatcher = null) {
        super(pathMatcher)

        this.jsonSlurper = new JsonSlurper()
    }

    @Override
    public Map<String, Object> parseMetadata(Path path) {
        path = Nextflow.file(path) as Path
        return jsonSlurper.parseText(path.text) as Map<String, Object>
    }
}