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

import io.galeb.core.entity.VirtualHost;
import io.galeb.router.ResponseCodeOnError;
import io.galeb.router.configurations.ManagerClientCacheConfiguration.ManagerClientCache;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.IPAddressAccessControlHandler;
import io.undertow.server.handlers.NameVirtualHostHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

public class NameVirtualHostDefaultHandler implements HttpHandler {

    public static final String IPACL_ALLOW = "allow";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ApplicationContext context;
    private final ManagerClientCache cache;

    public NameVirtualHostDefaultHandler(final ApplicationContext context,
                                         final ManagerClientCache cache) {
        this.context = context;
        this.cache = cache;
    }

    @Override
    public synchronized void handleRequest(HttpServerExchange exchange) throws Exception {
        final String hostName = exchange.getHostName();
        if (existHostname(hostName)) {
            final NameVirtualHostHandler nameVirtualHostHandler = context.getBean(NameVirtualHostHandler.class);
            if (!nameVirtualHostHandler.getHosts().containsKey(hostName)) {
                logger.info("adding " + hostName);
                final VirtualHost virtualHost = cache.get(hostName);
                nameVirtualHostHandler.addHost(hostName, defineNextHandler(virtualHost));
            }

            nameVirtualHostHandler.handleRequest(exchange);
        } else {
            handleVirtualhostNotFound(exchange);
        }
    }

    public void handleVirtualhostNotFound(HttpServerExchange exchange) throws Exception {
        ResponseCodeOnError.VIRTUALHOST_NOT_FOUND.getHandler().handleRequest(exchange);
    }

    private boolean existHostname(String hostname) {
        return cache.exist(hostname);
    }

    private HttpHandler defineNextHandler(final VirtualHost virtualHost) {
        final RuleTargetHandler ruleTargetHandler =  new RuleTargetHandler(virtualHost, context);
        if (virtualHost.getProperties().containsKey(IPACL_ALLOW)) {
            final IPAddressAccessControlHandler ipAddressAccessControlHandler = new IPAddressAccessControlHandler().setNext(ruleTargetHandler);
            Arrays.asList(virtualHost.getProperties().get(IPACL_ALLOW).split(","))
                    .forEach(ipAddressAccessControlHandler::addAllow);
            ipAddressAccessControlHandler.setDefaultAllow(false);
            return ipAddressAccessControlHandler;
        } else {
            return ruleTargetHandler;
        }
    }
}
