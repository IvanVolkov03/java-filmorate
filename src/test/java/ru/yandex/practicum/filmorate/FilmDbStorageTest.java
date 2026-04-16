package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(FilmDbStorage.class)
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Очищаем в правильном порядке (сначала дочерние таблицы)
        jdbcTemplate.update("DELETE FROM likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");

        // Создаём тестовых пользователей
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                "user1@test.com", "user1", "User 1", LocalDate.of(1990, 1, 1));
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                "user2@test.com", "user2", "User 2", LocalDate.of(1991, 1, 1));
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                "user3@test.com", "user3", "User 3", LocalDate.of(1992, 1, 1));
    }

    @Test
    void create_shouldSaveFilmAndReturnWithId() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Desc")
                .releaseDate(LocalDate.of(2024, 1, 1))
                .duration(120L)
                .mpaRatingId(3)
                .build();

        Film saved = filmStorage.create(film);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Film");
    }

    @Test
    void findById_shouldReturnFilm() {
        Film film = filmStorage.create(Film.builder()
                .name("F")
                .description("D")
                .releaseDate(LocalDate.of(2024, 1, 1))
                .duration(100L)
                .mpaRatingId(1)
                .build());

        Film found = filmStorage.findById(film.getId());

        assertThat(found.getId()).isEqualTo(film.getId());
        assertThat(found.getMpaRatingId()).isEqualTo(1);
    }

    @Test
    void addLike_shouldIncrementLikes() {
        Film film = filmStorage.create(Film.builder()
                .name("F")
                .description("D")
                .releaseDate(LocalDate.of(2024, 1, 1))
                .duration(100L)
                .mpaRatingId(1)
                .build());

        // Пользователи с id=1 и id=2 уже созданы в setUp()
        filmStorage.addLike(film.getId(), 1);
        filmStorage.addLike(film.getId(), 2);

        List<Film> popular = filmStorage.getPopularFilms(10);
        assertThat(popular).hasSize(1);
        assertThat(popular.get(0).getId()).isEqualTo(film.getId());
    }

    @Test
    void getPopularFilms_shouldReturnSortedByLikes() {
        Film f1 = filmStorage.create(Film.builder()
                .name("F1")
                .description("D")
                .releaseDate(LocalDate.of(2024, 1, 1))
                .duration(100L)
                .mpaRatingId(1)
                .build());
        Film f2 = filmStorage.create(Film.builder()
                .name("F2")
                .description("D")
                .releaseDate(LocalDate.of(2024, 1, 1))
                .duration(100L)
                .mpaRatingId(1)
                .build());

        // Пользователи с id=1,2,3 уже созданы в setUp()
        filmStorage.addLike(f1.getId(), 1);
        filmStorage.addLike(f1.getId(), 2);
        filmStorage.addLike(f1.getId(), 3);
        filmStorage.addLike(f2.getId(), 1);

        List<Film> popular = filmStorage.getPopularFilms(10);
        assertThat(popular.get(0).getId()).isEqualTo(f1.getId());
        assertThat(popular.get(1).getId()).isEqualTo(f2.getId());
    }
}