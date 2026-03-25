package vn.com.anhemsoftware.license_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import vn.com.anhemsoftware.license_app.entity.Priority;
import vn.com.anhemsoftware.license_app.entity.Task;
import vn.com.anhemsoftware.license_app.exception.GlobalExceptionHandler;
import vn.com.anhemsoftware.license_app.exception.TaskNotFoundException;
import vn.com.anhemsoftware.license_app.service.S3Service;
import vn.com.anhemsoftware.license_app.service.TaskService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

        @Mock
        private S3Service s3Service;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ─────────────────────────── GET /api/tasks ───────────────────────────────

    @Test
    void getAllTasks_returnsTasks() throws Exception {
        Task task = Task.builder().id(1L).title("Write tests").priority(Priority.HIGH).favorite(true).build();
        when(taskService.getAllTasks()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Write tests"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].isFavorite").value(true));
    }

    @Test
    void getAllTasks_returnsNoContentWhenListIsEmpty() throws Exception {
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllTasks_returnsNoContentWhenServiceReturnsNull() throws Exception {
        when(taskService.getAllTasks()).thenReturn(null);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isNoContent());
    }

    // ─────────────────────────── GET /api/tasks/{id} ──────────────────────────

    @Test
    void getTaskById_returnsTask() throws Exception {
                Task task = Task.builder().id(1L).title("My Task").completed(false).favorite(false).build();
        when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("My Task"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.isFavorite").value(false));
    }

    @Test
    void getTaskById_returns404WhenNotFound() throws Exception {
        when(taskService.getTaskById(99L))
                .thenThrow(new TaskNotFoundException("Không tìm thấy Task với id: 99"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTaskById_returns400WhenIdIsZero() throws Exception {
        mockMvc.perform(get("/api/tasks/0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void getTaskById_returns400WhenIdIsNegative() throws Exception {
        mockMvc.perform(get("/api/tasks/-5"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    // ─────────────────────────── POST /api/tasks ──────────────────────────────

    @Test
    void createTask_returnsCreatedTask() throws Exception {
        Task input = Task.builder().title("New Task").description("Some description").priority(Priority.MEDIUM).build();
        Task saved = Task.builder().id(2L).title("New Task").description("Some description").priority(Priority.MEDIUM).build();
        when(taskService.createTask(any(Task.class))).thenReturn(saved);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("New Task"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

        verify(taskService).createTask(any(Task.class));
    }

    @Test
    void createTask_returns400WhenTitleIsBlank() throws Exception {
        Task input = Task.builder().title("").description("desc").build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void createTask_returns400WhenTitleIsMissing() throws Exception {
        // title field omitted → @NotBlank triggers
        String body = "{\"description\":\"no title here\"}";

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void createTask_returns400WhenTitleExceedsMaxLength() throws Exception {
        String longTitle = "A".repeat(256);
        Task input = Task.builder().title(longTitle).build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    // ─────────────────────────── PUT /api/tasks/{id} ──────────────────────────

    @Test
    void updateTask_returnsUpdatedTask() throws Exception {
        Task input = Task.builder().title("Updated Title").description("Updated Desc").completed(true).build();
        Task result = Task.builder().id(1L).title("Updated Title").description("Updated Desc").completed(true).build();
        when(taskService.updateTask(eq(1L), any(Task.class))).thenReturn(result);

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void updateTask_returns404WhenNotFound() throws Exception {
        Task input = Task.builder().title("Updated Title").build();
        when(taskService.updateTask(eq(99L), any(Task.class)))
                .thenThrow(new TaskNotFoundException("Không tìm thấy Task với id: 99"));

        mockMvc.perform(put("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_returns400WhenIdIsZero() throws Exception {
        Task input = Task.builder().title("Some Title").build();

        mockMvc.perform(put("/api/tasks/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void updateTask_returns400WhenIdIsNegative() throws Exception {
        Task input = Task.builder().title("Some Title").build();

        mockMvc.perform(put("/api/tasks/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void updateTask_returns400WhenTitleIsEmptyString() throws Exception {
        // title present but blank → controller throws IllegalArgumentException
        Task input = Task.builder().title("").build();

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

        // ─────────────────────── GET /api/tasks/favorites ───────────────────────

        @Test
        void getFavoriteTasks_returnsTasks() throws Exception {
                Task task = Task.builder().id(1L).title("Fav task").favorite(true).build();
                when(taskService.getFavoriteTasks()).thenReturn(List.of(task));

                mockMvc.perform(get("/api/tasks/favorites"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                        .andExpect(jsonPath("$[0].isFavorite").value(true));
        }

        @Test
        void getFavoriteTasks_returnsNoContentWhenEmpty() throws Exception {
                when(taskService.getFavoriteTasks()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/api/tasks/favorites"))
                                .andExpect(status().isNoContent());
        }

        // ─────────────────────── PATCH /api/tasks/{id}/favorite ──────────────────

        @Test
        void setTaskFavorite_withRequestParam_updatesFavorite() throws Exception {
                Task updated = Task.builder().id(1L).title("Task").favorite(true).build();
                when(taskService.setFavorite(1L, true)).thenReturn(updated);

                mockMvc.perform(patch("/api/tasks/1/favorite")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"isFavorite\":true}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                        .andExpect(jsonPath("$.isFavorite").value(true));

                verify(taskService).setFavorite(1L, true);
        }

        @Test
            void setTaskFavorite_returns400WhenBodyMissingIsFavorite() throws Exception {
                mockMvc.perform(patch("/api/tasks/1/favorite")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());

                verifyNoInteractions(taskService);
        }

        @Test
        void setTaskFavorite_returns400WhenIdIsInvalid() throws Exception {
                mockMvc.perform(patch("/api/tasks/0/favorite")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"isFavorite\":true}"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(taskService);
        }

    // ─────────────────────────── DELETE /api/tasks/{id} ───────────────────────

    @Test
    void deleteTask_returnsNoContent() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTask_returns404WhenNotFound() throws Exception {
        doThrow(new TaskNotFoundException("Không tìm thấy Task với id: 99"))
                .when(taskService).deleteTask(99L);

        mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_returns400WhenIdIsZero() throws Exception {
        mockMvc.perform(delete("/api/tasks/0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void deleteTask_returns400WhenIdIsNegative() throws Exception {
        mockMvc.perform(delete("/api/tasks/-3"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

        // ─────────────────────── POST /api/tasks/{id}/image ──────────────────────

        @Test
        void uploadTaskImage_returnsImageUrlAndUpdatesTask() throws Exception {
                Task existingTask = Task.builder().id(1L).title("Task with old image").imageUrl("https://s3/old.png").build();
                when(taskService.getTaskById(1L)).thenReturn(existingTask);
                when(s3Service.uploadTaskImage(any(), eq(1L))).thenReturn("https://s3/new.png");

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "task.png",
                                MediaType.IMAGE_PNG_VALUE,
                                "fake-image-content".getBytes()
                );

                mockMvc.perform(multipart("/api/tasks/1/image").file(file))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.imageUrl").value("https://s3/new.png"));

                verify(taskService).getTaskById(1L);
                verify(s3Service).deleteFile("https://s3/old.png");
                verify(s3Service).uploadTaskImage(any(), eq(1L));
                verify(taskService).updateTask(eq(1L), any(Task.class));
        }

        // ───────────────────── DELETE /api/tasks/{id}/image ──────────────────────

        @Test
        void deleteTaskImage_returnsNoContentAndDeletesImage() throws Exception {
                Task existingTask = Task.builder().id(1L).title("Task with image").imageUrl("https://s3/image.png").build();
                when(taskService.getTaskById(1L)).thenReturn(existingTask);

                mockMvc.perform(delete("/api/tasks/1/image"))
                                .andExpect(status().isNoContent());

                verify(taskService).getTaskById(1L);
                verify(s3Service).deleteFile("https://s3/image.png");
                verify(taskService).updateTask(eq(1L), any(Task.class));
        }
}
