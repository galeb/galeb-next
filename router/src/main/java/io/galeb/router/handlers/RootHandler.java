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

package io.galeb.router.handlers;

import io.galeb.core.enums.SystemEnv;
import io.galeb.router.ResponseCodeOnError;
import io.galeb.router.handlers.completionListeners.AccessLogCompletionListener;
import io.galeb.router.handlers.completionListeners.StatsdCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.NameVirtualHostHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RootHandler implements HttpHandler {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final NameVirtualHostHandler nameVirtualHostHandler;
    private final AccessLogCompletionListener accessLogCompletionListener;
    private final StatsdCompletionListener statsdCompletionListener;
    private final AtomicBoolean rootHandlerFailed = new AtomicBoolean(false);

    private final boolean enableAccessLog = Boolean.parseBoolean(SystemEnv.ENABLE_ACCESSLOG.getValue());
    private final boolean enableStatsd    = Boolean.parseBoolean(SystemEnv.ENABLE_STATSD.getValue());

    public RootHandler(final NameVirtualHostHandler nameVirtualHostHandler,
                       final AccessLogCompletionListener accessLogCompletionListener,
                       final StatsdCompletionListener statsdCompletionListener) {
        this.nameVirtualHostHandler = nameVirtualHostHandler;
        this.accessLogCompletionListener = accessLogCompletionListener;
        this.statsdCompletionListener = statsdCompletionListener;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (enableAccessLog) exchange.addExchangeCompleteListener(accessLogCompletionListener);
        if (enableStatsd) exchange.addExchangeCompleteListener(statsdCompletionListener);
        try {
            nameVirtualHostHandler.handleRequest(exchange);
        } catch (Exception e) {
            rootHandlerFailed.compareAndSet(false, true);
            logger.error(ExceptionUtils.getStackTrace(e));
            ResponseCodeOnError.ROOT_HANDLER_FAILED.getHandler().handleRequest(exchange);
        }
    }

    public boolean isFailed() {
        return rootHandlerFailed.get();
    }
}
