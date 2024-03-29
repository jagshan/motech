package org.motechproject.security.service;

import org.motechproject.security.domain.MotechUserProfile;
import org.motechproject.security.model.UserDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Locale;

/**
 * Service interface that defines APIs to retrieve and manage user details
 */
public interface MotechUserService {

    /**
     * Registers new user
     *
     * @param username of new user
     * @param password of new user
     * @param email of new user
     * @param externalId of new user
     * @param roles list that contains roles for new user
     * @param locale to be set as default for new user
     */
    @PreAuthorize("hasRole('addUser')")
    void register(String username, String password, String email, String externalId, List<String> roles, Locale locale);

    /**
     * Registers new user
     *
     * @param username of new user
     * @param password of new user
     * @param email of new user
     * @param externalId of new user
     * @param roles list that contains roles for new user
     * @param locale to be set as default for new user
     * @param isActive flag that signalize if user is active or not
     * @param openId of new user
     */
    @PreAuthorize("hasRole('addUser')")
    void register(String username, String password, String email, // NO CHECKSTYLE More than 7 parameters (found 8).
                  String externalId, List<String> roles, Locale locale, boolean isActive, String openId);

    /**
     * A method that allows to register the first MOTECH Admin in the application. Throws {@link java.lang.IllegalStateException}
     * when an active Admin User is already registered.
     *
     * @param username Username of a new user
     * @param password Password of a new user
     * @param email Email address of a new user
     * @param locale Selected locale for the new user
     */
    void registerMotechAdmin(String username, String password, String email, Locale locale);

    /**
     * Activates user
     *
     * @param username of user to be activated
     */
    @PreAuthorize("hasRole('activateUser')")
    void activateUser(String username);

    /**
     * Returns {@link org.motechproject.security.domain.MotechUserProfile} for
     * user with given username and password
     *
     * @param username of user to be returned
     * @param password of user to be returned
     * @return profile of user with given credentials
     */
    @PreAuthorize("hasRole('viewUser')")
    MotechUserProfile retrieveUserByCredentials(String username, String password);

    /**
     * Allows to change a password of a currently logged-in user.
     *
     * @param oldPassword An old password of currently logged user
     * @param newPassword A new password for the currently logged user
     * @return MotechUserProfile with updated user information
     */
    MotechUserProfile changePassword(String oldPassword, String newPassword);

    /**
     * Changes the e-mail address of a currenty logged user
     *
     * @param email a new e-mail address
     */
    void changeEmail(String email);

    /**
     * Changes password of user with given username and return
     * his {@link org.motechproject.security.domain.MotechUserProfile}
     *
     * @param userName of user
     * @param oldPassword password that was used before
     * @param newPassword new password for user
     * @return user profile after password change
     */
    @PreAuthorize("hasRole('manageUser')")
    MotechUserProfile changePassword(String userName, String oldPassword, String newPassword);

    /**
     * Checks if user with given name exists
     *
     * @param username of user
     * @return true if user exists, otherwise return false
     */
    boolean hasUser(String username);

    /**
     * Checks if user with given email exists
     *
     * @param email of user
     * @return true if user exists, otherwise false
     */
    boolean hasEmail(String email);

    /**
     * Returns all {@link org.motechproject.security.domain.MotechUserProfile}
     *
     * @return list that contains profiles
     */
    @PreAuthorize("hasAnyRole('manageUser', 'viewUser')")
    List<MotechUserProfile> getUsers();

    /**
     * Returns user with given name
     *
     * @param userName of user
     * @return user with given name
     */
    @PreAuthorize("hasRole('editUser')")
    UserDto getUser(String userName);

    /**
     * Returns user with given email
     *
     * @param email of user
     * @returnuser with given email
     */
    @PreAuthorize("hasRole('editUser')")
    UserDto getUserByEmail(String email);

    /**
     * Returns user that is logged in current session
     *
     * @return current user
     */
    UserDto getCurrentUser();

    /**
     * Returns {@link java.util.Locale} of user with given name
     *
     * @param userName of user
     * @return locale of user
     */
    Locale getLocale(String userName);

    /**
     * Returns {@link org.motechproject.security.domain.MotechUserProfile}
     * of users with set OpenId
     *
     * @return list that contains users with OpenId
     */
    @PreAuthorize("hasRole('manageUser')")
    List<MotechUserProfile> getOpenIdUsers();

    /**
     * Updates user without setting new password
     *
     * @param user to be updated
     */
    @PreAuthorize("hasAnyRole('manageUser', 'editUser')")
    void updateUserDetailsWithoutPassword(UserDto user);

    /**
     * Updates user and set new password
     *
     * @param user to be updated
     */
    @PreAuthorize("hasAnyRole('manageUser', 'editUser')")
    void updateUserDetailsWithPassword(UserDto user);

    /**
     * Deletes given user
     *
     * @param user to be removed
     */
    @PreAuthorize("hasRole('deleteUser')")
    void deleteUser(UserDto user);

    /**
     * Sends login information by email using address set for user with given name
     *
     * @param userName name of user
     * @param password of user
     */
    void sendLoginInformation(String userName, String password);

    /**
     * Sets {@link org.motechproject.security.domain.MotechUserProfile}
     * for user in current session
     *
     * @param locale to be set for user
     */
    void setLocale(Locale locale);

    /**
     * Returns all roles of user with given name
     *
     * @param userName name of user
     * @return list that contains user roles
     */
    List<String> getRoles(String userName);

    /**
     * Checks if there active user with Admin role
     *
     * @return true if user exists, otherwise false
     */
    boolean hasActiveMotechAdmin();
}
