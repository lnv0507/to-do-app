package vn.com.anhemsoftware.license_app.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import vn.com.anhemsoftware.license_app.entity.Priority;
import vn.com.anhemsoftware.license_app.entity.Task;
import vn.com.anhemsoftware.license_app.exception.TaskNotFoundException;
import vn.com.anhemsoftware.license_app.repository.TaskRepository;
import vn.com.anhemsoftware.license_app.service.TaskService;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
    }

    @Override
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public List<Task> createMultipleTasks(List<Task> tasks) {
        return taskRepository.saveAll(tasks);
    }

    @Override
    public Task updateTask(Long id, Task taskDetails) {
        Task existingTask = getTaskById(id); // Reuse getTaskById method to check existence

        existingTask.setTitle(taskDetails.getTitle());
        existingTask.setDescription(taskDetails.getDescription());
        existingTask.setCompleted(taskDetails.isCompleted());
        existingTask.setPriority(taskDetails.getPriority());
        existingTask.setSpecification(taskDetails.getSpecification());
        existingTask.setDueDate(taskDetails.getDueDate());
        existingTask.setImageUrl(taskDetails.getImageUrl());

        return taskRepository.save(existingTask);
    }

    @Override
    public void deleteTask(Long id) {
        Task existingTask = getTaskById(id);
        taskRepository.delete(existingTask);
    }

    @Override
    public void deleteMultipleTasks(List<Long> ids) {
        log.info("🚀 AI Tool called: deleteMultipleTasks(ids={})", ids);
        taskRepository.deleteAllById(ids);
    }

    @Override
    // Removed @Tool to prevent AI confusion.
    public List<Task> getDueTasks() {
        return taskRepository.findByDueDateLessThanEqualAndCompletedFalse(LocalDate.now());
    }

    @Override
    // Removed @Tool to prevent AI confusion.
    public List<Task> getFavoriteTasks() {
        return taskRepository.findByFavoriteTrue();
    }

    @Override
    public Task setFavorite(Long id, boolean favorite) {
        Task existingTask = getTaskById(id);
        existingTask.setFavorite(favorite);
        return taskRepository.save(existingTask);
    }

    @Override
    public List<Task> setMultipleFavorite(List<Long> ids, boolean favorite) {
        log.info("🚀 AI Tool called: setMultipleFavorite(ids={}, favorite={})", ids, favorite);
        List<Task> tasks = ids.stream().map(this::getTaskById).toList();
        tasks.forEach(task -> task.setFavorite(favorite));
        List<Task> savedTasks = taskRepository.saveAll(tasks);
        log.info("✅ Successfully updated multiple tasks {} to favorite={}", ids, favorite);
        return savedTasks;
    }

    @Override
    public Task setCompleted(Long id, boolean completed) {
        log.info("🚀 AI Tool called: setCompleted(id={}, completed={})", id, completed);
        Task existingTask = getTaskById(id);
        existingTask.setCompleted(completed);
        Task savedTask = taskRepository.save(existingTask);

        log.info("✅ Successfully updated task ID {} to completed={}", savedTask.getId(), savedTask.isCompleted());
        return savedTask;
    }

    @Override
    public List<Task> setMultipleCompleted(List<Long> ids, boolean completed) {
        List<Task> tasks = ids.stream().map(this::getTaskById).toList();
        tasks.forEach(task -> task.setCompleted(completed));
        List<Task> savedTasks = taskRepository.saveAll(tasks);
        return savedTasks;
    }

    @Override
    // Removed @Tool to prevent AI confusion.
    public List<Task> getUncompletedTasks() {
        return taskRepository.findByCompletedFalse();
    }

    @Override
    public List<Task> searchTasks(
            String keyword,
            Boolean completed,
            Boolean favorite,
            String priority,
            LocalDate dueDateBefore,
            LocalDate dueDateAfter) {

        log.info(
                "🚀 AI Tool called: searchTasks(keyword={}, completed={}, favorite={}, priority={}, dueDateBefore={}, dueDateAfter={})",
                keyword, completed, favorite, priority, dueDateBefore, dueDateAfter);

        Specification<Task> spec = Specification.where(null);

        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")));
        }
        if (completed != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("completed"), completed));
        }
        if (favorite != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("favorite"), favorite));
        }
        if (priority != null && !priority.trim().isEmpty()) {
            try {
                Priority p = Priority.valueOf(priority.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), p));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid priority value from AI: {}", priority);
            }
        }
        if (dueDateBefore != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), dueDateBefore));
        }
        if (dueDateAfter != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), dueDateAfter));
        }

        return taskRepository.findAll(spec);
    }
}
