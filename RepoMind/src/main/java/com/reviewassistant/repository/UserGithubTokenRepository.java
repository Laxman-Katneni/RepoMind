package com.reviewassistant.repository;

import com.reviewassistant.model.UserGithubToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGithubTokenRepository extends JpaRepository<UserGithubToken, Long> {
    Optional<UserGithubToken> findByGithubId(Long githubId);
    Optional<UserGithubToken> findByUsername(String username);
}
