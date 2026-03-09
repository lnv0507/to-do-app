package vn.com.anhemsoftware.license_app.controller;

import lombok.RequiredArgsConstructor;
import vn.com.anhemsoftware.license_app.entity.Task;
import vn.com.anhemsoftware.license_app.service.TaskService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

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
            throw new IllegalArgumentException("Task ID không được null hoặc <= 0");
        }
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task không được null");
        }
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề Task không được trống");
        }
        Task createdTask = taskService.createTask(task);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody Task taskDetails) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Task ID không được null hoặc <= 0");
        }
        if (taskDetails == null) {
            throw new IllegalArgumentException("Task details không được null");
        }
        if (taskDetails.getTitle() != null && taskDetails.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề Task không được trống");
        }
        return ResponseEntity.ok(taskService.updateTask(id, taskDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Task ID không được null hoặc <= 0");
        }
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
