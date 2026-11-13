
package com.Ecommerce.payment_service.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final PaymentAuthenticationFilter authenticationFilter;

    public SecurityConfig(PaymentAuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Allow Spring Boot error dispatches.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Internal service requests.
                        .requestMatchers(HttpMethod.POST, "/payments")
                        .hasRole("SERVICE")

                        .requestMatchers(HttpMethod.PUT, "/payments/*/status")
                        .hasRole("SERVICE")

                        // Admin collects COD payments.
                        .requestMatchers(HttpMethod.PUT, "/payments/order/*/cod/collect")
                        .hasRole("ADMIN")

                        // Users and admins can access payment details.
                        .requestMatchers(HttpMethod.GET, "/payments/order/*")
                        .hasAnyRole("USER", "ADMIN")

                        // Reject everything else.
                        .anyRequest().denyAll()
                )

                .addFilterBefore(
                        authenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<PaymentAuthenticationFilter> disableFilterRegistration() {

        FilterRegistrationBean<PaymentAuthenticationFilter> registration =
                new FilterRegistrationBean<>(authenticationFilter);

        registration.setEnabled(false);

        return registration;
    }
}
