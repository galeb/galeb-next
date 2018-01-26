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

package io.galeb.api.security;

import io.galeb.api.security.filter.InMemoryAccountFilter;
import io.galeb.core.services.LocalAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final AuthenticationProvider authenticationProvider;
    private final LocalAdminService localAdmin;

    @Autowired
    public SecurityConfiguration(UserDetailsService userDetailsService, AuthenticationProvider authenticationProvider, LocalAdminService localAdmin) {
        this.userDetailsService = userDetailsService;
        this.authenticationProvider = authenticationProvider;
        this.localAdmin = localAdmin;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(new InMemoryAccountFilter(), BasicAuthenticationFilter.class);
        // @formatter:off
        http
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER).
            and().
                authorizeRequests().regexMatchers(HttpMethod.GET, "/.+/.+/(accounts|teams|rolegroups).*").denyAll().
            and().
                authorizeRequests().anyRequest().authenticated().
            and().
                httpBasic().
            and()
                .csrf().disable();
        // @formatter:off
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().
                withUser(localAdmin.getUsername()).
                password(localAdmin.getPassword()).
                roles("USER");
        auth.authenticationProvider(authenticationProvider);
        auth.userDetailsService(userDetailsService);
    }

}
