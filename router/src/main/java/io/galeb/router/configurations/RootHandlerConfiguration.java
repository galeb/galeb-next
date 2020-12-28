/*
 * Copyright (c) 2014-2017 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.galeb.router.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.galeb.router.handlers.RootHandler;
import io.galeb.router.handlers.VirtualHostHandler;
import io.galeb.router.handlers.completionListeners.AccessLogCompletionListener;
import io.galeb.router.handlers.completionListeners.PrometheusCompletionListener;
import io.galeb.router.handlers.completionListeners.StatsdCompletionListener;

@Configuration
@Order(1)
public class RootHandlerConfiguration {

    private final VirtualHostHandler virtualHostHandler;
    private final AccessLogCompletionListener accessLogCompletionListener;
    private final StatsdCompletionListener statsdCompletionListener;
    private final PrometheusCompletionListener prometheusCompletionListener;

    @Autowired
    public RootHandlerConfiguration(final VirtualHostHandler virtualHostHandler,
            final AccessLogCompletionListener accessLogCompletionListener,
            final StatsdCompletionListener statsdCompletionListener,
            final PrometheusCompletionListener prometheusCompletionListener) {
        this.virtualHostHandler = virtualHostHandler;
        this.accessLogCompletionListener = accessLogCompletionListener;
        this.statsdCompletionListener = statsdCompletionListener;
        this.prometheusCompletionListener = prometheusCompletionListener;
    }

    @Bean
    public RootHandler rootHandler() {
        return new RootHandler(virtualHostHandler, accessLogCompletionListener, statsdCompletionListener,
                prometheusCompletionListener);
    }

}
