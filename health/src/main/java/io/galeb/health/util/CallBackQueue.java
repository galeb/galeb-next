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

package io.galeb.health.util;

import static org.apache.activemq.artemis.api.core.Message.HDR_DUPLICATE_DETECTION_ID;

import io.galeb.core.entity.HealthStatus;
import io.galeb.core.entity.Target;
import io.galeb.core.entity.dto.TargetDTO;
import io.galeb.core.enums.SystemEnv;
import io.galeb.core.log.JsonEventToLogger;
import javax.jms.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class CallBackQueue {

    private static final String QUEUE_HEALTH_CALLBACK = "health-callback";
    private static final String QUEUE_HEALTH_REGISTER = "health-register";
    private static final String ZONE_ID = SystemEnv.ZONE_ID.getValue();

    private final JmsTemplate jmsTemplate;

    @Autowired
    public CallBackQueue(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void update(TargetDTO targetDTO) {
        String correlation = targetDTO.getCorrelation();
        try {
            jmsTemplate.send(QUEUE_HEALTH_CALLBACK, session -> {
                Message message = session.createObjectMessage(targetDTO);
                Target target = targetDTO.getTarget();
                String uniqueId = "ID:" + target.getName() + "-" + ZONE_ID + "_" + System.currentTimeMillis();
                message.setStringProperty("_HQ_DUPL_ID", uniqueId);
                message.setJMSMessageID(uniqueId);
                message.setStringProperty(HDR_DUPLICATE_DETECTION_ID.toString(), uniqueId);
                HealthStatus healthStatus = targetDTO.getHealthStatus(ZONE_ID).orElse(new HealthStatus());

                JsonEventToLogger eventToLogger = new JsonEventToLogger(this.getClass());
                eventToLogger.put("queue", QUEUE_HEALTH_CALLBACK);
                eventToLogger.put("short_message", "Sending to callback queue");
                eventToLogger.put("jmsMessageId", uniqueId);
                eventToLogger.put("correlation", correlation);
                eventToLogger.put("healthStatus_source", ZONE_ID);
                eventToLogger.put("healthStatus_statusDetailed", healthStatus.getStatusDetailed());
                eventToLogger.put("healthStatus_status", healthStatus.getStatus().name());
                eventToLogger.put("healthStatus_target", target.getName());
                eventToLogger.sendInfo();
                return message;
            });
        } catch (JmsException e) {
            JsonEventToLogger eventToLogger = new JsonEventToLogger(this.getClass());
            eventToLogger.put("queue", QUEUE_HEALTH_CALLBACK);
            eventToLogger.put("short_message", "Error sending to callback queue");
            eventToLogger.put("correlation", correlation);
            eventToLogger.sendError(e);
        }
    }

    public void register(String zoneId) {
        String envId = SystemEnv.ENVIRONMENT_ID.getValue();
        jmsTemplate.convertAndSend(QUEUE_HEALTH_REGISTER, "health:" + envId + ":" + zoneId + ":" + LocalIP.encode());
    }
}
