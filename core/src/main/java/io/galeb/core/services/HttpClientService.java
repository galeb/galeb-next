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

package io.galeb.core.services;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Realm;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

import static io.galeb.core.logger.ErrorLogger.logError;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

@Service
public class HttpClientService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AsyncHttpClient asyncHttpClient;

    public HttpClientService() {
        asyncHttpClient = asyncHttpClient(config()

                .setFollowRedirect(false)
                .setKeepAlive(true)
                .setConnectTimeout(10000)
                .setPooledConnectionIdleTimeout(10)
                .setSoReuseAddress(true)
                .setMaxConnectionsPerHost(100).build());
    }

    public String getResponseBodyWithToken(String url, String token) {
        try {
            RequestBuilder requestBuilder = new RequestBuilder().setUrl(url)
                    .setHeader("x-auth-token", token);
            Response response = asyncHttpClient.executeRequest(requestBuilder.build()).get();
            return response.getResponseBody();
        } catch (NullPointerException e) {
            logger.error("Token is NULL (auth problem?)");
        } catch (ExecutionException | InterruptedException e) {
            logError(e, this.getClass());
        }
        return "";
    }

    public boolean patchResponse(String url, String body, String token) {
            RequestBuilder requestBuilder = new RequestBuilder().setUrl(url)
                    .setHeader("x-auth-token", token).setMethod("PATCH")
                    .setBody(body);
            try {
                Response response = asyncHttpClient.executeRequest(requestBuilder.build()).get();
                if (response.getStatusCode() < 400) return true;
            } catch (InterruptedException | ExecutionException e) {
                logError(e, this.getClass());
            }
            return false;
    }

    public String getResponseBodyWithAuth(String user, String pass, String url) {
        RequestBuilder requestTokenBuilder = new RequestBuilder().setUrl(url)
                .setRealm(new Realm.Builder(user, pass).setScheme(Realm.AuthScheme.BASIC).build());
        try {
            Response response = asyncHttpClient.executeRequest(requestTokenBuilder).get();
            if (response.getStatusCode() == 401) {
                logger.error("401 Unauthorized: \"" + user + "\" auth failed");
                return "";
            }
            return response.getResponseBody();
        } catch (ExecutionException | InterruptedException e) {
            logError(e, this.getClass());
        }
        return "";
    }
}