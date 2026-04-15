package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User create(User user) {
        prepareUser(user);

        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday()
        );

        SqlRowSet idRows = jdbcTemplate.queryForRowSet(
                "SELECT id FROM users WHERE email = ? AND login = ?",
                user.getEmail(),
                user.getLogin()
        );

        if (idRows.next()) {
            int id = idRows.getInt("id");
            user.setId(id);
            log.info("Пользователь добавлен в БД: id={}", id);
        }

        return user;
    }

    @Override
    public User update(User user) {
        User existingUser = findById(user.getId());
        prepareUser(user);

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId()
        );

        log.info("Пользователь обновлен в БД: id={}", user.getId());
        return user;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        return jdbcTemplate.query(sql, this::mapUserFromRow);
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, this::mapUserFromRow, id);

        if (users.isEmpty()) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
        return users.get(0);
    }

    @Override
    public void delete(int id) {
        User user = findById(id);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        log.info("Пользователь удален из БД: id={}", id);
    }

    @Override
    public void addFriend(int userId, int friendId) {
        findById(userId);
        findById(friendId);

        String sql = "MERGE INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'confirmed')";
        jdbcTemplate.update(sql, userId, friendId);

        log.info("Дружба добавлена: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public void removeFriend(int userId, int friendId) {
        findById(userId);
        findById(friendId);

        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);

        log.info("Дружба удалена: userId={}, friendId={}", userId, friendId);
    }

    public void confirmFriend(int userId, int friendId) {
        findById(userId);
        findById(friendId);
        String sql = "MERGE INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'confirmed')";
        jdbcTemplate.update(sql, friendId, userId);
        log.info("Дружба подтверждена: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public Set<Integer> getFriends(int userId) {
        findById(userId);

        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Integer.class, userId));
    }

    @Override
    public Set<Integer> getCommonFriends(int userId, int otherUserId) {
        findById(userId);
        findById(otherUserId);

        Set<Integer> userFriends = getFriends(userId);
        Set<Integer> otherFriends = getFriends(otherUserId);

        // Пересечение множеств
        userFriends.retainAll(otherFriends);
        return userFriends;
    }

    private User mapUserFromRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    }

    private void prepareUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}