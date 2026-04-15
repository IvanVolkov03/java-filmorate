package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new HashMap<>();
    private final Map<Integer, Set<Integer>> filmLikes = new HashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @Override
    public Film create(Film film) {
        validateReleaseDate(film);
        int id = idCounter.getAndIncrement();
        film.setId(id);
        films.put(id, film);
        filmLikes.put(id, new HashSet<>());
        return film;
    }

    @Override
    public Film update(Film film) {
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }
        validateReleaseDate(film);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film findById(int id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        return film;
    }

    @Override
    public void delete(int id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        films.remove(id);
        filmLikes.remove(id);
    }

    @Override
    public void addLike(int filmId, int userId) {
        Film film = findById(filmId);
        Set<Integer> likes = filmLikes.get(filmId);
        if (likes == null) {
            likes = new HashSet<>();
            filmLikes.put(filmId, likes);
        }
        likes.add(userId);
    }

    @Override
    public void removeLike(int filmId, int userId) {
        Film film = findById(filmId);
        Set<Integer> likes = filmLikes.get(filmId);
        if (likes != null) {
            likes.remove(userId);
        }
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = filmLikes.getOrDefault(f1.getId(), new HashSet<>()).size();
                    int likes2 = filmLikes.getOrDefault(f2.getId(), new HashSet<>()).size();
                    return Integer.compare(likes2, likes1); // По убыванию
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ru.yandex.practicum.filmorate.exception.ValidationException(
                    "Дата релиза — не раньше 28 декабря 1895 года."
            );
        }
    }
}
