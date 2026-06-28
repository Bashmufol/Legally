package com.legally.service;

import com.legally.entity.AppUser;
import com.legally.repository.AppUserRepository;
import com.legally.security.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures an {@link AppUser} row exists for the current Firebase UID.
 */
@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /** Creates or updates lastSeenAt for the authenticated user. Skips guest mode. */
    @Transactional
    public void syncCurrentUser() {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return;
        }
        appUserRepository.findById(uid).ifPresentOrElse(
                user -> {
                    user.touch();
                    appUserRepository.save(user);
                },
                () -> appUserRepository.save(new AppUser(uid)));
    }
}
