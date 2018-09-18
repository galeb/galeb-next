package io.galeb.legba.services;

import com.google.gson.Gson;
import io.galeb.core.enums.SystemEnv;
import io.galeb.core.services.ChangesService;
import io.galeb.core.services.VersionService;
import io.galeb.legba.common.ErrorLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RoutersService {

    private static final Log LOGGER = LogFactory.getLog(RoutersService.class);
    private final Gson gson = new Gson();

    /**
     * Description arguments:
     * {0} - Environment Id
     * {1} - Group ID
     * {2} - Local IP
     */
    private static final String FORMAT_KEY_ROUTERS = "routers:{0}:{1}:{2}";

    public static long REGISTER_TTL  = Long.valueOf(Optional.ofNullable(System.getenv("REGISTER_ROUTER_TTL")).orElse("30000")); // ms

    private static final String LOGGING_TAGS = SystemEnv.LOGGING_TAGS.getValue();

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ChangesService changesService;

    @Autowired
    private VersionService versionService;

    @PersistenceContext
    private EntityManager entityManager;

    public Set<JsonSchema.Env> get() {
        return get(null);
    }

    public Set<JsonSchema.Env> get(String environmentId) {
        try {
            Assert.notNull(redisTemplate, StringRedisTemplate.class.getSimpleName() + " IS NULL");

            final Set<JsonSchema.Env> envs = new HashSet<>();
            String key_envid = environmentId == null ? "*" : environmentId;
            redisTemplate.keys(MessageFormat.format(FORMAT_KEY_ROUTERS, key_envid, "*", "*")).forEach(key -> {
                try {
                    String etag = redisTemplate.opsForValue().get(key);
                    long expire = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
                    MessageFormat ms = new MessageFormat(FORMAT_KEY_ROUTERS);
                    Object[] positions = ms.parse(key);
                    String env = (String) positions[0];
                    String groupId = (String) positions[1];
                    String localIp = (String) positions[2];

                    JsonSchema.Env envSchema = envs.stream()
                            .filter(e -> e.getEnvId().equals(env))
                            .findAny()
                            .orElseGet(() -> new JsonSchema.Env(env, new HashSet<>()));
                    JsonSchema.GroupID groupIDSchema = envSchema.getGroupIDs().stream()
                            .filter(g -> g.getGroupID().equals(groupId))
                            .findAny()
                            .orElseGet(() -> new JsonSchema.GroupID(groupId, new HashSet<>()));
                    groupIDSchema.getRouters().add(new JsonSchema.Router(localIp, etag, expire));
                    envSchema.getGroupIDs().add(groupIDSchema);
                    envs.add(envSchema);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return envs;
        } catch (Exception e) {
            ErrorLogger.logError(e, this.getClass());
        }
        return Collections.emptySet();
    }

    public int get(String envid, String routerGroupId) {
        int numRouters;
        numRouters = get(envid)
                .stream()
                .mapToInt(e -> e.getGroupIDs()
                        .stream()
                        .filter(g -> g.getGroupID().equals(routerGroupId))
                        .mapToInt(r -> r.getRouters().size())
                        .sum())
                .sum();
        return numRouters;
    }

    @Transactional
    public void put(String routerGroupId, String routerLocalIP, String version, String envId) {
        try {
            String key = MessageFormat.format(FORMAT_KEY_ROUTERS, envId, routerGroupId, routerLocalIP);
            Assert.notNull(redisTemplate, StringRedisTemplate.class.getSimpleName() + " IS NULL");
            if (!redisTemplate.hasKey(key)) {
                Long versionIncremented = versionService.incrementVersion(envId);
                Map<String, String> mapLog = new HashMap<>();
                mapLog.put("keyAdded", key);
                mapLog.put("versionIncremented", String.valueOf(versionIncremented));
                mapLog.put("environmentId", envId);
                mapLog.put("tags", LOGGING_TAGS);
                LOGGER.info(gson.toJson(mapLog));
            }
            redisTemplate.opsForValue().set(key, version, REGISTER_TTL, TimeUnit.MILLISECONDS);
            updateRouterState(envId);
        } catch (Exception e) {
            ErrorLogger.logError(e, this.getClass());
        }
    }

    private void updateRouterState(String envId) {
        Assert.notNull(redisTemplate, StringRedisTemplate.class.getSimpleName() + " IS NULL");
        Set<Long> eTagRouters = new HashSet<>();
        String keyAll = MessageFormat.format(FORMAT_KEY_ROUTERS, envId, "*", "*");
        redisTemplate.keys(keyAll).stream().forEach(key -> {
            try {
                eTagRouters.add(Long.valueOf(redisTemplate.opsForValue().get(key)));
            } catch (NumberFormatException e) {
                LOGGER.warn("Version is not a number. Verify the environment " + envId);
            }
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            if (ttl == null || ttl < (REGISTER_TTL/2)) {
                redisTemplate.delete(key);
                Long versionIncremented = versionService.incrementVersion(envId);

                Map<String, String> mapLog = new HashMap<>();
                mapLog.put("keyExpired", key);
                mapLog.put("versionIncremented", String.valueOf(versionIncremented));
                mapLog.put("environmentId", envId);
                mapLog.put("tags", LOGGING_TAGS);
                LOGGER.info(gson.toJson(mapLog));
            }
        });
        Long versionRouter = eTagRouters.stream().mapToLong(i -> i).min().orElse(-1L);

        changesService.listEntitiesWithOldestVersion(envId, versionRouter).entrySet().stream().forEach(mapOfEntities -> {
            mapOfEntities.getValue().entrySet().stream().filter(entry -> ChangesService.entitiesRegistrable.contains(entry.getKey())).forEach(entry -> {
                String entityClass = entry.getKey();
                String entityId = entry.getValue();
                Query query = entityManager.createQuery("DELETE FROM " + entityClass + " e WHERE e.id = :entityId AND e.quarantine = true");
                query.setParameter("entityId", Long.parseLong(entityId));
                int numEntities = query.executeUpdate();

                if (numEntities > 0) {
                    Map<String, String> mapLog = new HashMap<>();
                    mapLog.put("entityIdDeleted", entityId);
                    mapLog.put("entityClassDeleted", entityClass);
                    mapLog.put("environmentId", envId);
                    mapLog.put("tags", LOGGING_TAGS);
                    LOGGER.info(gson.toJson(mapLog));
                }
            });
            changesService.delete(mapOfEntities.getKey());
        });
    }
}
