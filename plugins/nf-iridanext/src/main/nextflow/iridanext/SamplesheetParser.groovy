package nextflow.iridanext

import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.DataflowReadChannel
import nextflow.Channel
import nextflow.extension.CH
import nextflow.extension.DataflowHelper
import nextflow.Session
import nextflow.plugin.extension.Operator
import nextflow.plugin.extension.PluginExtensionPoint
import nextflow.iridanext.IridaNextJSONOutput


class SamplesheetParser extends PluginExtensionPoint {
    private String id_key

    @Override
    void init(Session session) {
        this.id_key = session.config.navigate('iridanext.output.files.idkey', "id")
    }

    @Operator
    DataflowWriteChannel parseSamplesheet( DataflowReadChannel source ) {
        final target = CH.createBy(source)
        final String scope = IridaNextJSONOutput.SAMPLES

        final next = { it ->
            def meta = it[0]
            def id = meta[this.id_key]

            IridaNextJSONOutput.addId(scope, id)
            target.bind(it)
        }

        final done = {
            target.bind(Channel.STOP)
        }

        DataflowHelper.subscribeImpl(source, [onNext: next, onComplete: done])
        return target
    }
    
}
