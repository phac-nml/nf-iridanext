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
import groovy.util.logging.Slf4j

@Slf4j
class SamplesheetParser extends PluginExtensionPoint {
    private String id_key

    @Override
    void init(Session session) {
        this.id_key = session.config.navigate('iridanext.output.files.idkey', "id")
    }

    @Operator
    DataflowWriteChannel loadIridaSampleIds( DataflowReadChannel source ) {
        final target = CH.createBy(source)
        final String scope = IridaNextJSONOutput.SAMPLES
        final IridaNextJSONOutput iridaNextJSONOutput = IridaNextJSONOutput.getInstance()

        final next = { it ->
            def meta = it[0]

            if(meta instanceof Map<String,Object>)
            {
                if(meta.containsKey(this.id_key))
                {
                    def id = meta[this.id_key]
                    iridaNextJSONOutput.addId(scope, id)
                }
                else {
                    log.warn("The expected key (${this.id_key}) was not found in the meta map (${meta}).")
                }

            }
            else {
                log.warn("Expected a Map object in channel, but found ${meta}.")
            }

            target.bind(it)
        }

        final done = {
            target.bind(Channel.STOP)
        }

        DataflowHelper.subscribeImpl(source, [onNext: next, onComplete: done])
        return target
    }
    
}
