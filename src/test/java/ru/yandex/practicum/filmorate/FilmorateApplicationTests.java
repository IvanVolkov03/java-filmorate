package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FilmorateApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Film testFilm;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@yandex.ru");
        testUser.setLogin("tester");
        testUser.setName("Test User");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));

        testFilm = new Film();
        testFilm.setName("Test Film");
        testFilm.setDescription("Test description");
        testFilm.setReleaseDate(LocalDate.of(2020, 1, 1));
        testFilm.setDuration(120);
    }

    @Test
    @DisplayName("POST /films — создание фильма")
    void shouldCreateFilm() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFilm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Film"));
    }

    @Test
    @DisplayName("GET /films/{id} — получение фильма по ID")
    void shouldGetFilmById() throws Exception {
        // Создаём фильм
        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFilm)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int filmId = objectMapper.readTree(response).get("id").asInt();

        // Получаем по ID
        mockMvc.perform(get("/films/" + filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(filmId));
    }

    @Test
    @DisplayName("GET /films/{id} — 404 если фильм не найден")
    void shouldReturn404IfFilmNotFound() throws Exception {
        mockMvc.perform(get("/films/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /films/{id}/like/{userId} — добавление лайка")
    void shouldAddLike() throws Exception {
        int userId = createTestUser();
        int filmId = createTestFilm();

        mockMvc.perform(put("/films/" + filmId + "/like/" + userId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /films/{id}/like/{userId} — удаление лайка")
    void shouldRemoveLike() throws Exception {
        int userId = createTestUser();
        int filmId = createTestFilm();

        mockMvc.perform(put("/films/" + filmId + "/like/" + userId));
        mockMvc.perform(delete("/films/" + filmId + "/like/" + userId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /films/popular — список популярных фильмов")
    void shouldGetPopularFilms() throws Exception {
        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /films/popular?count=5 — ограничение количества")
    void shouldGetPopularFilmsWithCount() throws Exception {
        mockMvc.perform(get("/films/popular").param("count", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /users — создание пользователя")
    void shouldCreateUser() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("GET /users/{id} — получение пользователя по ID")
    void shouldGetUserById() throws Exception {
        int userId = createTestUser();

        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));
    }

    @Test
    @DisplayName("PUT /users/{id}/friends/{friendId} — добавление в друзья")
    void shouldAddFriend() throws Exception {
        int user1Id = createTestUser();
        int user2Id = createTestUser();

        mockMvc.perform(put("/users/" + user1Id + "/friends/" + user2Id))
                .andExpect(status().isOk());

        // Проверяем двустороннюю дружбу
        mockMvc.perform(get("/users/" + user1Id + "/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(user2Id));
    }

    @Test
    @DisplayName("DELETE /users/{id}/friends/{friendId} — удаление из друзей")
    void shouldRemoveFriend() throws Exception {
        int user1Id = createTestUser();
        int user2Id = createTestUser();

        mockMvc.perform(put("/users/" + user1Id + "/friends/" + user2Id));
        mockMvc.perform(delete("/users/" + user1Id + "/friends/" + user2Id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + user1Id + "/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /users/{id}/friends/common/{otherId} — общие друзья")
    void shouldGetCommonFriends() throws Exception {
        int user1 = createTestUser();
        int user2 = createTestUser();
        int commonFriend = createTestUser();

        mockMvc.perform(put("/users/" + user1 + "/friends/" + commonFriend));
        mockMvc.perform(put("/users/" + user2 + "/friends/" + commonFriend));

        mockMvc.perform(get("/users/" + user1 + "/friends/common/" + user2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commonFriend));
    }

    @Test
    @DisplayName("POST /films — 400 при пустом названии")
    void shouldFailFilmWithEmptyName() throws Exception {
        testFilm.setName("");
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFilm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users — 400 при некорректном email")
    void shouldFailUserWithInvalidEmail() throws Exception {
        testUser.setEmail("invalid");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /users/{id} — 404 для несуществующего пользователя")
    void shouldReturn404OnUpdateUnknownUser() throws Exception {
        testUser.setId(9999);
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isNotFound());
    }

    private int createTestUser() throws Exception {
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asInt();
    }

    private int createTestFilm() throws Exception {
        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFilm)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asInt();
    }
}