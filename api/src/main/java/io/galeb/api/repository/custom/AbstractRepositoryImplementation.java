/*
 * Copyright (c) 2014-2018 Globo.com - ATeam
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

package io.galeb.api.repository.custom;

import com.google.common.reflect.TypeToken;
import io.galeb.api.services.GenericDaoService;
import io.galeb.api.services.StatusService;
import io.galeb.core.entity.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.galeb.api.services.PermissionService.Action.VIEW;

@SuppressWarnings("unused")
@NoRepositoryBean
public abstract class AbstractRepositoryImplementation<T extends AbstractEntity> implements WithRoles {

    private static final Logger LOGGER = LogManager.getLogger(AbstractRepositoryImplementation.class);

    Long NOT_FOUND = -404L;

    private SimpleJpaRepository<T, Long> simpleJpaRepository;
    private StatusService statusService;
    @SuppressWarnings("UnstableApiUsage")
    private TypeToken<T> typeToken = new TypeToken<T>(getClass()) {};
    private Class<? super T> entityClass = typeToken.getRawType();
    private GenericDaoService genericDaoService;

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setSimpleJpaRepository(Class<T> klazz, GenericDaoService genericDaoService) {
        if (this.simpleJpaRepository != null) return;
        this.simpleJpaRepository = new SimpleJpaRepository<>(klazz, genericDaoService.entityManager());
        this.genericDaoService = genericDaoService;
    }

    protected long getIdIfExist(Object criteria) {
        long id = -1L;
        if (criteria == null) {
            return NOT_FOUND;
        }
        if (criteria instanceof AbstractEntity) {
            id = ((AbstractEntity) criteria).getId();
        }
        if (criteria instanceof Long) {
            id = (long) criteria;
        }
        boolean exist = genericDaoService.exist(entityClass.getSimpleName(), id);
        if (!exist) {
            return NOT_FOUND;
        }
        return id;
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public T findOneJpa(Long id) {
        return simpleJpaRepository.findOne(id);
    }

    public T findOne(Long id) {
        T entity = findOneJpa(id);
        setStatus(entity);
        return entity;
    }

    @Deprecated
    public Iterable<T> findAll(Sort sort) {
        LOGGER.warn("Sorting not yet supported");
        return Collections.emptySet();
    }

    @Transactional
    public T saveByPass(T entity) {
        return simpleJpaRepository.saveAndFlush(entity);
    }
    
    @SuppressWarnings("unchecked")
    public Page<T> findAll(Pageable pageable) {
        Account account = (Account)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isViewAll;
        boolean isView;
        Set<String> roles = mergeAllRolesOf(account);
        String roleView = entityClass.getSimpleName().toUpperCase() + "_VIEW";
        isView = roles.contains(roleView);

        String roleViewAll = roleView + "_ALL";
        isViewAll = roles.contains(roleViewAll);

        if (!isView && !isViewAll) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        String username = account.getUsername();
        String queryName = entityClass.getSimpleName() + "Default";

        List<?> queryResult;
        if (isViewAll) {
            queryResult = genericDaoService.findAll((Class<T>) entityClass, pageable);
        } else {
            queryResult = genericDaoService.findAllNamed(queryName, (Class<T>) entityClass, username, pageable);
        }
        Page<T> page = new PageImpl<>((List<T>) queryResult, pageable, queryResult.size());
        for (T entity: page) {
            setStatus(entity);
        }
        return page;
    }

    @Transactional
    public void delete(Long id) {
        T entity = simpleJpaRepository.findOne(id);
        if (entity instanceof WithStatus) {
            entity.quarantine(true);
            simpleJpaRepository.saveAndFlush(entity);
        } else {
            simpleJpaRepository.delete(entity);
        }
    }

    protected Set<Environment> getAllEnvironments(AbstractEntity entity) {
        return Collections.emptySet();
    }

    protected long getProjectId(Object criteria) {
        return -1;
    }

    private Set<String> projectRoles(Account account, long projectId) {
        try {
            if (!isAccountLinkedWithProject(account.getId(), projectId)) return Collections.emptySet();
            return mergeAllRolesOf(account);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return Collections.emptySet();
    }

    private boolean isAccountLinkedWithProject(long accountId, long projectId) {
        List<Project> projects = genericDaoService.projectLinkedToAccount(accountId, projectId);
        if (projects == null || projects.isEmpty()) {
            LOGGER.warn("Project id " + projectId + " not linked with account id " + accountId);
            return false;
        }
        return true;
    }


    @Override
    public Set<String> mergeAllRolesOf(Account account) {
        return genericDaoService.mergeAllRolesOf(account.getId());
    }

    public Set<String> roles(Object criteria) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long projectId = getProjectId(criteria);
        if (projectId == NOT_FOUND) {
            return Collections.singleton(entityClass.getSimpleName().toUpperCase() + "_" + VIEW.toString());
        }
        if (projectId > -1) {
            long accountId = account.getId();
            return projectRoles(account, projectId);
        }
        return Collections.emptySet();
    }

    private void setStatus(T entity) {
        if (entity instanceof WithStatus) {
            entity.setAllEnvironments(getAllEnvironments(entity));
            ((WithStatus) entity).setStatus(statusService.status(entity));
        }
    }

    @Override
    public Class<? extends AbstractEntity> classEntity() {
        return (Class<? extends AbstractEntity>)entityClass;
    }

}
