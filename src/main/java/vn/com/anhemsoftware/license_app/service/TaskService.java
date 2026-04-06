package vn.com.anhemsoftware.license_app.service;

import java.util.List;

import vn.com.anhemsoftware.license_app.entity.Task;

public interface TaskService {
    List<Task> getAllTasks();

    Task getTaskById(Long id);

    Task createTask(Task task);

    List<Task> createMultipleTasks(List<Task> tasks);

    Task updateTask(Long id, Task taskDetails);

    void deleteTask(Long id);

    void deleteMultipleTasks(List<Long> ids);

    List<Task> getDueTasks();

    List<Task> getFavoriteTasks();

    Task setFavorite(Long id, boolean favorite);

    List<Task> setMultipleFavorite(List<Long> ids, boolean favorite);

    Task setCompleted(Long id, boolean completed);

    List<Task> setMultipleCompleted(List<Long> ids, boolean completed);

    List<Task> getUncompletedTasks();

    List<Task> searchTasks(String keyword, Boolean completed, Boolean favorite, String priority,
            java.time.LocalDate dueDateBefore, java.time.LocalDate dueDateAfter);
}
