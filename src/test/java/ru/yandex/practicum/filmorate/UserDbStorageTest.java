package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    void create_shouldSaveUserAndReturnWithId() {
        User user = User.builder()
                .email("test@test.com")
                .login("testuser")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User saved = userStorage.create(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void findById_shouldReturnUser_whenExists() {
        User user = userStorage.create(User.builder()
                .email("t@test.com").login("t").name("T").birthday(LocalDate.of(1990, 1, 1)).build());

        User found = userStorage.findById(user.getId());

        assertThat(found.getId()).isEqualTo(user.getId());
        assertThat(found.getLogin()).isEqualTo("t");
    }

    @Test
    void findById_shouldThrowNotFoundException_whenNotExists() {
        assertThatThrownBy(() -> userStorage.findById(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void update_shouldModifyUser() {
        User user = userStorage.create(User.builder()
                .email("old@test.com").login("old").name("Old").birthday(LocalDate.of(1990, 1, 1)).build());

        user.setEmail("new@test.com");
        user.setName("New Name");
        User updated = userStorage.update(user);

        assertThat(updated.getEmail()).isEqualTo("new@test.com");
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        userStorage.create(User.builder().email("a@test.com").login("a").name("A").birthday(LocalDate.of(1990,1,1)).build());
        userStorage.create(User.builder().email("b@test.com").login("b").name("B").birthday(LocalDate.of(1991,1,1)).build());

        List<User> users = userStorage.findAll();

        assertThat(users).hasSize(2);
    }

    @Test
    void addFriend_shouldCreatePendingFriendship() {
        User u1 = userStorage.create(User.builder()
                .email("u1@test.com").login("u1").name("U1").birthday(LocalDate.of(1990,1,1)).build());
        User u2 = userStorage.create(User.builder()
                .email("u2@test.com").login("u2").name("U2").birthday(LocalDate.of(1991,1,1)).build());

        userStorage.addFriend(u1.getId(), u2.getId());

        // После addFriend дружба в статусе 'pending', поэтому getFriends вернёт пустой список
        Set<Integer> pendingFriends = userStorage.getFriends(u1.getId());
        assertThat(pendingFriends).doesNotContain(u2.getId());

        // Проверяем, что заявка создана
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ? AND status = 'pending'",
                Integer.class, u1.getId(), u2.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void confirmFriend_shouldMakeFriendshipConfirmed() {
        User u1 = userStorage.create(User.builder()
                .email("u1@test.com").login("u1").name("U1").birthday(LocalDate.of(1990,1,1)).build());
        User u2 = userStorage.create(User.builder()
                .email("u2@test.com").login("u2").name("U2").birthday(LocalDate.of(1991,1,1)).build());

        // u1 добавляет u2 в друзья
        userStorage.addFriend(u1.getId(), u2.getId());

        // u2 подтверждает дружбу
        userStorage.confirmFriend(u1.getId(), u2.getId());

        // Теперь u1 должен быть в списке друзей u2
        Set<Integer> confirmedFriends = userStorage.getFriends(u2.getId());
        assertThat(confirmedFriends).contains(u1.getId());
    }

    @Test
    void getCommonFriends_shouldReturnIntersection() {
        User u1 = userStorage.create(User.builder()
                .email("u1@test.com").login("u1").name("U1").birthday(LocalDate.of(1990,1,1)).build());
        User u2 = userStorage.create(User.builder()
                .email("u2@test.com").login("u2").name("U2").birthday(LocalDate.of(1991,1,1)).build());
        User u3 = userStorage.create(User.builder()
                .email("u3@test.com").login("u3").name("U3").birthday(LocalDate.of(1992,1,1)).build());

        // u1 и u2 добавляют u3 в друзья
        userStorage.addFriend(u1.getId(), u3.getId());
        userStorage.addFriend(u2.getId(), u3.getId());

        // u3 подтверждает обе заявки
        userStorage.confirmFriend(u1.getId(), u3.getId());
        userStorage.confirmFriend(u2.getId(), u3.getId());

        // Теперь u3 должен быть общим другом
        Set<Integer> common = userStorage.getCommonFriends(u1.getId(), u2.getId());
        assertThat(common).contains(u3.getId());
    }
}