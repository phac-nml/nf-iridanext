package nextflow.iridanext

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Path

import groovy.transform.CompileStatic

import nextflow.iridanext.MetadataParser

@CompileStatic
class MetadataParserCSV extends MetadataParser {
    public MetadataParserCSV(PathMatcher pathMatcher, String id) {
        super(pathMatcher, id)
    }

    @Override
    public Map<String, Object> parseMetadata(Path path) {
        return csvToJsonById(path, this.id)
    }

    private Map<String, Object> csvToJsonById(Path path, String idColumn) {
        path = Nextflow.file(path) as Path
        List rowsList = path.splitCsv(header:true, strip:true, sep:',', quote:'\"')

        Map<String, Object> rowsMap = rowsList.collectEntries { row ->
            if (idColumn !in row) {
                throw new Exception("Error: column with idColumn=${idColumn} not in CSV ${path}")
            } else {
                return [(row[idColumn] as String): (row as Map).findAll { it.key != idColumn }]
            }
        }

        return rowsMap
    }
}