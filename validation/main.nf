include { loadIridaSampleIds } from 'plugin/nf-iridanext'

workflow {
    channel.of([["id":"sample1"]], [["id":"sample2"]], [["id":"sample3"]]).loadIridaSampleIds()
}
