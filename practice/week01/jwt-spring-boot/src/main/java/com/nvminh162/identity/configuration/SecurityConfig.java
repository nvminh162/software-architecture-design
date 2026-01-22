package com.nvminh162.identity.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity // options: có thể có hoặc không có
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {
    ObjectMapper objectMapper;
    CustomJwtDecoder jwtDecoder;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/users", "/auth/token", "/auth/introspect", "/auth/logout", "/auth/refresh"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(request -> {
            request.requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS)
                    .permitAll()
                    /* Cách 1: Default is SCOPE_ => custom jwtAuthenticationConverter() => ROLE_ */
                    // .requestMatchers(HttpMethod.GET, "/users").hasAuthority("ROLE_ADMIN") /* ROLE_ADMIN */
                    /* Cách 2: hasRole(Role.ADMIN.name()) => Role.ADMIN.name() là enum Role của admin */
                    // .requestMatchers(HttpMethod.GET, "/users").hasRole(Role.ADMIN.name()) /* ADMIN */
                    /* Cách 3: Phân quyền trên method */
                    .anyRequest()
                    .authenticated();
        });

        /*
        Đăng ký với provider manager support jwt token
        => khi thực hiện request cung cấp token vào header Authorization: Bearer <token>
        => jwt authentication thực hiện authentication dựa trên token
        */
        http.oauth2ResourceServer(oauth2 -> oauth2
                /* options: .jwkSetUri(null): cấu hình với resource server thứ3 cần url này
                => dùng decoder để decode token do hệ thống generate token */
                .jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(jwtDecoder)
                        /* Converter tùy chỉnh để map scope thành authorities */
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                /* Cấu hình này bắt exception khi user không truyền token */
                /* Xử lý 401 exception ở trên tầng filter mà exception handler không xử lý được */
                /* AuthenticationEntryPoint: khi authentication fail sẽ điều hướng đi đâu? => ở đây chỉ cần trả về error response */
                .authenticationEntryPoint(jwtAuthenticationEntryPoint()));

        /*
        spring security mặc định sẽ bật cấu hình csrf
        => Bảo vệ endpoint trước attack cross-site request forgery
        => config để disable csrf */
        http.csrf(AbstractHttpConfigurer::disable);

        /*
        Cấu hình CORS để cho phép các request từ các origin khác nhau
        => Cần thiết khi frontend và backend chạy trên các domain/port khác nhau
        */
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Cho phép tất cả các origin (trong production nên chỉ định cụ thể)
        // Sử dụng addAllowedOriginPattern thay vì addAllowedOrigin để tương thích với credentials
        corsConfiguration.addAllowedOriginPattern("*");
        // Cho phép tất cả các HTTP methods
        corsConfiguration.addAllowedMethod("*");
        // Cho phép tất cả các headers (bao gồm Authorization header cho JWT)
        corsConfiguration.addAllowedHeader("*");
        // Cho phép gửi credentials (cần thiết nếu frontend gửi cookies)
        // Lưu ý: Khi set true, phải dùng addAllowedOriginPattern thay vì addAllowedOrigin
        corsConfiguration.setAllowCredentials(true);
        // Thời gian cache preflight request (1 giờ)
        corsConfiguration.setMaxAge(3600L);
        // Cho phép expose các headers trong response (cần thiết để frontend đọc được)
        corsConfiguration.addExposedHeader("*");

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return urlBasedCorsConfigurationSource;
    }

    /*
     * Chịu trách nhiệm verify token*/
    /*=> dùng CustomJwtDecoder chèn thêm buước introspect check logout*/
    /*@Bean
    JwtDecoder jwtDecoder() {
    	SecretKeySpec secretKey = new SecretKeySpec(SIGNER_KEY.getBytes(), "HS512");
    	return NimbusJwtDecoder
    		.withSecretKey(secretKey)
    		.macAlgorithm(MacAlgorithm.HS512)
    		.build();
    }*/

    /*
    Default is 10
    Càng lốn độ mạnh mật khẩu càng cao
    => ảnh hưởng performance nếu đặt lớn
    => yêu cầu mã hoá dưới 1s tuỳ yêu cầu system */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /* Default is SCOPE_ */
    /* Cần để map scope thành authorities */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        //        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); // đã thêm ở buildScope(User user)
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }
}
