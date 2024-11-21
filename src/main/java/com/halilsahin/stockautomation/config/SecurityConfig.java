package com.halilsahin.stockautomation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Locale;
import java.util.ResourceBundle;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    ResourceBundle bundle = ResourceBundle.getBundle("messages", new Locale("en"));
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(bundle.getString("sales.path")).authenticated()
                        .requestMatchers( "/", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage(bundle.getString("login.path"))
                        .defaultSuccessUrl(bundle.getString("sales.path"), true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl(bundle.getString("logout.path"))
                        .logoutSuccessUrl(bundle.getString("login.path"))
                        .permitAll()
                );
        return http.build();
    }
}
