package com.smim.backend.domain.user;

import com.smim.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 향후 이메일 로그인을 위한 필드 (nullable)
    private String password;

    @Builder
    public User(String email, String name, String profileImage,
                Provider provider, String providerId, Role role) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    public User update(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
        return this;
    }
}
