package cc.cornerstones.biz.authentication.service.assembly;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class AuthenticationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandler.class);

    /**
     * Application Name
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * JWT 的 Secret
     */
    @Value("${private.auth.jwt.secret}")
    private String jwtSecret;

    public String encodeTokenByJwt(String subject, LocalDateTime issuedAt, LocalDateTime expiresAt) throws Exception {
        Algorithm algorithm = Algorithm.HMAC256(this.jwtSecret);

        try {
            String token = JWT.create().
                    withIssuer(this.applicationName).
                    withExpiresAt(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant())).
                    withSubject(subject).
                    withIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant())).
                    sign(algorithm);
            return token;
        } catch (JWTCreationException e) {
            throw new Exception("failed to encode JWT token", e);
        }
    }

    public DecodedJWT decodeTokenByJwt(String token) throws Exception {
        Algorithm algorithm = Algorithm.HMAC256(this.jwtSecret);

        try {
            JWTVerifier verifier = JWT.require(algorithm).withIssuer(this.applicationName).build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new Exception("failed to decode JWT token", e);
        }
    }

    public void validateAccessToken(String accessToken) throws Exception {
        try {
            decodeTokenByJwt(accessToken);
        } catch (Exception e) {
            LOGGER.error("failed to decode token: {}", accessToken, e);
            throw new Exception("failed to decode token");
        }
    }

    public String validateAccessTokenAndExtractSubject(String accessToken) throws Exception {
        DecodedJWT decodedJwt;
        try {
            decodedJwt = decodeTokenByJwt(accessToken);
        } catch (Exception e) {
            LOGGER.error("failed to decode token: {}", accessToken, e);
            throw new Exception("failed to decode token");
        }

        return decodedJwt.getSubject();
    }

    public static void main(String[] args) {
        sha1("root");
    }

    public static void generateSignature(String appKey, String appSecret) throws Exception {
        String toHashText = appKey + appSecret;
        String hashedText = DigestUtils.sha256Hex(toHashText);
        System.out.println(hashedText);
    }

    public static String encodeTokenByJwt() throws Exception {
        Algorithm algorithm = Algorithm.HMAC256("xnZNg662nD9BGYSd05KH9xSs");

        LocalDateTime future = LocalDateTime.now().plusHours(2);
        try {
            String token = JWT.create().
                    withIssuer("xxx").
                    withExpiresAt(Date.from(future.atZone(ZoneId.systemDefault()).toInstant())).
                    withSubject("12").
                    withIssuedAt(new Date()).
                    sign(algorithm);
            return token;
        } catch (JWTCreationException e) {
            throw new Exception("failed to encode JWT token", e);
        }
    }

    public static void sha1(String input) {
       System.out.println(DigestUtils.sha1Hex(input));
    }

    public static void sha256(String input) {
        System.out.println(DigestUtils.sha256Hex(input));
    }

    public static void sha1AndSha256(String input) {
        System.out.println(DigestUtils.sha256Hex(DigestUtils.sha1Hex(input)));
    }
}
