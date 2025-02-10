package nextflow.iridanext

import java.nio.file.Paths
import java.nio.file.FileSystems
import nextflow.iridanext.SamplesheetParser

import nextflow.Session
import spock.lang.Specification
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.SchemaStore
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowReadChannel

import nextflow.iridanext.TestHelper

import java.nio.file.Files
import java.util.jar.Manifest

import nextflow.Channel
import nextflow.plugin.Plugins
import nextflow.plugin.TestPluginDescriptorFinder
import nextflow.plugin.TestPluginManager
import nextflow.plugin.extension.PluginExtensionProvider
import org.pf4j.PluginDescriptorFinder
import spock.lang.Shared
import spock.lang.Timeout
import test.Dsl2Spec

import java.nio.file.Path


@Slf4j
class SamplesheetParserTest extends Dsl2Spec {

    @Shared String pluginsMode

    // NOTE: This setup() may fail if updated to Nextflow v24.01.
    //       Please refer to these changes for a possible fix:
    //       https://github.com/nextflow-io/nf-hello/commit/f686fbcf2346f3fc02b2288f567c016ab6bf2e50#diff-ae2fcafb7e787d2831f4cc882f85884ac2a100c5183b014baee656e96ddd69a6
    def setup() {
        // reset previous instances
        PluginExtensionProvider.reset()
        // this need to be set *before* the plugin manager class is created
        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')
        // the plugin root should
        def root = Path.of('.').toAbsolutePath().normalize()
        def manager = new TestPluginManager(root){
            @Override
            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return new TestPluginDescriptorFinder(){
                    @Override
                    protected Path getManifestPath(Path pluginPath) {
                        return pluginPath.resolve('build/resources/main/META-INF/MANIFEST.MF')
                    }
                }
            }
        }
        Plugins.init(root, 'dev', manager)
    }

    def cleanup() {
        Plugins.stop()
        PluginExtensionProvider.reset()
        pluginsMode ? System.setProperty('pf4j.mode',pluginsMode) : System.clearProperty('pf4j.mode')
    }

    def 'Test loadIridaSampleIds only metadata' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    path: "/tmp/output/output.json.gz"
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        def SCRIPT = '''
            include { loadIridaSampleIds } from 'plugin/nf-iridanext'
            channel
                .of([["id":"sample1"]], [["id":"sample2"]], [["id":"sample3"]])
                .loadIridaSampleIds()
            '''

        final IridaNextJSONOutput iridaNextJSONOutput = IridaNextJSONOutput.getInstance()
        iridaNextJSONOutput.reset() // Otherwise singleton class attributes persist across tests.

        and:
        def result = new MockScriptRunner([:]).setScript(SCRIPT).execute()

        then:
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample1")
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample2")
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample3")
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample4") == false
    }

    def 'Test loadIridaSampleIds metadata and data' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    path: "/tmp/output/output.json.gz"
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        def SCRIPT = '''
            include { loadIridaSampleIds } from 'plugin/nf-iridanext'
            channel
                .of([["id":"sample1"], "data1"], [["id":"sample2"], "data2"], [["id":"sample3"], "data3"])
                .loadIridaSampleIds()
            '''
        
        final IridaNextJSONOutput iridaNextJSONOutput = IridaNextJSONOutput.getInstance()
        iridaNextJSONOutput.reset() // Otherwise singleton class attributes persist across tests.

        and:
        def result = new MockScriptRunner([:]).setScript(SCRIPT).execute()

        then:
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample1")
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample2")
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample3")
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample4") == false
    }

    def 'Test no loadIridaSampleIds' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    path: "/tmp/output/output.json.gz"
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        def SCRIPT = '''
            include { loadIridaSampleIds } from 'plugin/nf-iridanext'
            channel.of([["id":"sample1"], "data1"], [["id":"sample2"], "data2"], [["id":"sample3"], "data3"])
            '''
        
        final IridaNextJSONOutput iridaNextJSONOutput = IridaNextJSONOutput.getInstance()
        iridaNextJSONOutput.reset() // Otherwise singleton class attributes persist across tests.

        and:
        def result = new MockScriptRunner([:]).setScript(SCRIPT).execute()

        then:
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample1") == false
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample2") == false
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample3") == false
        iridaNextJSONOutput.isValidId(iridaNextJSONOutput.SAMPLES, "sample4") == false
    }
}
