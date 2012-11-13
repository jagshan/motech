package org.motechproject.security.service;

import org.motechproject.security.model.UserDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;


public interface MotechUserService {

    @PreAuthorize("isFullyAuthenticated()")
    public void register(String username, String password, String email, String externalId, List<String> roles);

    public void register(String username, String password, String email, String externalId, List<String> roles, boolean isActive);

    @PreAuthorize("isFullyAuthenticated()")
    public void activateUser(String username);

    public MotechUserProfile retrieveUserByCredentials(String username, String password);

    public MotechUserProfile changePassword(String username, String oldPassword, String newPassword);

    @PreAuthorize("isFullyAuthenticated()")
    public void remove(String username);

    public boolean hasUser(String username);

    public List<MotechUserProfile> getUsers();

    UserDto getUser(String userName);

    void updateUser(UserDto user);

    void deleteUser(UserDto user);
}
