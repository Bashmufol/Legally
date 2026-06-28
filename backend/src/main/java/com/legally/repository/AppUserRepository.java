package com.legally.repository;

import com.legally.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA access for {@link AppUser} rows keyed by Firebase UID. */
public interface AppUserRepository extends JpaRepository<AppUser, String> {
}
