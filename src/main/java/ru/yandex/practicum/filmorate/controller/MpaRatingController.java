package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRatingStorage;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaRatingController {
    private final MpaRatingStorage mpaStorage;

    @GetMapping
    public List<MpaRating> getAllRatings() {
        return mpaStorage.findAll();
    }

    @GetMapping("/{id}")
    public MpaRating getRatingById(@PathVariable int id) {
        return mpaStorage.findById(id);
    }
}