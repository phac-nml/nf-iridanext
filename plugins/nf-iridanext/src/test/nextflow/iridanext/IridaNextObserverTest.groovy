package nextflow.iridanext

import java.nio.file.Paths
import nextflow.iridanext.IridaNextObserver

import nextflow.Session
import spock.lang.Specification


class IridaNextObserverTest extends Specification {

    def 'Test relativize paths' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    path: "/tmp/output/output.json.gz",
                    relativize: true
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        iridaNextObserver.getIridaNextJSONOutput().getShouldRelativize()
        iridaNextObserver.getIridaNextJSONOutput().getRelativizePath() == Paths.get("/tmp/output")
    }

    def 'Test no relativize paths' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    path: "/tmp/output/output.json.gz",
                    relativize: false
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        !iridaNextObserver.getIridaNextJSONOutput().getShouldRelativize()
        iridaNextObserver.getIridaNextJSONOutput().getRelativizePath() == null
    }
}
