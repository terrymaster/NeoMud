package com.neomud.server.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.HashSet

private val logger = LoggerFactory.getLogger("PlatformTokenVerifier")

/** Claims extracted from a verified Platform JWT. */
data class PlatformClaims(val userId: String, val role: String)

/**
 * Verifies Platform JWTs using either:
 * - RS256 via JWKS endpoint (production) — fetches and caches public keys
 * - HS256 via shared secret (dev mode) — for local development without RSA keys
 */
class PlatformTokenVerifier(
    private val jwksUrl: String?,
    private val devSecret: String?,
    private val issuer: String = "neomud-platform"
) {
    private val jwtProcessor = if (jwksUrl != null) buildRs256Processor() else null

    private fun buildRs256Processor(): DefaultJWTProcessor<SecurityContext> {
        val jwkSource = JWKSourceBuilder.create<SecurityContext>(URL(jwksUrl))
            .cache(true)
            .rateLimited(true)
            .build()

        val keySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)

        return DefaultJWTProcessor<SecurityContext>().apply {
            jwsTypeVerifier = DefaultJOSEObjectTypeVerifier.JWT
            jwsKeySelector = keySelector
            jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
                JWTClaimsSet.Builder().issuer(issuer).build(),
                HashSet(listOf("sub", "userId", "role", "exp", "iss"))
            )
        }
    }

    /** Verify a Platform JWT and extract claims. Returns null on any failure. */
    fun verify(token: String): PlatformClaims? {
        return try {
            if (jwtProcessor != null) {
                verifyRs256(token)
            } else if (devSecret != null) {
                verifyHs256(token)
            } else {
                logger.warn("No JWKS URL or dev secret configured — cannot verify platform tokens")
                null
            }
        } catch (e: Exception) {
            logger.warn("Platform token verification failed: {}", e.message)
            null
        }
    }

    private fun verifyRs256(token: String): PlatformClaims {
        val claims = jwtProcessor!!.process(token, null)
        return extractClaims(claims)
    }

    private fun verifyHs256(token: String): PlatformClaims {
        val jwt = SignedJWT.parse(token)
        val verifier = MACVerifier(devSecret!!.toByteArray())
        if (!jwt.verify(verifier)) error("HS256 signature invalid")

        val claims = jwt.jwtClaimsSet
        if (claims.issuer != issuer) error("Invalid issuer: ${claims.issuer}")
        if (claims.expirationTime != null && claims.expirationTime.before(java.util.Date())) {
            error("Token expired")
        }
        return extractClaims(claims)
    }

    private fun extractClaims(claims: JWTClaimsSet): PlatformClaims {
        val userId = claims.getStringClaim("userId")
            ?: error("Missing userId claim")
        val role = claims.getStringClaim("role")
            ?: error("Missing role claim")
        return PlatformClaims(userId, role)
    }

    /** Whether this verifier is configured and can verify tokens. */
    val isEnabled: Boolean get() = jwksUrl != null || devSecret != null
}
