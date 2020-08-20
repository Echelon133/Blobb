package ml.echelon133.blobb.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;


    @Test
    public void followUserWithUuid_ThrowsWhenUserDoesntExist() {
        UUID u1Uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");
        UUID u2Uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(u2Uuid)).willReturn(false);

        // when
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.followUserWithUuid(any(), u2Uuid);
        }).getMessage();

        assertEquals(String.format("User with UUID %s doesn't exist", u2Uuid), message);
    }

    @Test
    public void followUserWithUuid_WhenUserDoesntAlreadyFollow() throws Exception {
        UUID u1Uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");
        user.setUuid(u1Uuid);
        UUID u2Uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(u2Uuid)).willReturn(true);
        given(userRepository.checkIfUserWithUuidFollows(u1Uuid, u2Uuid)).willReturn(Optional.empty());
        given(userRepository.followUserWithUuid(u1Uuid, u2Uuid)).willReturn(Optional.of(1L));

        // when
        boolean result = userService.followUserWithUuid(user, u2Uuid);

        // then
        assertTrue(result);
    }

    @Test
    public void followUserWithUuid_WhenUserAlreadyFollows() throws Exception {
        UUID u1Uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");
        user.setUuid(u1Uuid);
        UUID u2Uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(u2Uuid)).willReturn(true);
        given(userRepository.checkIfUserWithUuidFollows(u1Uuid, u2Uuid)).willReturn(Optional.of(1L));

        // when
        boolean result = userService.followUserWithUuid(user, u2Uuid);

        // then
        assertTrue(result);
    }

    @Test
    public void followUserWithUuid_WhenFollowingFails() throws Exception {
        UUID u1Uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");
        user.setUuid(u1Uuid);
        UUID u2Uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(u2Uuid)).willReturn(true);
        given(userRepository.checkIfUserWithUuidFollows(u1Uuid, u2Uuid)).willReturn(Optional.empty());
        given(userRepository.followUserWithUuid(u1Uuid, u2Uuid)).willReturn(Optional.empty());

        // when
        boolean result = userService.followUserWithUuid(user, u2Uuid);

        // then
        assertFalse(result);
    }

    @Test
    public void followUserWithUuid_ThrowsWhenUserFollowsThemselves() throws Exception {
        UUID u1Uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");
        user.setUuid(u1Uuid);

        // given
        given(userRepository.existsById(u1Uuid)).willReturn(true);
        given(userRepository.checkIfUserWithUuidFollows(u1Uuid, u1Uuid)).willReturn(Optional.empty());

        // then
        String message = assertThrows(IllegalArgumentException.class, () -> {
            userService.followUserWithUuid(user, u1Uuid);
        }).getMessage();

        assertEquals("Users cannot follow themselves.", message);
    }

    @Test
    public void unfollowUserWithUuid_ThrowsWhenUserDoesntExist() {
        UUID u1Uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");
        user.setUuid(u1Uuid);
        UUID u2Uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(u2Uuid)).willReturn(false);

        // when
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.unfollowUserWithUuid(user, u2Uuid);
        }).getMessage();

        // then
        assertEquals(String.format("User with UUID %s doesn't exist", u2Uuid), message);
    }

    @Test
    public void unfollowUserWithUuid_WhenUnfollowSucceeds() throws Exception {
        UUID u1Uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");
        user.setUuid(u1Uuid);
        UUID u2Uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(u2Uuid)).willReturn(true);
        given(userRepository.checkIfUserWithUuidFollows(u1Uuid, u2Uuid)).willReturn(Optional.empty());

        // when
        boolean result = userService.unfollowUserWithUuid(user, u2Uuid);

        // then
        assertTrue(result);
    }

    @Test
    public void unfollowUserWithUuid_WhenUnfollowFails() throws Exception {
        UUID u1Uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");
        user.setUuid(u1Uuid);
        UUID u2Uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(u2Uuid)).willReturn(true);
        given(userRepository.checkIfUserWithUuidFollows(u1Uuid, u2Uuid)).willReturn(Optional.of(1L));

        // when
        boolean result = userService.unfollowUserWithUuid(user, u2Uuid);

        // then
        assertFalse(result);
    }

    @Test
    public void findAllFollowsOfUser_ThrowsWhenUserDoesntExist() {
        UUID uUuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uUuid)).willReturn(false);

        // when
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.findAllFollowsOfUser(uUuid, 0L, 5L);
        }).getMessage();

        // then
        assertEquals(String.format("User with UUID %s doesn't exist", uUuid), message);
    }

    @Test
    public void findAllFollowsOfUser_ThrowsWhenSkipAndLimitArgumentsNegative() {
        UUID uUuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uUuid)).willReturn(true);

        // then
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.findAllFollowsOfUser(uUuid, -1L, 0L);
        });

        assertEquals("Invalid skip and/or limit values.", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.findAllFollowsOfUser(uUuid, 0L, -1L);
        });

        assertEquals("Invalid skip and/or limit values.", ex.getMessage());
    }

    @Test
    public void findAllFollowsOfUser_ReturnsEmptyListIfNobodyFollowed() throws Exception {
        UUID uUuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uUuid)).willReturn(true);
        given(userRepository.findAllFollowsOfUserWithUuid(uUuid, 0L, 5L)).willReturn(List.of());

        // when
        List<User> followedBy = userService.findAllFollowsOfUser(uUuid, 0L, 5L);

        // then
        assertEquals(0, followedBy.size());
    }

    @Test
    public void findAllFollowsOfUser_ReturnsListOfFollowers() throws Exception {
        UUID uUuid = UUID.randomUUID();

        List<User> mockList = List.of(new User(), new User(), new User());

        // given
        given(userRepository.existsById(uUuid)).willReturn(true);
        given(userRepository.findAllFollowsOfUserWithUuid(uUuid, 0L, 5L)).willReturn(mockList);

        // when
        List<User> followedBy = userService.findAllFollowsOfUser(uUuid, 0L, 5L);

        // then
        assertEquals(3, followedBy.size());
    }

    @Test
    public void findAllFollowersOfUser_ThrowsWhenUserDoesntExist() {
        UUID uUuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uUuid)).willReturn(false);

        // when
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.findAllFollowersOfUser(uUuid, 0L, 5L);
        }).getMessage();

        // then
        assertEquals(String.format("User with UUID %s doesn't exist", uUuid), message);
    }

    @Test
    public void findAllFollowersOfUser_ThrowsWhenSkipAndLimitArgumentsNegative() {
        UUID uUuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uUuid)).willReturn(true);

        // then
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.findAllFollowersOfUser(uUuid, -1L, 0L);
        });

        assertEquals("Invalid skip and/or limit values.", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.findAllFollowersOfUser(uUuid, 0L, -1L);
        });

        assertEquals("Invalid skip and/or limit values.", ex.getMessage());
    }

    @Test
    public void findAllFollowersOfUser_ReturnsListOfFollowers() throws Exception {
        UUID uUuid = UUID.randomUUID();

        List<User> mockList = List.of(new User(), new User(), new User());

        // given
        given(userRepository.existsById(uUuid)).willReturn(true);
        given(userRepository.findAllFollowersOfUserWithUuid(uUuid, 0L, 5L)).willReturn(mockList);

        // when
        List<User> following = userService.findAllFollowersOfUser(uUuid, 0L, 5L);

        // then
        assertEquals(3, following.size());
    }

    @Test
    public void findAllFollowersOfUser_ReturnsEmptyListIfNobodyFollowed() throws Exception {
        UUID uUuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uUuid)).willReturn(true);
        given(userRepository.findAllFollowersOfUserWithUuid(uUuid, 0L, 5L)).willReturn(List.of());

        // when
        List<User> following = userService.findAllFollowersOfUser(uUuid, 0L, 5L);

        // then
        assertEquals(0, following.size());
    }

    @Test
    public void getUserProfileInfo_ThrowsWhenUserDoesntExist() {
        UUID uUuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uUuid)).willReturn(false);

        // then
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.getUserProfileInfo(uUuid);
        }).getMessage();

        assertEquals(String.format("User with UUID %s doesn't exist", uUuid), message);
    }

    @Test
    public void getUserProfileInfo_ReturnsPlaceholderObjectWhenFails() throws Exception {
        UUID uUuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uUuid)).willReturn(true);
        given(userRepository.getUserProfileInfo(uUuid)).willReturn(Optional.empty());

        // when
        UserProfileInfo profileInfo = userService.getUserProfileInfo(uUuid);

        // then
        assertNull(profileInfo.getUuid());
        assertNull(profileInfo.getFollows());
        assertNull(profileInfo.getFollowers());
    }

    @Test
    public void getUserProfileInfo_ReturnsObject() throws Exception {
        UUID uUuid = UUID.randomUUID();

        UserProfileInfo mockProfileInfo = new UserProfileInfo();
        mockProfileInfo.setUuid(uUuid);
        mockProfileInfo.setFollowers(10L);
        mockProfileInfo.setFollows(20L);

        // given
        given(userRepository.existsById(uUuid)).willReturn(true);
        given(userRepository.getUserProfileInfo(uUuid)).willReturn(Optional.of(mockProfileInfo));

        // when
        UserProfileInfo profileInfo = userService.getUserProfileInfo(uUuid);

        // then
        assertEquals(uUuid, profileInfo.getUuid());
        assertEquals(20L, profileInfo.getFollows());
        assertEquals(10L, profileInfo.getFollowers());
    }

    @Test
    public void findByUuid_ThrowsWhenUserDoesntExist() {
        UUID uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uuid)).willReturn(false);

        // then
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.findByUuid(uuid);
        }).getMessage();

        assertEquals(String.format("User with UUID %s doesn't exist", uuid), message);
    }

    @Test
    public void findByUuid_ThrowsWhenDatabaseFails() {
        UUID uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uuid)).willReturn(true);
        given(userRepository.findById(uuid)).willReturn(Optional.empty());

        // then
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.findByUuid(uuid);
        }).getMessage();

        assertEquals(String.format("User with UUID %s doesn't exist", uuid), message);
    }

    @Test
    public void findByUuid_ReturnsObject() throws Exception {
        UUID uuid = UUID.randomUUID();
        User user = new User();
        user.setUuid(uuid);

        // given
        given(userRepository.existsById(uuid)).willReturn(true);
        given(userRepository.findById(uuid)).willReturn(Optional.of(user));

        // when
        User foundUser = userService.findByUuid(uuid);

        //then
        assertNotNull(foundUser);
        assertEquals(uuid, foundUser.getUuid());
    }

    @Test
    public void checkIfUserFollows_ThrowsWhenUserDoesntExist() {
        UUID uuid = UUID.randomUUID();

        // given
        given(userRepository.existsById(uuid)).willReturn(false);

        // then
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.checkIfUserFollows(any(User.class), uuid);
        }).getMessage();

        assertEquals(String.format("User with UUID %s doesn't exist", uuid), message);
    }

    @Test
    public void checkIfUserFollows_ReturnsFalseWhenThereIsNoFollow() throws Exception {
        UUID uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");

        // given
        given(userRepository.existsById(uuid)).willReturn(true);
        given(userRepository.checkIfUserWithUuidFollows(user.getUuid(), uuid)).willReturn(Optional.empty());

        // when
        boolean result = userService.checkIfUserFollows(user, uuid);

        // then
        assertFalse(result);
    }

    @Test
    public void checkIfUserFollows_ReturnsTrueWhenThereIsFollow() throws Exception {
        UUID uuid = UUID.randomUUID();
        User user = new User("test1", "mail@test.com", "", "");

        // given
        given(userRepository.existsById(uuid)).willReturn(true);
        given(userRepository.checkIfUserWithUuidFollows(user.getUuid(), uuid)).willReturn(Optional.of(1L));

        // when
        boolean result = userService.checkIfUserFollows(user, uuid);

        // then
        assertTrue(result);
    }

    @Test
    public void findByUsername_ThrowsWhenUserDoesntExist() {
        String invalidUsername = "test321";

        // given
        given(userRepository.findByUsername(invalidUsername)).willReturn(Optional.empty());

        // then
        String message = assertThrows(UserDoesntExistException.class, () -> {
            userService.findByUsername(invalidUsername);
        }).getMessage();

        assertEquals(String.format("User %s doesn't exist", invalidUsername), message);
    }

    @Test
    public void findByUsername_ReturnsExistingObject() throws Exception {
        String username = "test321";
        User user = new User(username, "", "", "");

        // given
        given(userRepository.findByUsername(username))
                .willReturn(Optional.of(user));

        // when
        User receivedUser = userService.findByUsername(username);

        // then
        assertEquals(user, receivedUser);
    }
}
