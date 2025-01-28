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


class IDParser extends PluginExtensionPoint {

    @Override
    void init(Session session) {}

    @Operator
    DataflowWriteChannel parseSamplesheet( DataflowReadChannel source ) {
        final target = CH.createBy(source)

        final next = { it ->
            IridaNextJSONOutput.addId("samples", it[0].id)
            target.bind(it)
        }

        final done = {
            target.bind(Channel.STOP)
        }

        DataflowHelper.subscribeImpl(source, [onNext: next, onComplete: done])
        return target
    }
    
}
