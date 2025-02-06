package com.halilsahin.stockautomation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Locale;
import java.util.ResourceBundle;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    ResourceBundle bundle = ResourceBundle.getBundle("messages", new Locale("en"));
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String loginPath = bundle.getString("login.path");
        String salesPath = bundle.getString("sales.path");
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers( "/", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers(loginPath).permitAll()
                        .requestMatchers(salesPath).authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage(loginPath)
                        .defaultSuccessUrl(salesPath, true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl(bundle.getString("logout.path"))
                        .logoutSuccessUrl(loginPath)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .invalidSessionUrl(loginPath));
        return http.build();
    }
}
