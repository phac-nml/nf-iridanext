include { loadIridaSampleIds } from 'plugin/nf-iridanext'

process metadata {
    input:
    val(samples)

    output:
    path('metadata.csv'), emit: metadata

    exec:
    // Assumption: the keys are the same among all elements
    List headers = samples[0].keySet() as List

    List rows = samples.collect{ s -> headers.collect{ h -> s[h] } }

    task.workDir.resolve('metadata.csv').withWriter { writer ->
        // Header:
        writer.writeLine(headers.join(","))

        // Contents:
        rows.each {
            writer.writeLine(it.join(","))
        }
    }
}

workflow {
    main:
    ch_in = channel.of(
        [["id": "sample1", "colour": "red"]],
        [["id": "sample2", "colour": "green"]],
        [["id": "sample3", "colour": "blue"]]).loadIridaSampleIds()
    ch_in = ch_in.collect()

    ch_out = metadata(ch_in)

    publish:
    metadata = ch_out
}

// This is the newer syntax for publishing output files
output {
    metadata {
        path 'metadata'
    }
}
