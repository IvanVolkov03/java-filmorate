package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new HashMap<>();
    private final Map<Integer, Set<Integer>> userFriends = new HashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    @Override
    public User create(User user) {
        prepareUser(user);
        int id = idCounter.getAndIncrement();
        user.setId(id);
        users.put(id, user);
        userFriends.put(id, new HashSet<>());
        return user;
    }

    @Override
    public User update(User user) {
        if (!users.containsKey(user.getId())) {
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }
        prepareUser(user);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User findById(int id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
        return user;
    }

    @Override
    public void delete(int id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
        users.remove(id);
        userFriends.remove(id);
        // Удаляем этого пользователя из списков друзей других пользователей
        userFriends.values().forEach(friends -> friends.remove(id));
    }

    @Override
    public void addFriend(int userId, int friendId) {
        User user = findById(userId);
        User friend = findById(friendId);

        Set<Integer> userFriendsSet = userFriends.get(userId);
        Set<Integer> friendFriendsSet = userFriends.get(friendId);

        userFriendsSet.add(friendId);
        friendFriendsSet.add(userId); // Двусторонняя дружба
    }

    @Override
    public void removeFriend(int userId, int friendId) {
        User user = findById(userId);
        User friend = findById(friendId);

        Set<Integer> userFriendsSet = userFriends.get(userId);
        Set<Integer> friendFriendsSet = userFriends.get(friendId);

        userFriendsSet.remove(friendId);
        friendFriendsSet.remove(userId); // Двустороннее удаление
    }

    @Override
    public Set<Integer> getFriends(int userId) {
        findById(userId); // Проверяем существование
        return new HashSet<>(userFriends.getOrDefault(userId, new HashSet<>()));
    }

    @Override
    public Set<Integer> getCommonFriends(int userId, int otherUserId) {
        findById(userId);
        findById(otherUserId);

        Set<Integer> userFriendsSet = userFriends.getOrDefault(userId, new HashSet<>());
        Set<Integer> otherFriendsSet = userFriends.getOrDefault(otherUserId, new HashSet<>());

        return userFriendsSet.stream()
                .filter(otherFriendsSet::contains)
                .collect(Collectors.toSet());
    }

    private void prepareUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}