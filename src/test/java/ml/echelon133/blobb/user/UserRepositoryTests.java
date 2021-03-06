package ml.echelon133.blobb.user;

import ml.echelon133.blobb.blobb.Blobb;
import ml.echelon133.blobb.blobb.BlobbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.*;

@DataNeo4jTest
public class UserRepositoryTests {

    private UserRepository userRepository;
    private BlobbRepository blobbRepository;

    @Autowired
    public UserRepositoryTests(UserRepository userRepository, BlobbRepository blobbRepository) {
        this.userRepository = userRepository;
        this.blobbRepository = blobbRepository;
    }

    @BeforeEach
    public void beforeEach() {
        User u1 = new User("user1", "user1@mail.com", "user1", "");
        User u2 = new User("user2", "user2@mail.com", "user2", "");
        User u3 = new User("user3", "user3@mail.com", "user3", "");
        User savedU1 = userRepository.save(u1);
        User savedU2 = userRepository.save(u2);
        User savedU3 = userRepository.save(u3);
        userRepository.followUserWithUuid(savedU1.getUuid(), savedU1.getUuid());
        userRepository.followUserWithUuid(savedU2.getUuid(), savedU2.getUuid());
        userRepository.followUserWithUuid(savedU3.getUuid(), savedU3.getUuid());
    }

