package ru.yandex.practicum.filmorate.storage.mpa;

import ru.yandex.practicum.filmorate.model.MpaRating;
import java.util.List;

public interface MpaRatingStorage {
    List<MpaRating> findAll();

    MpaRating findById(int id);
}