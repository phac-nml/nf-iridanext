package nextflow.iridanext

import java.nio.file.Paths
import java.nio.file.FileSystems
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

    def 'Test configure files section' () {
        when:
        def config = [
            iridanext: [
                enabled: true,
                output: [
                    files: [
                        idkey: "id1",
                        global: ["**/file1.txt", "**/fastq/file2.fastq"],
                        samples: ["**/assembly/*.contigs.fa.gz"]
                    ]
                ]
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        def matcher = { it ->
            FileSystems.getDefault().getPathMatcher(it)
        }
        IridaNextObserver iridaNextObserver = new IridaNextObserver()
        iridaNextObserver.onFlowCreate(session)
        
        then:
        iridaNextObserver.getFilesMetaId() == "id1"
        iridaNextObserver.getPathMatchers("global").any {it.matches(Paths.get("folder/file1.txt"))}
        iridaNextObserver.getPathMatchers("global").any {it.matches(Paths.get("/fastq/file2.fastq"))}
        !iridaNextObserver.getPathMatchers("global").any {it.matches(Paths.get("/fastq/not/file2.fastq"))}
        iridaNextObserver.getPathMatchers("samples").any {it.matches(Paths.get("/assembly/sam.contigs.fa.gz"))}
    }
}