    @Test
    public void savedUserGetsUuid() {
        User user = new User("test1", "test@mail.com", "test1", "");
        assertNull(user.getUuid());

        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getUuid());
    }

    @Test
    public void followUserWithUuid_LetsUsersFollowEachOther() {
        User u1 = userRepository.findByUsername("user1").orElse(null);
        User u2 = userRepository.findByUsername("user2").orElse(null);
        User u3 = userRepository.findByUsername("user3").orElse(null);

        // All these lists should be empty, because we don't have any relationships between users
        List<User> usersFollowedByU1 = userRepository.findAllFollowsOfUserWithUuid(u1.getUuid(), 0L, 5L);
        List<User> usersFollowedByU2 = userRepository.findAllFollowsOfUserWithUuid(u2.getUuid(), 0L, 5L);
        List<User> usersFollowedByU3 = userRepository.findAllFollowsOfUserWithUuid(u3.getUuid(), 0L, 5L);

        assertEquals(0, usersFollowedByU1.size());
        assertEquals(0, usersFollowedByU2.size());
        assertEquals(0, usersFollowedByU3.size());

        // u1 follows u2, u3
        userRepository.followUserWithUuid(u1.getUuid(), u2.getUuid());
        userRepository.followUserWithUuid(u1.getUuid(), u3.getUuid());

        // u2 follows u3
        userRepository.followUserWithUuid(u2.getUuid(), u3.getUuid());

        // u3 follows u1
        userRepository.followUserWithUuid(u3.getUuid(), u1.getUuid());

        // query the database again
        usersFollowedByU1 = userRepository.findAllFollowsOfUserWithUuid(u1.getUuid(), 0L, 5L);
        usersFollowedByU2 = userRepository.findAllFollowsOfUserWithUuid(u2.getUuid(), 0L, 5L);
        usersFollowedByU3 = userRepository.findAllFollowsOfUserWithUuid(u3.getUuid(), 0L, 5L);

        // check if u1 follows u2, u3
        assertEquals(2, usersFollowedByU1.size());
        assertEquals(1, usersFollowedByU1.stream().filter(u -> u.getUuid() == u2.getUuid()).count());
        assertEquals(1, usersFollowedByU1.stream().filter(u -> u.getUuid() == u3.getUuid()).count());

        // check if u2 follows u3
        assertEquals(1, usersFollowedByU2.size());
        assertEquals(1, usersFollowedByU2.stream().filter(u -> u.getUuid() == u3.getUuid()).count());

        // check if u3 follows u1
        assertEquals(1, usersFollowedByU3.size());
        assertEquals(1, usersFollowedByU3.stream().filter(u -> u.getUuid() == u1.getUuid()).count());
    }

    @Test
    public void unfollowUserWithUuid_LetsUsersUnfollowEachOther() {
        User u1 = userRepository.findByUsername("user1").orElse(null);
        User u2 = userRepository.findByUsername("user2").orElse(null);

        // u1 follows u2, u2 follows u1
        userRepository.followUserWithUuid(u1.getUuid(), u2.getUuid());
        userRepository.followUserWithUuid(u2.getUuid(), u1.getUuid());

        List<User> usersFollowedByU1 = userRepository.findAllFollowsOfUserWithUuid(u1.getUuid(), 0L, 5L);
        List<User> usersFollowedByU2 = userRepository.findAllFollowsOfUserWithUuid(u2.getUuid(), 0L, 5L);

        // check if u1 follows u2, and u2 follows u1
        assertEquals(1, usersFollowedByU1.size());
        assertEquals(1, usersFollowedByU1.stream().filter(u -> u.getUuid() == u2.getUuid()).count());
        assertEquals(1, usersFollowedByU2.size());
        assertEquals(1, usersFollowedByU2.stream().filter(u -> u.getUuid() == u1.getUuid()).count());

        // make u1 and u2 unfollow each other
        userRepository.unfollowUserWithUuid(u1.getUuid(), u2.getUuid());
        userRepository.unfollowUserWithUuid(u2.getUuid(), u1.getUuid());

        // check the database again
        usersFollowedByU1 = userRepository.findAllFollowsOfUserWithUuid(u1.getUuid(), 0L, 5L);
        usersFollowedByU2 = userRepository.findAllFollowsOfUserWithUuid(u2.getUuid(), 0L, 5L);

        // check if both lists are empty
        assertEquals(0, usersFollowedByU1.size());
        assertEquals(0, usersFollowedByU2.size());
    }

    @Test
    public void findAllFollowersOfUserWithUuid_ReturnsCorrectListOfFollowers() {
        User u1 = userRepository.findByUsername("user1").orElse(null);
        User u2 = userRepository.findByUsername("user2").orElse(null);

        // list of all who follow u1
        List<User> usersFollowingU1 = userRepository.findAllFollowersOfUserWithUuid(u1.getUuid(), 0L, 5L);
        assertEquals(0, usersFollowingU1.size());

        // follow u1 as u2
        userRepository.followUserWithUuid(u2.getUuid(), u1.getUuid());

        // refresh the list
        usersFollowingU1 = userRepository.findAllFollowersOfUserWithUuid(u1.getUuid(), 0L, 5L);

        // check if the list of u1 followers contains u2
        assertEquals(1, usersFollowingU1.size());
        assertEquals(1, usersFollowingU1.stream().filter(u -> u.getUuid() == u2.getUuid()).count());
    }

    @Test
    public void getUserProfileInfo_ReturnsCorrectCounterValues() {
        User u1 = userRepository.findByUsername("user1").orElse(null);
        User u2 = userRepository.findByUsername("user2").orElse(null);
        User u3 = userRepository.findByUsername("user3").orElse(null);

        UserProfileInfo u1ProfileInfo = userRepository.getUserProfileInfo(u1.getUuid()).orElse(null);

        // both followedBy and follows counters should be 0
        assertEquals(0, u1ProfileInfo.getFollowers());
        assertEquals(0, u1ProfileInfo.getFollows());

        // u2 and u3 follow u1
        userRepository.followUserWithUuid(u2.getUuid(), u1.getUuid());
        userRepository.followUserWithUuid(u3.getUuid(), u1.getUuid());

        // u1 follows u3
        userRepository.followUserWithUuid(u1.getUuid(), u3.getUuid());

        // update profile info
        u1ProfileInfo = userRepository.getUserProfileInfo(u1.getUuid()).orElse(null);

        // u1 is followed by two people (u2, u3) and follows one user (u3)
        assertEquals(2, u1ProfileInfo.getFollowers());
        assertEquals(1, u1ProfileInfo.getFollows());
    }

    @Test
    public void findAllFollowsOfUserWithUuid_LimitAndSkipArgumentsWork() {
        User u1 = userRepository.findByUsername("user1").orElse(null);
        User u2 = userRepository.findByUsername("user2").orElse(null);
        User u3 = userRepository.findByUsername("user3").orElse(null);

        // u1 follows u2, u3
        userRepository.followUserWithUuid(u1.getUuid(), u2.getUuid());
        userRepository.followUserWithUuid(u1.getUuid(), u3.getUuid());

        List<User> skip0limit1 = userRepository.findAllFollowsOfUserWithUuid(u1.getUuid(), 0L, 1L);
        List<User> skip0limit5 = userRepository.findAllFollowsOfUserWithUuid(u1.getUuid(), 0L, 5L);
        List<User> skip1limit5 = userRepository.findAllFollowsOfUserWithUuid(u1.getUuid(), 1L, 5L);

        assertEquals(1, skip0limit1.size());
        assertEquals(2, skip0limit5.size());
        assertEquals(1, skip1limit5.size());
    }

    @Test
    public void findAllFollowersOfUserWithUuid_LimitAndSkipArgumentsWork() {
        User u1 = userRepository.findByUsername("user1").orElse(null);
        User u2 = userRepository.findByUsername("user2").orElse(null);
        User u3 = userRepository.findByUsername("user3").orElse(null);

        // u3 follows u1
        userRepository.followUserWithUuid(u3.getUuid(), u1.getUuid());
        // u2 follows u1
        userRepository.followUserWithUuid(u2.getUuid(), u1.getUuid());

        List<User> skip0limit1 = userRepository.findAllFollowersOfUserWithUuid(u1.getUuid(), 0L, 1L);
        List<User> skip0limit5 = userRepository.findAllFollowersOfUserWithUuid(u1.getUuid(), 0L, 5L);
        List<User> skip1limit5 = userRepository.findAllFollowersOfUserWithUuid(u1.getUuid(), 1L, 5L);

        assertEquals(1, skip0limit1.size());
        assertEquals(2, skip0limit5.size());
        assertEquals(1, skip1limit5.size());
    }

    @Test
    public void checkIfUserWithUuidFollows_Works() {
        User u1 = userRepository.findByUsername("user1").orElse(null);
        User u2 = userRepository.findByUsername("user2").orElse(null);

        Optional<Long> u1FollowsU2 = userRepository.checkIfUserWithUuidFollows(u1.getUuid(), u2.getUuid());
        Optional<Long> u2FollowsU1 = userRepository.checkIfUserWithUuidFollows(u2.getUuid(), u1.getUuid());

        assertTrue(u1FollowsU2.isEmpty());
        assertTrue(u2FollowsU1.isEmpty());

        // u1 follows u2
        userRepository.followUserWithUuid(u1.getUuid(), u2.getUuid());
        // u2 follows u1
        userRepository.followUserWithUuid(u2.getUuid(), u1.getUuid());

        u1FollowsU2 = userRepository.checkIfUserWithUuidFollows(u1.getUuid(), u2.getUuid());
        u2FollowsU1 = userRepository.checkIfUserWithUuidFollows(u2.getUuid(), u1.getUuid());

        assertTrue(u1FollowsU2.isPresent());
        assertTrue(u2FollowsU1.isPresent());
    }

    @Test
    public void findRecentBlobbsOfUser_IsEmptyWhenNoPostsMade() {
        User u1 = userRepository.findByUsername("user1").orElse(null);

        // when
        List<UserBlobb> recent = userRepository
                .findRecentBlobbsOfUser(u1.getUuid(), 0L, 10L);

        // then
        assertEquals(0, recent.size());
    }

    @Test
    public void findRecentBlobbsOfUser_ReturnsObjectsInCorrectOrder() {
        User u1 = userRepository.findByUsername("user1").orElse(null);

        Date now = new Date();
        Date hourAgo = Date.from(Instant.now().minus(1, HOURS));
        Date twoHoursAgo = Date.from(Instant.now().minus(2, HOURS));

        // three u1 blobbs
        Blobb b1 = new Blobb(u1, "content1");
        b1.setCreationDate(now);
        Blobb b2 = new Blobb(u1, "content2");
        b2.setCreationDate(hourAgo);
        Blobb b3 = new Blobb(u1, "content3");
        b3.setCreationDate(twoHoursAgo);

        blobbRepository.save(b1);
        blobbRepository.save(b2);
        blobbRepository.save(b3);

        // when
        List<UserBlobb> recent = userRepository
                .findRecentBlobbsOfUser(u1.getUuid(), 0L, 10L);

        // then
        List<String> recentContents = recent.stream().map(UserBlobb::getContent).collect(Collectors.toList());

        assertEquals(3, recent.size());

        List<String> expectedOrder = List.of("content1", "content2", "content3");

        for (int i = 0; i < recent.size(); i++) {
            assertEquals(expectedOrder.get(i), recentContents.get(i));
        }
    }

    @Test
    public void findRecentBlobbsOfUser_LimitsNumberOfResults() {
        User u1 = userRepository.findByUsername("user1").orElse(null);

        Date now = new Date();
        Date hourAgo = Date.from(Instant.now().minus(1, HOURS));
        Date twoHoursAgo = Date.from(Instant.now().minus(2, HOURS));

        // three u1 blobbs
        Blobb b1 = new Blobb(u1, "content1");
        b1.setCreationDate(now);
        Blobb b2 = new Blobb(u1, "content2");
        b2.setCreationDate(hourAgo);
        Blobb b3 = new Blobb(u1, "content3");
        b3.setCreationDate(twoHoursAgo);

        blobbRepository.save(b1);
        blobbRepository.save(b2);
        blobbRepository.save(b3);

        // when
        List<UserBlobb> recent = userRepository
                .findRecentBlobbsOfUser(u1.getUuid(), 0L, 1L);

        // then
        List<String> recentContents = recent.stream().map(UserBlobb::getContent).collect(Collectors.toList());

        assertEquals(1, recent.size());

        List<String> expectedOrder = Collections.singletonList("content1");

        for (int i = 0; i < recent.size(); i++) {
            assertEquals(expectedOrder.get(i), recentContents.get(i));
        }
    }

    @Test
    public void findRecentBlobbsOfUser_SkipsResults() {
        User u1 = userRepository.findByUsername("user1").orElse(null);

        Date now = new Date();
        Date hourAgo = Date.from(Instant.now().minus(1, HOURS));
        Date twoHoursAgo = Date.from(Instant.now().minus(2, HOURS));

        // three u1 blobbs
        Blobb b1 = new Blobb(u1, "content1");
        b1.setCreationDate(now);
        Blobb b2 = new Blobb(u1, "content2");
        b2.setCreationDate(hourAgo);
        Blobb b3 = new Blobb(u1, "content3");
        b3.setCreationDate(twoHoursAgo);

        blobbRepository.save(b1);
        blobbRepository.save(b2);
        blobbRepository.save(b3);

        // when
        List<UserBlobb> recent = userRepository
                .findRecentBlobbsOfUser(u1.getUuid(), 1L, 5L);
        // then
        List<String> recentContents = recent.stream().map(UserBlobb::getContent).collect(Collectors.toList());

        assertEquals(2, recent.size());

        List<String> expectedOrder = List.of("content2", "content3");

        for (int i = 0; i < recent.size(); i++) {
            assertEquals(expectedOrder.get(i), recentContents.get(i));
        }
    }

    @Test
    public void findRecentBlobbsOfUser_DoesNotShowBlobbsOfOtherUsers() {
        User u1 = userRepository.findByUsername("user1").orElse(null);
        User u2 = userRepository.findByUsername("user2").orElse(null);

        // create one blobb as u1
        Blobb b1 = new Blobb(u1, "content1");
        blobbRepository.save(b1);

        // when
        List<UserBlobb> recent = userRepository
                .findRecentBlobbsOfUser(u2.getUuid(), 0L, 5L);

        // then
        assertEquals(0, recent.size());
    }
}
