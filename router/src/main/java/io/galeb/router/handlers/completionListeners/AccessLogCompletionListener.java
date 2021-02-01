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

package io.galeb.router.handlers.completionListeners;

import com.google.gson.JsonObject;
import io.galeb.core.enums.SystemEnv;
import io.galeb.router.client.hostselectors.HostSelector;
import io.galeb.router.handlers.RequestIDHandler;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static io.undertow.attribute.ExchangeAttributes.*;

@Component
public class AccessLogCompletionListener extends ProcessorLocalStatusCode implements ExchangeCompletionListener {

    private static final int MAX_REQUEST_TIME = Integer.MAX_VALUE - 1;
    private static final String REQUESTID_HEADER = SystemEnv.REQUESTID_HEADER.getValue();
    public static final String SHORT_MESSAGE = "This log was generated by Galeb Router";

    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
        try {
            logger.info(getJsonObject(exchange));
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            nextListener.proceed();
        }
    }

    public String getJsonObject(HttpServerExchange exchange) {
        final String remoteAddr = remoteIp().readAttribute(exchange);
        final String host = localServerName().readAttribute(exchange);
        final String requestElements[] = requestList().readAttribute(exchange).split(" ");
        final String method = exchange.getRequestMethod().toString();
        final String requestUri = exchange.getRequestURI();
        final String proto = exchange.getProtocol().toString();
        final String httpReferer = requestElements.length > 3 ? requestElements[3] : null;
        final String xMobileGroup = requestElements.length > 4 ? requestElements[4] : null;
        final int originalStatusCode = Integer.parseInt(responseCode().readAttribute(exchange));
        final long responseBytesSent = exchange.getResponseBytesSent();
        final String bytesSent = Long.toString(responseBytesSent);
        final String bytesSentOrDash = responseBytesSent == 0L ? "-" : bytesSent;
        final Integer responseTime = Math.round(Float.parseFloat(responseTimeAttribute.readAttribute(exchange)));
        final String realDestAttached = exchange.getAttachment(HostSelector.REAL_DEST);
        final String realDest = realDestAttached != null ? realDestAttached : extractXGalebErrorHeader(exchange.getResponseHeaders());
        final String userAgent = requestHeader(Headers.USER_AGENT).readAttribute(exchange);
        final String requestId = !"".equals(REQUESTID_HEADER) ? requestHeader(RequestIDHandler.requestIdHeader()).readAttribute(exchange) : null;
        final String xForwardedFor = requestHeader(Headers.X_FORWARDED_FOR).readAttribute(exchange);

        final int fakeStatusCode = getFakeStatusCode(realDestAttached, originalStatusCode, responseBytesSent, responseTime, MAX_REQUEST_TIME);
        final int statusCode = fakeStatusCode != ProcessorLocalStatusCode.NOT_MODIFIED ? fakeStatusCode : originalStatusCode;

        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"); // ISO-8601
        final String json = "{" +
                "\"@timestamp\":\"" + dateFormat.format(new Date()) + "\"," +
                "\"@version\":\"" + "1" + "\"," +
                "\"host\":\"" + SystemEnv.HOSTNAME.getValue() + "\"," +
                "\"short_message\":\"" + SHORT_MESSAGE + "\"," +
                "\"vhost\":\"" + host + "\"," +
                "\"_tags\":\"" + SystemEnv.LOGGING_TAGS.getValue() + ",ACCESS" + "\"," +
                "\"remote_addr\":\"" + remoteAddr + "\"," +
                "\"request_method\":\"" + method + "\"," +
                "\"request_uri\":\"" + requestUri + "\"," +
                "\"server_protocol\":\"" + proto + "\"," +
                "\"http_referer\":\"" +  (httpReferer != null ? httpReferer : "-") + "\"," +
                "\"http_x_mobile_group\":\"" + (xMobileGroup != null ? xMobileGroup : "-") + "\"," +
                "\"status\":\"" + statusCode + "\"," +
                "\"body_bytes_sent\":\"" + bytesSent + "\"," +
                "\"request_time\":\"" + responseTime + "\"," +
                "\"upstream_addr\":\"" + realDest + "\"," +
                "\"upstream_status\":\"" + originalStatusCode + "\"," +
                "\"upstream_response_length\":\"" + bytesSentOrDash + "\"," +
                "\"http_user_agent\":\"" + (userAgent != null ? userAgent : "-") + "\"," +
                "\"request_id\":\"" + (requestId != null ? requestId : "-") + "\"," +
                "\"http_x_forwarded_for\":\"" + (xForwardedFor != null ? xForwardedFor : "-") + "\"" +
                "}";

        return json;
    }
}
