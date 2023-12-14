/*
 * Original file Copyright 2021, Seqera Labs (from nf-hello plugin template)
 * Modifications Copyright 2023, Government of Canada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nextflow.iridanext

import nextflow.iridanext.IridaNextObserverFactory
import nextflow.iridanext.IridaNextObserver

import nextflow.Session
import spock.lang.Specification

/**
 * Test class for IridaNextObserverFactory
 * @author Aaron Petkau <aaron.petkau@gmail.com>
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class IridaNextObserverFactoryTest extends Specification {

    def 'should not return observer' () {
        when:
        // How to set this up from https://github.com/nextflow-io/nf-prov tests
        def config = [
            iridanext: [
                enabled: false
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        def result = new IridaNextObserverFactory().create(session)
        then:
        result.size()==0
    }

    def 'should return observer' () {
        when:
        def config = [
            iridanext: [
                enabled: true
            ]
        ]
        def session = Spy(Session) {
            getConfig() >> config
        }
        def result = new IridaNextObserverFactory().create(session)
        then:
        result.size()==1
        result[0] instanceof IridaNextObserver
    }
}
