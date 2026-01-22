package com.nvminh162.identity.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nvminh162.identity.dto.request.AuthenticationRequest;
import com.nvminh162.identity.dto.request.IntrospectRequest;
import com.nvminh162.identity.dto.request.LogoutRequest;
import com.nvminh162.identity.dto.request.RefreshRequest;
import com.nvminh162.identity.dto.response.AuthenticationResponse;
import com.nvminh162.identity.dto.response.IntrospectResponse;
import com.nvminh162.identity.entity.InvalidatedToken;
import com.nvminh162.identity.entity.User;
import com.nvminh162.identity.exception.AppException;
import com.nvminh162.identity.exception.ErrorCode;
import com.nvminh162.identity.repository.InvalidatedTokenRepository;
import com.nvminh162.identity.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${jwt.signer-key}")
    String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    long REFRESHABLE_DURATION;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("TEST LOG SIGNER_KEY: {}", SIGNER_KEY);
        log.info("TEST LOG VALID_DURATION: {}", VALID_DURATION);
        log.info("TEST LOG REFRESHABLE_DURATION: {}", REFRESHABLE_DURATION);

        var user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);

        return AuthenticationResponse.builder().authenticated(true).token(token).build();
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        String token = request.getToken();
        boolean isValiid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValiid = false;
        }

        return IntrospectResponse.builder().valid(isValiid).build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            /* khi logout cần check theo thời gian refreshToken */
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            // even the token is invalid, we don't care
            log.info("Token already expired");
        }
    }

    /* EX:
    Token tạo lúc: 10:00
    REFRESHABLE_DURATION = 7 ngày
    Refresh được đến 10:00 sau 7 ngày*/
    private SignedJWT verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                /* isRefresh: true thì verity: refresh token*/
                /* refreshToken = thời gian issue + REFRESHABLE_DURATION (thời gian có thể refresh Token này)  */
                /* iat = Issued At: số giây tính từ 01/01/1970 */
                ? new Date(signedJWT
                        .getJWTClaimsSet()
                        .getIssueTime()
                        .toInstant()
                        .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                        .toEpochMilli())
                /* isRefresh: false thì verity: authenticate, introspect*/
                : signedJWT.getJWTClaimsSet().getExpirationTime(); // lấy thời gian hết hạn

        boolean isUnauthorized = !signedJWT.verify(verifier)
                || expiryTime.before(new Date())
                || invalidatedTokenRepository.existsById(
                        signedJWT.getJWTClaimsSet().getJWTID());

        if (isUnauthorized) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return signedJWT;
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512); // nội dung các thuật toán sử dụng

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername()) // đại diện user đăng nhập
                .issuer("nvminh162.com") // xác định token issue từ ai? thườngg là domain của service
                .issueTime(new Date()) // thời gian hiện tại
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli())) // thời gian hết hạn
                .claim(
                        "scope",
                        buildScope(user)) // claim là dữ liệu bổ sung cho token, nó có thể là bất kỳ loại dữ liệu nào
                .jwtID(UUID.randomUUID()
                        .toString()) // thêm claim jti là chuoỗi 36 ký tự UUID để lưu vào CSDL nếu logout
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes())); // cần thuật toán để sign
            return jwsObject.serialize(); // serialize => chuyển đổi thành chuỗi JSON
        } catch (Exception e) {
            log.error("Cannot generate token: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner joiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                joiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> joiner.add(permission.getName()));
            });
        return joiner.toString();
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signedJWT = verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

        invalidatedTokenRepository.save(invalidatedToken);

        var username = signedJWT.getJWTClaimsSet().getSubject();

        var user =
                userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);

        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }
}
