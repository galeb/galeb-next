@active
Feature: Flux
  Scenario: Flux
    # Create environment envOne
    Given a REST client authenticated as adminTeamOne with password pass
    When request json body has:
      | name  | envOne |
    And send POST /environment
    Then the response status is 201
    # Create balance policy balancePolicyOne
    Given a REST client authenticated as adminTeamOne with password pass
    When request json body has:
      | name  | balancePolicyOne |
    And send POST /balancepolicy
    Then the response status is 201
    # Create projOne
    Given a REST client authenticated as user1 with password ""
    Then the response status is 200
    When request json body has:
      | name     | teamlocal              |
      | accounts         | [Account=user1]      |
    And send POST /team
    Then the response status is 201
    When request json body has:
      | name  | projOne |
      | teams | [Team=teamlocal] |
    And send POST /project
    Then the response status is 201
    # Create pool poolOne
    When request json body has:
      | name  | poolOne |
      | environment  | Environment=EnvOne |
      | balancepolicy  | BalancePolicy=balancePolicyOne |
      | project  | Project=projOne |
    And send POST /pool
    Then the response status is 201
    # Create pool poolTwo
    When request json body has:
      | name  | poolTwo |
      | environment  | Environment=EnvOne |
      | balancepolicy  | BalancePolicy=balancePolicyOne |
      | project  | Project=projOne |
    And send POST /pool
    Then the response status is 201
    # Create rule ruleOne
    When request json body has:
      | name  | ruleOne |
      | matching  | / |
      | pools  | [Pool=poolOne, Pool=poolTwo] |
      | project  | Project=projOne |
    And send POST /rule
    Then the response status is 201
    # Create rule ruleTwo
    When request json body has:
      | name  | ruleTwo |
      | matching  | / |
      | project  | Project=projOne |
    And send POST /rule
    Then the response status is 201
    # Patch rule ruleTwo
    When request json body has:
      | pools  | [Pool=poolOne, Pool=poolTwo] |
    And send PATCH /rule/2
    Then the response status is 200
    # Create virtualhost vhOne
    When request json body has:
      | name  | vhOne |
      | project  | Project=projOne |
      | environments  | [Environment=EnvOne] |
    And send POST /virtualhost
    Then the response status is 201
    # Create virtualhost vhTwo
    When request json body has:
      | name  | vhTwo |
      | project  | Project=projOne |
      | environments  | [Environment=EnvOne] |
      | virtualhostgroup | VirtualhostGroup=vhOne |
    And send POST /virtualhost
    Then the response status is 201
    # Create virtualhost vhThree
    When request json body has:
      | name  | vhThree |
      | project  | Project=projOne |
      | environments  | [Environment=EnvOne] |
      | virtualhostgroup | VirtualhostGroup=vhOne |
    And send POST /virtualhost
    Then the response status is 201
    # Create ruleordered roOne
    When request json body has:
      | order | 1     |
      | rule  | Rule=ruleOne |
      | environment  | Environment=EnvOne |
      | virtualhostgroup | VirtualhostGroup=vhOne |
    And send POST /ruleordered
    Then the response status is 201
    # Create ruleordered roTwo
    When request json body has:
      | order | 3     |
      | rule  | Rule=ruleOne |
      | environment  | Environment=EnvOne |
      | virtualhostgroup | VirtualhostGroup=vhOne |
    And send POST /ruleordered
    Then the response status is 201
    # Create target targetOne
    When request json body has:
      | name  | targetOne |
      | pool  | Pool=poolOne |
    And send POST /target
    Then the response status is 201
    # Create target targetTwo
    When request json body has:
      | name  | targetTwo |
      | pool  | Pool=poolTwo |
    And send POST /target
    Then the response status is 201