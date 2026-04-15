package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MpaRatingDbStorage implements MpaRatingStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MpaRating> mpaRowMapper = (rs, rowNum) ->
            new MpaRating(rs.getInt("id"), rs.getString("name"));

    @Override
    public List<MpaRating> findAll() {
        return jdbcTemplate.query("SELECT * FROM mpa_ratings ORDER BY id", mpaRowMapper);
    }

    @Override
    public MpaRating findById(int id) {
        List<MpaRating> ratings = jdbcTemplate.query(
                "SELECT * FROM mpa_ratings WHERE id = ?", mpaRowMapper, id);
        if (ratings.isEmpty()) {
            throw new NotFoundException("Рейтинг с ID " + id + " не найден");
        }
        return ratings.get(0);
    }
}