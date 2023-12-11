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

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.trace.TraceObserverFactory
/**
 * Factory for building the IridaNextObserver
 *
 * @author Aaron Petkau <aaron.petkau@phac-aspc.gc.ca>
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class IridaNextFactory implements TraceObserverFactory {

    @Override
    Collection<TraceObserver> create(Session session) {
        final result = new ArrayList()
        final enabled = session.config.navigate('iridanext.enabled')

        if (enabled) {
            result.add( new IridaNextObserver() )
        }
        
        return result
    }
}
