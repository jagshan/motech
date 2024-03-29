package org.motechproject.security.it;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.motechproject.security.authentication.MotechPasswordEncoder;
import org.motechproject.security.domain.MotechUser;
import org.motechproject.security.repository.MotechRolesDataService;
import org.motechproject.security.repository.MotechUsersDataService;
import org.motechproject.security.service.MotechUserService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MotechUserServiceBundleIT extends BaseIT {

    @Inject
    private MotechUserService motechUserService;

    @Inject
    private MotechUsersDataService usersDataService;

    @Inject
    private MotechRolesDataService rolesDataService;

    private MotechPasswordEncoder passwordEncoder;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        passwordEncoder = getFromContext(MotechPasswordEncoder.class);

        usersDataService.deleteAll();
        rolesDataService.deleteAll();

        // authorize
        motechUserService.registerMotechAdmin("admin", "admin", "admin@mail.com", Locale.ENGLISH);

        setUpSecurityContext("admin", "admin");
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        usersDataService.deleteAll();
        rolesDataService.deleteAll();

        clearSecurityContext();
    }

    @Test
    public void testRegister() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        MotechUser motechUser = usersDataService.findByUserName("userName");
        assertNotNull(motechUser);
        assertTrue(motechUser.getRoles().contains("IT_ADMIN"));
        assertTrue(motechUser.getRoles().contains("DB_ADMIN"));
    }

    @Test
    public void shouldActivateUser() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH, false, "");
        motechUserService.activateUser("userName");
        MotechUser motechUser = usersDataService.findByUserName("userName");
        assertTrue(motechUser.isActive());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUserNameIsEmptyForRegister() {
        motechUserService.register("", "password", "ext_id", "", new ArrayList<String>(), Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUserNameIsEmptyForRegisterWithActiveInfo() {
        motechUserService.register("", "password", "ext_id", "", new ArrayList<String>(), Locale.ENGLISH, true, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfPasswordIsEmptyForRegister() {
        motechUserService.register("user", "", "ext_id", "", new ArrayList<String>(), Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUserNameisNull() {
        motechUserService.register(null, "", "ext_id", "", new ArrayList<String>(), Locale.ENGLISH);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenAddingMoreThanOneAdminUser() {
        // The first one is created during test initialization
        motechUserService.registerMotechAdmin("admin2", "admin2", "admin2@mail.com", Locale.ENGLISH);
    }

    @Test
    public void shouldNotActivateInvalidUser() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH, false, "");
        motechUserService.activateUser("userName1");
        MotechUser motechUser = usersDataService.findByUserName("userName");
        assertFalse(motechUser.isActive());
    }

    @Test
    public void shouldCreateActiveUserByDefault() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        MotechUser motechUser = usersDataService.findByUserName("userName");
        assertTrue(motechUser.isActive());
    }

    @Test
    public void shouldCreateInActiveUser() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH, false, "");
        MotechUser motechUser = usersDataService.findByUserName("userName");
        assertFalse(motechUser.isActive());
    }

    @Test
    public void testPasswordEncoding() {
        String plainTextPassword = "testpassword";
        motechUserService.register("testuser", plainTextPassword, "entity1", "", asList("ADMIN"), Locale.ENGLISH);
        MotechUser motechUser = usersDataService.findByUserName("testuser");
        assertTrue(passwordEncoder.isPasswordValid(motechUser.getPassword(), plainTextPassword));
    }

    @Test
    public void shouldChangePassword() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        motechUserService.changePassword("userName", "password", "newPassword");
        MotechUser motechUser = usersDataService.findByUserName("userName");
        assertTrue(passwordEncoder.isPasswordValid(motechUser.getPassword(), "newPassword"));
    }

    @Test
    public void shouldNotChangePasswordWithoutOldPassword() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        motechUserService.changePassword("userName", "foo", "newPassword");
        MotechUser motechUser = usersDataService.findByUserName("userName");
        assertTrue(passwordEncoder.isPasswordValid(motechUser.getPassword(), "password"));
    }

    @Test
    public void hasUserShouldReturnTrueOnlyIfUserExists() {
        assertFalse(motechUserService.hasUser("username"));
        motechUserService.register("userName", "password", "1234", "", Arrays.asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        assertTrue(motechUserService.hasUser("username"));
    }


    @Test
    public void shouldReturnPresenceOfAdmin() {
        motechUserService.deleteUser(motechUserService.getCurrentUser());
        clearSecurityContext();

        assertFalse(motechUserService.hasActiveMotechAdmin());
        motechUserService.registerMotechAdmin("admin", "admin", "aaa@admin.com", Locale.ENGLISH);
        assertTrue(motechUserService.hasActiveMotechAdmin());
    }

    @Test
    public void shouldValidateUserCredentials() {
        motechUserService.register("username", "password", "1234", "", asList("IT_ADMIN"), Locale.ENGLISH);
        assertNotNull(motechUserService.retrieveUserByCredentials("username", "password"));
        assertNull(motechUserService.retrieveUserByCredentials("username", "passw550rd"));
    }

    @Test
    public void shouldReturnEmptyListOfRolesForNonExistentUser() {
        List<String> roles = motechUserService.getRoles("non-existent");
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }
}
