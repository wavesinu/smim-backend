package com.smim.backend.global.auth.oauth2.user;

public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getName();
    String getImageUrl();
}
