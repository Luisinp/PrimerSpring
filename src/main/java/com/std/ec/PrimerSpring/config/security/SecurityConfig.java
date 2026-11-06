package com.std.ec.PrimerSpring.config.security;
 
import java.net.URI;
import java.util.Set;
 
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
 
 
@Configuration
@EnableWebSecurity
public class SecurityConfig {
 
    // Método auxiliar para crear coincidencias de rutas con MVC (SIN @Autowired)
    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }
 
    // Las contraseñas de la tabla usuarios están en BCrypt ($2a$10$...).
    // OJO: no usar DelegatingPasswordEncoder, porque exigiría el prefijo {bcrypt}.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
 
    /**
     * Después del login:
     *  - si la persona venía de una página protegida (por ejemplo pulsó "Reservar"),
     *    vuelve a esa página;
     *  - en cualquier otro caso va al panel de su rol (mínimo privilegio).
     * Solo se vuelve a páginas .xhtml reales: así una solicitud automática del navegador
     * (iconos, imágenes que faltan, etc.) nunca termina mostrando un 404 después del login.
     */
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        RequestCache requestCache = new HttpSessionRequestCache();
        return (request, response, authentication) -> {
            String destino = null;
            SavedRequest guardada = requestCache.getRequest(request, response);
            if (guardada != null) {
                String url = guardada.getRedirectUrl().replaceAll("[?&]continue$", "");
                if (esPaginaDeRetorno(url)) {
                    destino = url;
                }
                requestCache.removeRequest(request, response);   // siempre se limpia
            }
            if (destino == null) {
                destino = request.getContextPath() + panelPorRol(authentication);
            }
            response.sendRedirect(destino);
        };
    }
 
    private boolean esPaginaDeRetorno(String url) {
        try {
            String ruta = URI.create(url).getPath();
            return ruta != null && ruta.endsWith(".xhtml")
                    && !ruta.endsWith("/login.xhtml") && !ruta.endsWith("/registro.xhtml");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
 
    private String panelPorRol(Authentication authentication) {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        if (roles.contains("ROLE_ADMIN")) {
            return "/admin/home-admin.xhtml";
        }
        if (roles.contains("ROLE_AGENTE")) {
            return "/operario/home-operario.xhtml";
        }
        if (roles.contains("ROLE_CLIENTE")) {
            return "/cliente/home-cliente.xhtml";
        }
        return "/home.xhtml";
    }
 
    // Definimos el filtro de la configuración de seguridad
    @Bean
    public SecurityFilterChain configure(HttpSecurity http, MvcRequestMatcher.Builder mvc) {
        try {
            http.csrf(AbstractHttpConfigurer::disable);
            http.authorizeHttpRequests(authorize -> authorize
                    // Recursos y páginas públicas (catálogo visible sin sesión)
                    .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/jakarta.faces.resource/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/assets/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/media/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
                    .requestMatchers(mvc.pattern("/login.xhtml")).permitAll()
                    .requestMatchers(mvc.pattern("/registro.xhtml")).permitAll()
                    .requestMatchers(mvc.pattern("/home.xhtml")).permitAll()
                    .requestMatchers(mvc.pattern("/paquete.xhtml")).permitAll()
                    .requestMatchers(mvc.pattern("/403.xhtml")).permitAll()
                    .requestMatchers(mvc.pattern("/404.xhtml")).permitAll()
                    // Páginas con sesión
                    .requestMatchers(mvc.pattern("/nosotros.xhtml")).hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENTE", "ROLE_CLIENTE")
                    .requestMatchers(mvc.pattern("/control.xhtml")).hasAnyAuthority("ROLE_ADMIN")
                    .requestMatchers(mvc.pattern("/preguntas.xhtml")).hasAnyAuthority("ROLE_AGENTE", "ROLE_CLIENTE")
                    // Un panel por rol (el administrador también puede hacer el trabajo operativo)
                    .requestMatchers(mvc.pattern("/admin/**")).hasAnyAuthority("ROLE_ADMIN")
                    .requestMatchers(mvc.pattern("/operario/**")).hasAnyAuthority("ROLE_AGENTE", "ROLE_ADMIN")
                    .requestMatchers(mvc.pattern("/cliente/**")).hasAnyAuthority("ROLE_CLIENTE")
                    // /reservar.xhtml y todo lo demás: requiere iniciar sesión
                    .anyRequest()
                    .authenticated()
            )
            .formLogin(formLogin -> formLogin
                    .loginPage("/login.xhtml").permitAll()
                    .failureUrl("/login.xhtml?error=true")
                    .successHandler(successHandler())
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/home.xhtml")
                    .deleteCookies("JSESSIONID")
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/403.xhtml"));
 
            return http.build();
        } catch (Exception ex) {
            throw new BeanCreationException("Wrong spring security configuration", ex);
        }
    }
}
 