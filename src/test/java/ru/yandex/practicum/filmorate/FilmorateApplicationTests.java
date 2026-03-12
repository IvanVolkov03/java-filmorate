package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {
    @Autowired
    private FilmController filmController;

    @Autowired
    private UserController userController;

    @Test
    void contextLoads() {
        assertNotNull(filmController);
        assertNotNull(userController);
    }

    // Тесты Film

    @Test
    void shouldCreateValidFilm() {
        Film film = new Film();
        film.setName("Inception");
        film.setDescription("Dream within a dream");
        film.setReleaseDate(LocalDate.of(2010, 7, 16));
        film.setDuration(148);

        Film saved = filmController.create(film);
        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
    }

    @Test
    void shouldFailFilmWithEmptyName() {
        Film film = new Film();
        film.setName(""); // Ошибка: пустое имя
        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void shouldFailFilmWithTooLongDescription() {
        Film film = new Film();
        film.setName("Movie");
        film.setDescription("A".repeat(201)); // Ошибка: > 200 символов
        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void shouldFailFilmWithEarlyReleaseDate() {
        Film film = new Film();
        film.setName("Old Movie");
        film.setReleaseDate(LocalDate.of(1895, 12, 27)); // Ошибка: раньше 28.12.1895
        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void shouldFailFilmWithNegativeDuration() {
        Film film = new Film();
        film.setName("Fast");
        film.setDuration(-10); // Ошибка: отрицательная длительность
        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void shouldUpdateFilmSuccessfully() {
        Film film = new Film();
        film.setName("Original");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        Film created = filmController.create(film);
        created.setName("Updated");
        Film updated = filmController.update(created);
        assertEquals("Updated", updated.getName());
    }

    // Тесты User

    @Test
    void shouldCreateValidUser() {
        User user = new User();
        user.setEmail("test@gmail.com");
        user.setLogin("tester");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        User saved = userController.create(user);
        assertNotNull(saved);
        assertEquals("tester", saved.getName()); // Проверка: имя взято из логина
    }

    @Test
    void shouldFailUserWithInvalidEmail() {
        User user = new User();
        user.setEmail("bad-email"); // Ошибка: нет @
        user.setLogin("login");
        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void shouldFailUserWithSpacesInLogin() {
        User user = new User();
        user.setEmail("mail@mail.ru");
        user.setLogin("my login"); // Ошибка: пробел
        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void shouldFailUserWithFutureBirthday() {
        User user = new User();
        user.setEmail("mail@mail.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.now().plusDays(1)); // Ошибка: в будущем
        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void shouldUpdateUserWithExistingId() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("old_login");
        user.setBirthday(LocalDate.of(1980, 5, 5));
        User created = userController.create(user);
        created.setLogin("new_login");
        User updated = userController.update(created);
        assertEquals("new_login", updated.getLogin());
    }
}
