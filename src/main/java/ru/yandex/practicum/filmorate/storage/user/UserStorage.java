package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;
import java.util.List;
import java.util.Set;

public interface UserStorage {

    User create(User user);

    User update(User user);

    List<User> findAll();

    User findById(int id);

    void delete(int id);

    void addFriend(int userId, int friendId);

    void removeFriend(int userId, int friendId);

    Set<Integer> getFriends(int userId);

    Set<Integer> getCommonFriends(int userId, int otherUserId);
}