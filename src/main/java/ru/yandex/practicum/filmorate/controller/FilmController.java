package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        log.info("Получен запрос на добавление фильма: {}", film);
        Film createdFilm = filmService.create(film);
        log.info("Фильм успешно добавлен: {}", createdFilm);
        return createdFilm;
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);
        Film updatedFilm = filmService.update(film);
        log.info("Фильм успешно обновлен: {}", updatedFilm);
        return updatedFilm;
    }

    @GetMapping
    public List<Film> findAll() {
        log.info("Получен запрос на получение всех фильмов");
        return filmService.findAll();
    }

    @GetMapping("/{id}")
    public Film findById(@PathVariable int id) {
        log.info("Получен запрос на получение фильма с id={}", id);
        return filmService.findById(id);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable int id, @PathVariable int userId) {
        log.info("Получен запрос на добавление лайка: filmId={}, userId={}", id, userId);
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable int id, @PathVariable int userId) {
        log.info("Получен запрос на удаление лайка: filmId={}, userId={}", id, userId);
        filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(defaultValue = "10") int count) {
        log.info("Получен запрос на получение популярных фильмов: count={}", count);
        return filmService.getPopularFilms(count);
    }
}