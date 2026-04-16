package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film create(Film film) {
        validateReleaseDate(film);
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql, new String[]{"id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setLong(4, film.getDuration());
            ps.setObject(5, film.getMpaRatingId());
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        film.setId(id);
        if (film.getGenreIds() != null && !film.getGenreIds().isEmpty()) {
            saveFilmGenres(id, film.getGenreIds());
        }
        log.info("Фильм добавлен в БД: id={}", id);
        return film;
    }

    @Override
    public Film update(Film film) {
        validateReleaseDate(film);
        Film existingFilm = findById(film.getId());

        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, " +
                "duration = ?, mpa_rating_id = ? WHERE id = ?";

        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpaRatingId(),
                film.getId()
        );
        if (film.getGenreIds() != null) {
            updateFilmGenres(film.getId(), film.getGenreIds());
        }
        log.info("Фильм обновлен в БД: id={}", film.getId());
        return film;
    }

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, fg.genre_id " +
                "FROM films f " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "ORDER BY f.id";

        List<Film> films = new ArrayList<>();
        Map<Integer, Film> filmMap = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            int filmId = rs.getInt("id");

            Film film = filmMap.get(filmId);
            if (film == null) {
                film = mapFilmFromRow(rs);
                filmMap.put(filmId, film);
                films.add(film);
            }

            Integer genreId = rs.getObject("genre_id", Integer.class);
            if (genreId != null) {
                if (film.getGenreIds() == null) {
                    film.setGenreIds(new HashSet<>());
                }
                film.getGenreIds().add(genreId);
            }
        });

        return films;
    }

    @Override
    public Film findById(int id) {
        String sql = "SELECT f.*, fg.genre_id " +
                "FROM films f " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "WHERE f.id = ?";

        List<Film> filmContainer = new ArrayList<>();

        jdbcTemplate.query(sql, rs -> {
            if (filmContainer.isEmpty()) {
                Film film = mapFilmFromRow(rs);
                filmContainer.add(film);
            }

            Film film = filmContainer.get(0);
            Integer genreId = rs.getObject("genre_id", Integer.class);
            if (genreId != null) {
                if (film.getGenreIds() == null) {
                    film.setGenreIds(new HashSet<>());
                }
                film.getGenreIds().add(genreId);
            }
        }, id);

        if (filmContainer.isEmpty()) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }

        return filmContainer.get(0);
    }

    @Override
    public void delete(int id) {
        Film film = findById(id);
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
        log.info("Фильм удален из БД: id={}", id);
    }

    @Override
    public void addLike(int filmId, int userId) {
        findById(filmId);
        String sql = "MERGE INTO likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
        log.info("Лайк добавлен: filmId={}, userId={}", filmId, userId);
    }

    @Override
    public void removeLike(int filmId, int userId) {
        findById(filmId);
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.info("Лайк удален: filmId={}, userId={}", filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, fg.genre_id, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "GROUP BY f.id, fg.genre_id " +
                "ORDER BY likes_count DESC, f.id " +
                "LIMIT ?";

        List<Film> films = new ArrayList<>();
        Map<Integer, Film> filmMap = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            int filmId = rs.getInt("id");

            Film film = filmMap.get(filmId);
            if (film == null) {
                film = mapFilmFromRow(rs);
                filmMap.put(filmId, film);
                films.add(film);
            }

            Integer genreId = rs.getObject("genre_id", Integer.class);
            if (genreId != null) {
                if (film.getGenreIds() == null) {
                    film.setGenreIds(new HashSet<>());
                }
                film.getGenreIds().add(genreId);
            }
        }, count);

        return films;
    }

    private Film mapFilmFromRow(ResultSet rs) throws SQLException {
        Film.FilmBuilder builder = Film.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date").toLocalDate())
                .duration(rs.getLong("duration"));

        Integer mpaRatingId = rs.getObject("mpa_rating_id", Integer.class);
        if (mpaRatingId != null) {
            builder.mpaRatingId(mpaRatingId);
        }

        return builder.build();
    }

    private void saveFilmGenres(int filmId, Set<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return;
        }
        String sql = "MERGE INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            private final List<Integer> genreList = new ArrayList<>(genreIds);

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, filmId);
                ps.setInt(2, genreList.get(i));
            }

            @Override
            public int getBatchSize() {
                return genreList.size();
            }
        });
    }

    private void updateFilmGenres(int filmId, Set<Integer> genreIds) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);
        if (genreIds != null && !genreIds.isEmpty()) {
            String sql = "MERGE INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                private final List<Integer> genreList = new ArrayList<>(genreIds);

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setInt(1, filmId);
                    ps.setInt(2, genreList.get(i));
                }

                @Override
                public int getBatchSize() {
                    return genreList.size();
                }
            });
        }
    }

    private void validateReleaseDate(Film film) {
        LocalDate cinemaBirthday = LocalDate.of(1895, 12, 28);
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(cinemaBirthday)) {
            throw new ru.yandex.practicum.filmorate.exception.ValidationException(
                    "Дата релиза — не раньше 28 декабря 1895 года."
            );
        }
    }
}