package vn.com.anhemsoftware.license_app.controller;

import lombok.RequiredArgsConstructor;
import vn.com.anhemsoftware.license_app.entity.Task;
import vn.com.anhemsoftware.license_app.payload.task.request.FavoriteTaskRequest;
import vn.com.anhemsoftware.license_app.service.S3Service;
import vn.com.anhemsoftware.license_app.service.TaskService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final S3Service s3Service;

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        if (tasks == null || tasks.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Task ID must not be null or <= 0");
        }
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task must not be null");
        }
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        Task createdTask = taskService.createTask(task);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody Task taskDetails) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Task ID must not be null or <= 0");
        }
        if (taskDetails == null) {
            throw new IllegalArgumentException("Task details must not be null");
        }
        if (taskDetails.getTitle() != null && taskDetails.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        return ResponseEntity.ok(taskService.updateTask(id, taskDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Task ID must not be null or <= 0");
        }
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/due")
    public ResponseEntity<List<Task>> getDueTasks() {
        List<Task> dueTasks = taskService.getDueTasks();
        if (dueTasks.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dueTasks);
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<Task>> getFavoriteTasks() {
        List<Task> favoriteTasks = taskService.getFavoriteTasks();
        if (favoriteTasks == null || favoriteTasks.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(favoriteTasks);
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<Task> setTaskFavorite(
            @PathVariable Long id,
            @Valid @RequestBody FavoriteTaskRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Task ID must not be null or <= 0");
        }

        Task updatedTask = taskService.setFavorite(id, request.getIsFavorite());

        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Upload image for Task to AWS S3.
     * POST /api/tasks/{id}/image
     * Form-data key: "file"
     */
    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadTaskImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Task ID must not be null or <= 0");
        }
        // Verify task exists before uploading
        Task task = taskService.getTaskById(id);

        // Delete old image if present
        if (task.getImageUrl() != null) {
            s3Service.deleteFile(task.getImageUrl());
        }

        String imageUrl = s3Service.uploadTaskImage(file, id);
        task.setImageUrl(imageUrl);
        taskService.updateTask(id, task);

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    /**
     * Delete task image from AWS S3.
     * DELETE /api/tasks/{id}/image
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteTaskImage(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Task ID must not be null or <= 0");
        }
        Task task = taskService.getTaskById(id);
        if (task.getImageUrl() != null) {
            s3Service.deleteFile(task.getImageUrl());
            task.setImageUrl(null);
            taskService.updateTask(id, task);
        }
        return ResponseEntity.noContent().build();
    }
}
