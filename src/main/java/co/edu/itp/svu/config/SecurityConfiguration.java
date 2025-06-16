package co.edu.itp.svu.config;

import static org.springframework.security.config.Customizer.withDefaults;

import co.edu.itp.svu.security.*;
import co.edu.itp.svu.web.filter.SpaWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    /**
     * Creates an in-memory service for storing and retrieving authorized OAuth2
     * clients.
     * <p>
     * This bean is essential for the server-to-server OAuth2 flow used by the
     * {@code OAuth2MailService}.
     * It provides a non-persistent, in-memory storage for
     * {@link org.springframework.security.oauth2.client.OAuth2AuthorizedClient}
     * objects, which contain the access and refresh tokens. This implementation is
     * suitable for a single-instance
     * application where state does not need to be preserved across restarts.
     *
     * @param clientRegistrationRepository The repository of configured OAuth2
     *                                     clients (e.g., "google").
     * @return An in-memory implementation of {@link OAuth2AuthorizedClientService}.
     * @see co.edu.itp.svu.service.OAuth2MailService
     */
    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    /**
     * Creates a repository for managing authorized clients, associating them with
     * the authenticated principal.
     * <p>
     * This bean is primarily used by Spring Security's web-aware components. It
     * uses the underlying
     * {@link OAuth2AuthorizedClientService} to persist authorized client
     * information between requests,
     * typically by storing a reference in the {@code HttpSession}. While our mail
     * service uses a non-web
     * manager, defining this bean ensures full compatibility with the Spring
     * Security OAuth2 client framework.
     *
     * @param authorizedClientService The service that handles the actual storage of
     *                                authorized clients.
     * @return A repository that links authorized clients to the current user's
     *         session.
     */
    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }

    private final JHipsterProperties jHipsterProperties;

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class)
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(FrameOptionsConfig::sameOrigin)
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicy(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                                authz
                                                .requestMatchers(mvc.pattern("/index.html"), mvc.pattern("/*.js"),
                                                                mvc.pattern("/*.txt"),
                                                                mvc.pattern("/*.json"), mvc.pattern("/*.map"),
                                                                mvc.pattern("/*.css"))
                                                .permitAll()
                                                .requestMatchers(mvc.pattern("/*.ico"), mvc.pattern("/*.png"),
                                                                mvc.pattern("/*.svg"),
                                                                mvc.pattern("/*.webapp"))
                                                .permitAll()
                                                .requestMatchers(mvc.pattern("/assets/**")).permitAll()
                                                .requestMatchers(mvc.pattern("/content/**")).permitAll()
                                                .requestMatchers(mvc.pattern("/swagger-ui/**")).permitAll()
                                                .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/authenticate"))
                                                .permitAll()
                                                .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/authenticate"))
                                                .permitAll()
                                                .requestMatchers(mvc.pattern("/api/register")).permitAll()
                                                .requestMatchers(mvc.pattern("/api/activate")).permitAll()
                                                .requestMatchers(mvc.pattern("/api/account/reset-password/init"))
                                                .permitAll()
                                                .requestMatchers(mvc.pattern("/api/account/reset-password/finish"))
                                                .permitAll()
                                                // .requestMatchers(mvc.pattern("/api/account/**")).hasAnyAuthority(AuthoritiesConstants.ADMIN,
                                                // AuthoritiesConstants.USER)
                                                // .requestMatchers(mvc.pattern("/api/admin/users/**")).permitAll()
                                                // api/oficinas/oficinasUserLogin

                                                .requestMatchers(mvc.pattern("/api/public/pqrs/**")).permitAll()
                                                .requestMatchers(mvc.pattern("/api/oficina/**"))
                                                .hasAnyAuthority(AuthoritiesConstants.ADMIN)
                                                .requestMatchers(mvc.pattern("/api/archivo-adjuntos/**")).permitAll()
                                                .requestMatchers(mvc.pattern("/api/informe-pqrs/**"))
                                                .hasAnyAuthority(AuthoritiesConstants.ADMIN)
                                                .requestMatchers(mvc.pattern("/api/sse-notifications/subscribe"))
                                                .hasAnyAuthority(AuthoritiesConstants.ADMIN,
                                                                AuthoritiesConstants.FUNCTIONARY,
                                                                AuthoritiesConstants.FRONT_DESK_CS)
                                                .requestMatchers(mvc.pattern("/api/notifications/**")).permitAll()
                                                .requestMatchers(mvc.pattern("/api/oficinas/oficinasUserLogin/*"))
                                                .permitAll()
                                                .requestMatchers(mvc.pattern("/api/admin/**"))
                                                .hasAuthority(AuthoritiesConstants.ADMIN)
                                                .requestMatchers(mvc.pattern("/api/**")).authenticated()
                                                .requestMatchers(mvc.pattern("/v3/api-docs/**"))
                                                .hasAuthority(AuthoritiesConstants.ADMIN)
                                                .requestMatchers(mvc.pattern("/management/health")).permitAll()
                                                .requestMatchers(mvc.pattern("/management/health/**")).permitAll()
                                                .requestMatchers(mvc.pattern("/management/info")).permitAll()
                                                .requestMatchers(mvc.pattern("/management/prometheus")).permitAll()
                                                .requestMatchers(mvc.pattern("/management/**"))
                                                .hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }
}
