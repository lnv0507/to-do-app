package vn.com.anhemsoftware.license_app.service;

import java.util.List;

import vn.com.anhemsoftware.license_app.entity.Task;

public interface TaskService {
    List<Task> getAllTasks();

    Task getTaskById(Long id);

    Task createTask(Task task);

Task updateTask(Long id, Task taskDetails);

    void deleteTask(Long id);

    List<Task> getDueTasks();
}
