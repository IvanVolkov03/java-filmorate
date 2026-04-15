package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Film {
    private Integer id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private Long duration;
    private Integer mpaRatingId;
    private Set<Integer> genreIds;
    private MpaRating mpaRating;
    private MpaRating mpa;
    private Set<Genre> genres;

    public void setMpa(MpaRating mpa) {
        this.mpa = mpa;
        if (mpa != null && mpa.getId() != null) {
            this.mpaRatingId = mpa.getId();
        }
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
        if (genres != null && !genres.isEmpty()) {
            this.genreIds = genres.stream()
                    .map(Genre::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}