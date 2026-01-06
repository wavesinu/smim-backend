package com.smim.backend.global.auth;

import com.smim.backend.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Spring Security의 UserDetails 및 OAuth2User 구현체
 * User 엔티티 정보를 Spring Security가 이해할 수 있는 형태로 변환합니다.
 * JWT 토큰 생성 및 인증 과정에서 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails, OAuth2User {

    /** 사용자 ID */
    private final Long id;

    /** 사용자 이메일 */
    private final String email;

    /** 사용자 권한 목록 */
    private final Collection<? extends GrantedAuthority> authorities;

    /** OAuth2 사용자 속성 */
    private final Map<String, Object> attributes;

    /**
     * User 엔티티로부터 UserPrincipal 생성
     * 
     * @param user User 엔티티
     * @return UserPrincipal 인스턴스
     */
    public static UserPrincipal create(User user) {
        return create(user, Collections.emptyMap());
    }

    /**
     * User 엔티티와 OAuth2 속성으로부터 UserPrincipal 생성
     * 
     * @param user       User 엔티티
     * @param attributes OAuth2 사용자 속성
     * @return UserPrincipal 인스턴스
     */
    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        Collection<GrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority(user.getRole().getKey()));

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                authorities,
                attributes);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    @Override
    public String getPassword() {
        return null; // OAuth2 로그인에서는 비밀번호를 사용하지 않음
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
