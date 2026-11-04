package io.github.igorermolenko.trimly.access.api;

import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * The owner API ({@code /api/**}) requires the owner credential as a bearer token; everything else,
 * including every redirect, is public (feature ADR-0001). Stateless: no session, no CSRF, no form
 * login, no HTTP Basic.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      OwnerCredentialProperties credential,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    var entryPoint = new ProblemDetailsAuthenticationEntryPoint(resolver);
    return http.authorizeHttpRequests(requests ->
            requests.requestMatchers("/api/**").authenticated().anyRequest().permitAll())
        .addFilterBefore(
            new OwnerCredentialFilter(credential.ownerCredential()),
            AnonymousAuthenticationFilter.class)
        .exceptionHandling(exceptions ->
            exceptions
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(
                    (request, response, denied) -> entryPoint.commence(request, response, null)))
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable())
        .requestCache(cache -> cache.disable())
        // Redirects set Cache-Control: no-store themselves (AC-07); don't let Security rewrite it.
        .headers(headers -> headers.cacheControl(cacheControl -> cacheControl.disable()))
        .build();
  }

  /** No username/password users: stops Boot from generating and logging a default password. */
  @Bean
  UserDetailsService noUsers() {
    return new InMemoryUserDetailsManager();
  }
}
