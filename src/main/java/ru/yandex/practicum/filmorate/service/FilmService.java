package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaRatingStorage mpaRatingStorage;

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    public Film create(Film film) {
        validateFilm(film);
        validateAndSetIds(film);
        validateGenres(film.getGenreIds());
        validateMpaRating(film.getMpaRatingId());

        Film saved = filmStorage.create(film);
        return enrichFilm(saved);
    }

    public Film update(Film film) {
        validateFilm(film);
        validateAndSetIds(film);
        validateGenres(film.getGenreIds());
        validateMpaRating(film.getMpaRatingId());

        Film updated = filmStorage.update(film);
        return enrichFilm(updated);
    }

    public List<Film> findAll() {
        return filmStorage.findAll().stream()
                .map(this::enrichFilm)
                .collect(Collectors.toList());
    }

    public Film findById(int id) {
        Film film = filmStorage.findById(id);
        return enrichFilm(film);
    }

    public void addLike(int filmId, int userId) {
        log.info("Добавление лайка: filmId={}, userId={}", filmId, userId);
        userStorage.findById(userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(int filmId, int userId) {
        log.info("Удаление лайка: filmId={}, userId={}", filmId, userId);
        userStorage.findById(userId);
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        log.info("Получение популярных фильмов, count={}", count);
        return filmStorage.getPopularFilms(count).stream()
                .map(this::enrichFilm)
                .collect(Collectors.toList());
    }

    private void validateAndSetIds(Film film) {
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            film.setMpaRatingId(film.getMpa().getId());
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Integer> ids = film.getGenres().stream()
                    .map(Genre::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            film.setGenreIds(ids);
        }
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года");
        }
        if (film.getDuration() != null && film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительной");
        }
    }

    private void validateGenres(Set<Integer> genreIds) {
        if (genreIds != null) {
            for (Integer genreId : genreIds) {
                try {
                    genreStorage.findById(genreId);
                } catch (NotFoundException e) {
                    throw new NotFoundException("Жанр с ID " + genreId + " не найден");
                }
            }
        }
    }

    private void validateMpaRating(Integer mpaRatingId) {
        if (mpaRatingId != null) {
            try {
                mpaRatingStorage.findById(mpaRatingId);
            } catch (NotFoundException e) {
                throw new NotFoundException("Рейтинг с ID " + mpaRatingId + " не найден");
            }
        }
    }

    private Film enrichFilm(Film film) {
        if (film.getMpaRatingId() != null) {
            MpaRating mpa = mpaRatingStorage.findById(film.getMpaRatingId());
            film.setMpaRating(mpa);
            film.setMpa(mpa);
        }
        if (film.getGenreIds() != null && !film.getGenreIds().isEmpty()) {
            Set<Genre> genres = film.getGenreIds().stream()
                    .map(genreStorage::findById)
                    .collect(Collectors.toSet());
            film.setGenres(genres);
        } else {
            film.setGenres(new HashSet<>());
        }

        return film;
    }
}