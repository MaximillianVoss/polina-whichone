package com.whichone.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final long ttlSeconds;

    public TokenService(JwtEncoder encoder, @Value("${whichone.jwt.issuer}") String issuer,
                        @Value("${whichone.jwt.audience}") String audience,
                        @Value("${whichone.jwt.ttl-seconds}") long ttlSeconds) {
        if (ttlSeconds < 60 || ttlSeconds > 86400) {
            throw new IllegalArgumentException("JWT lifetime must be between 60 and 86400 seconds.");
        }
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String username) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(username).audience(List.of(audience))
                .issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(ttlSeconds)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public long getTtlSeconds() { return ttlSeconds; }
}
