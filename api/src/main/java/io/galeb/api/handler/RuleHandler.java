package io.galeb.api.handler;

import io.galeb.api.repository.EnvironmentRepository;
import io.galeb.core.entity.Environment;
import io.galeb.core.entity.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RuleHandler extends AbstractHandler<Rule> {

    @Autowired
    EnvironmentRepository environmentRepository;

    @Override
    protected Set<Environment> getAllEnvironments(Rule entity) {
        return environmentRepository.findAllByRuleId(entity.getId());
    }
}
