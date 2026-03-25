package vn.com.anhemsoftware.license_app.service.impl;

import lombok.RequiredArgsConstructor;
import vn.com.anhemsoftware.license_app.entity.Task;
import vn.com.anhemsoftware.license_app.exception.TaskNotFoundException;
import vn.com.anhemsoftware.license_app.repository.TaskRepository;
import vn.com.anhemsoftware.license_app.service.TaskService;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
                .orElseThrow(() -> new TaskNotFoundException("Không tìm thấy Task với id: " + id));
    }

    @Override
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public Task updateTask(Long id, Task taskDetails) {
        Task existingTask = getTaskById(id); // Tái sử dụng hàm getTaskById để check tồn tại

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
    public List<Task> getDueTasks() {
        return taskRepository.findByDueDateLessThanEqualAndCompletedFalse(LocalDate.now());
    }

    @Override
    public List<Task> getFavoriteTasks() {
        return taskRepository.findByFavoriteTrue();
    }

    @Override
    public Task setFavorite(Long id, boolean favorite) {
        Task existingTask = getTaskById(id);
        existingTask.setFavorite(favorite);
        return taskRepository.save(existingTask);
    }
}
