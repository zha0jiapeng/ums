package com.global.ums.utils;

import com.global.ums.dto.TokenDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtils {

    // 用户ID的键名
    private static final String CLAIM_KEY_USER_ID = "userId";
    // 用户类型的键名
    private static final String CLAIM_KEY_USER_TYPE = "userType";
    // 用户名的键名
    private static final String CLAIM_KEY_USERNAME = "username";
    // 创建时间的键名
    private static final String CLAIM_KEY_CREATED = "created";

    // Token类型的键名 (access/refresh)
    private static final String CLAIM_KEY_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret:UMSSecretKey}")
    private String secret;

    @Value("${jwt.token-prefix:GlbUmsBearer}")
    private String tokenPrefix;

    @Value("${jwt.expiration:3600}") // 默认1小时
    private Long expiration;

    @Value("${jwt.refresh-expiration:2592000}") // 默认30天
    private Long refreshExpiration;

    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Long userId;
        try {
            final Claims claims = getClaimsFromToken(token);
            userId = Long.valueOf(claims.get(CLAIM_KEY_USER_ID).toString());
        } catch (Exception e) {
            userId = null;
        }
        return userId;
    }

    /**
     * 从token中获取所有载荷信息并转换为Map
     *
     * @param token JWT
     * @return 包含所有载荷的Map
     */
    public Map<String, Object> getAllClaimsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        // 手动转换，确保类型安全
        map.put(LoginUserContextHolder.KEY_USER_ID, Long.valueOf(claims.get(CLAIM_KEY_USER_ID).toString()));
        map.put(LoginUserContextHolder.KEY_USER_TYPE, Integer.valueOf(claims.get(CLAIM_KEY_USER_TYPE).toString()));
        map.put(LoginUserContextHolder.KEY_USERNAME, claims.get(CLAIM_KEY_USERNAME).toString());
        return map;
    }

    /**
     * 从token中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Date expiration;
        try {
            final Claims claims = getClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (Exception e) {
            expiration = null;
        }
        return expiration;
    }

    /**
     * 从token中获取载荷信息
     */
    private Claims getClaimsFromToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            claims = null;
        }
        return claims;
    }

    /**
     * 检查token是否已过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 生成 Access Token
     */
    private String generateAccessToken(Long userId, Integer userType, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USER_ID, userId);
        claims.put(CLAIM_KEY_USER_TYPE, userType);
        claims.put(CLAIM_KEY_USERNAME, username);
        claims.put(CLAIM_KEY_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        return generateToken(claims, expiration);
    }

    /**
     * 生成 Refresh Token
     */
    private String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USER_ID, userId);
        claims.put(CLAIM_KEY_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        return generateToken(claims, refreshExpiration);
    }

    public TokenDTO generateToken(Long userId, Integer userType, String username){
        // 生成token
        String accessToken = generateAccessToken(userId, userType, username);
        String refreshToken = generateRefreshToken(userId);
        // 返回token信息
        return new TokenDTO(
                accessToken,
                refreshToken,
                tokenPrefix,
                userId,
                userType,
                username);
    }

    /**
     * 根据载荷和过期时间生成JWT token
     */
    private String generateToken(Map<String, Object> claims, Long expireTime) {
        Date expirationDate = new Date(System.currentTimeMillis() + expireTime * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 验证token是否有效
     */
    public Boolean validateToken(String token) {
        try {
            final Claims claims = getClaimsFromToken(token);
            return claims != null && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证是否为Access Token
     */
    public Boolean isAccessToken(String token) {
        try {
            final Claims claims = getClaimsFromToken(token);
            String tokenType = (String) claims.get(CLAIM_KEY_TOKEN_TYPE);
            return TOKEN_TYPE_ACCESS.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证是否为Refresh Token
     */
    public Boolean isRefreshToken(String token) {
        try {
            final Claims claims = getClaimsFromToken(token);
            String tokenType = (String) claims.get(CLAIM_KEY_TOKEN_TYPE);
            return TOKEN_TYPE_REFRESH.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

} 