package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final Map<Integer, Film> films = new HashMap<>();
    private int idCounter = 1;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @PostMapping
    public Film create(@RequestBody Film film) {
        log.info("Получен запрос на добавление фильма: {}", film);
        validate(film);
        film.setId(idCounter++);
        films.put(film.getId(), film);
        log.info("Фильм успешно добавлен: {}", film);
        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);
        if (!films.containsKey(film.getId())) {
            log.warn("Ошибка обновления: фильм с id {} не найден", film.getId());
            throw new ValidationException("Фильм с таким ID не существует.");
        }
        validate(film);
        films.put(film.getId(), film);
        log.info("Фильм успешно обновлен: {}", film);
        return film;
    }

    @GetMapping
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    private void validate(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название не может быть пустым.");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Максимальная длина описания — 200 символов.");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года.");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительной.");
        }
    }
}
