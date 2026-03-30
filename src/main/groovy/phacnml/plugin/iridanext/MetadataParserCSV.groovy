package phacnml.plugin.iridanext

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Path

import groovy.transform.CompileStatic

import nextflow.Nextflow
import phacnml.plugin.iridanext.MetadataParser

@CompileStatic
class MetadataParserCSV extends MetadataParser {

    private String idcol
    private String sep
    
    public MetadataParserCSV(String idcol, String sep, PathMatcher pathMatcher = null) {
        super(pathMatcher)
        this.idcol = idcol
        this.sep = sep
    }

    public String getIdCol() {
        return idcol
    }

    public String getSep() {
        return sep
    }

    @Override
    public Map<String, Object> parseMetadata(Path path) {
        return csvToJsonById(path, this.idcol)
    }

    private Map<String, Object> csvToJsonById(Path path, String idColumn) {
        path = Nextflow.file(path) as Path
        List<Map> rowsList = path.splitCsv(header:true, strip:true, sep:sep, quote:'\"')

        Map<String, Object> rowsMap = rowsList.collectEntries { row ->
            if (!row.containsKey(idColumn)) {
                throw new Exception("Error: column with idColumn=${idColumn} not in CSV ${path}: row=${row}")
            } else {
                return [(row[idColumn] as String): (row as Map).findAll { it.key != idColumn }]
            }
        }

        return rowsMap
    }
}