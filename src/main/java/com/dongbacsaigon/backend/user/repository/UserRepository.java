package com.dongbacsaigon.backend.user.repository;

import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    Optional<User> findByIdAndRole(UUID id, UserRole role);

    @Query("""
            select user
            from User user
            where user.role = :role
              and (:status is null or user.status = :status)
              and (
                  :search is null
                  or lower(user.email) like concat('%', :search, '%')
                  or lower(user.fullName) like concat('%', :search, '%')
              )
            """)
    Page<User> searchByRole(
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
