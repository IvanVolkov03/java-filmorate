package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FilmorateApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Тесты Film

    @Test
    @DisplayName("Должен успешно создать корректный фильм")
    void shouldCreateValidFilm() throws Exception {
        Film film = new Film();
        film.setName("Inception");
        film.setDescription("Dream within a dream");
        film.setReleaseDate(LocalDate.of(2010, 7, 16));
        film.setDuration(148);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Inception"));
    }

    @Test
    @DisplayName("Должен вернуть 400 при пустом названии фильма")
    void shouldFailFilmWithEmptyName() throws Exception {
        Film film = new Film();
        film.setName(""); // Ошибка: @NotBlank
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.now());
        film.setDuration(100);
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть 400 при описании фильма > 200 символов")
    void shouldFailFilmWithTooLongDescription() throws Exception {
        Film film = new Film();
        film.setName("Movie");
        film.setDescription("A".repeat(201)); // Ошибка: @Size(max=200)
        film.setReleaseDate(LocalDate.now());
        film.setDuration(100);

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть 400 при дате релиза раньше 28.12.1895")
    void shouldFailFilmWithEarlyReleaseDate() throws Exception {
        Film film = new Film();
        film.setName("Old Movie");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(100);

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть 400 при отрицательной длительности фильма")
    void shouldFailFilmWithNegativeDuration() throws Exception {
        Film film = new Film();
        film.setName("Fast");
        film.setDuration(-10); // Ошибка: @Positive

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    // Тесты для User

    @Test
    @DisplayName("Должен успешно создать корректного пользователя")
    void shouldCreateValidUser() throws Exception {
        User user = new User();
        user.setEmail("test@yandex.ru");
        user.setLogin("tester");
        user.setName("John Doe");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Должен вернуть 400 при некорректном формате Email")
    void shouldFailUserWithInvalidEmail() throws Exception {
        User user = new User();
        user.setEmail("invalid-email"); // Ошибка: @Email
        user.setLogin("tester");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен вернуть 400 при пустом логине")
    void shouldFailUserWithSpacesInLogin() throws Exception {
        User user = new User();
        user.setEmail("mail@mail.ru");
        user.setLogin("my login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен подставить логин в имя, если оно пустое")
    void shouldSetLoginAsNameIfNameIsEmpty() throws Exception {
        User user = new User();
        user.setEmail("mail@mail.ru");
        user.setLogin("only_login");
        user.setName(""); // Должно замениться на only_login
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("only_login"));
    }

    @Test
    @DisplayName("Должен вернуть 400 при дате рождения в будущем")
    void shouldFailUserWithFutureBirthday() throws Exception {
        User user = new User();
        user.setEmail("mail@mail.ru");
        user.setLogin("tester");
        user.setBirthday(LocalDate.now().plusDays(1)); // Ошибка: @PastOrPresent

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен успешно обновить существующего пользователя")
    void shouldUpdateUserSuccessfully() throws Exception {
        User user = new User();
        user.setEmail("update@mail.ru");
        user.setLogin("updater");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        user.setId(1);
        user.setName("Updated Name");
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }
}
