package vn.com.anhemsoftware.license_app.ai.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import vn.com.anhemsoftware.license_app.ai.dto.*;
import vn.com.anhemsoftware.license_app.entity.Task;
import vn.com.anhemsoftware.license_app.service.TaskService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAiTools {

    private final TaskService taskService;

    @Tool(description = "Get detailed information of a specific task by its numeric ID. Call this when the user mentions a specific task ID.")
    public Task getTaskById(Long id) {
        log.info("🚀 AI Tool called: getTaskById(id={})", id);
        return taskService.getTaskById(id);
    }

    @Tool(description = "Create a new task. Required info is title. Attributes like description, priority, dueDate are optional. Call this when user wants to add or create a single task. WARNING: Modifies database. If uncertain, show plan and ask for confirmation first.")
    public Task createTask(CreateTaskRequest request) {
        log.info("🚀 AI Tool called: createTask(title={})", request.title());
        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .completed(request.completed())
                .priority(request.priority())
                .specification(request.specification())
                .dueDate(request.dueDate())
                .imageUrl(request.imageUrl())
                .build();
        return taskService.createTask(task);
    }

    @Tool(description = "Create MULTIPLE new tasks at once. Required info is a list of tasks. Call this when user wants to add or create multiple tasks. WARNING: Modifies database. Show a plan or the extracted list to the user and ask for confirmation BEFORE calling this tool.")
    public List<Task> createMultipleTasks(MultipleTaskCreateRequest request) {
        log.info("🚀 AI Tool called: createMultipleTasks(tasks={})", request.tasks().size());
        List<Task> taskEntities = request.tasks().stream().map(req -> Task.builder()
                .title(req.title())
                .description(req.description())
                .completed(req.completed())
                .priority(req.priority())
                .specification(req.specification())
                .dueDate(req.dueDate())
                .imageUrl(req.imageUrl())
                .build()).toList();
        return taskService.createMultipleTasks(taskEntities);
    }

    @Tool(description = "Update an existing task by its ID. Use this to change properties (title, completed status, priority). You MUST provide the ID and new task details.")
    public Task updateTask(UpdateTaskRequest request) {
        log.info("🚀 AI Tool called: updateTask(id={})", request.id());
        Task taskDetails = Task.builder()
                .title(request.title())
                .description(request.description())
                .completed(request.completed())
                .priority(request.priority())
                .specification(request.specification())
                .dueDate(request.dueDate())
                .imageUrl(request.imageUrl())
                .build();
        return taskService.updateTask(request.id(), taskDetails);
    }

    @Tool(description = "Delete or remove a task by its ID. Call this when user asks to delete, remove or clear a specific task. WARNING: Modifies database. If uncertain, show plan and ask for confirmation first.")
    public void deleteTask(Long id) {
        log.info("🚀 AI Tool called: deleteTask(id={})", id);
        taskService.deleteTask(id);
    }

    @Tool(description = "Delete or remove MULTIPLE tasks by a list of their IDs. Call this when user asks to delete, remove or clear multiple tasks at once. WARNING: Modifies database. ALWAYS search for tasks first, show them to the user as a plan, and ask for explicit confirmation BEFORE calling this tool.")
    public void deleteMultipleTasks(MultipleTaskDeleteRequest request) {
        log.info("🚀 AI Tool called: deleteMultipleTasks(ids={})", request.ids());
        taskService.deleteMultipleTasks(request.ids());
    }

    @Tool(description = "Toggle the favorite status of a single task by its ID. Pass 'true' to mark as favorite, 'false' to remove from favorites.")
    public Task setFavorite(SingleTaskFavoriteRequest request) {
        log.info("🚀 AI Tool called: setFavorite(id={}, favorite={})", request.id(), request.favorite());
        return taskService.setFavorite(request.id(), request.favorite());
    }

    @Tool(description = "Mark MULTIPLE tasks as favorite (true) or unfavorite (false) by a list of their IDs. Call this when the user wants to favorite/unfavorite multiple tasks at once.")
    public List<Task> setMultipleFavorite(MultipleTaskFavoriteRequest request) {
        log.info("🚀 AI Tool called: setMultipleFavorite(ids={}, favorite={})", request.ids(), request.favorite());
        return taskService.setMultipleFavorite(request.ids(), request.favorite());
    }

    @Tool(description = "Mark a task as completed (true) or incomplete (false) by its ID. Call this when user wants to finish, complete, check off, or reopen a task.")
    public Task setCompleted(SingleTaskCompletedRequest request) {
        log.info("🚀 AI Tool called: setCompleted(id={}, completed={})", request.id(), request.completed());
        return taskService.setCompleted(request.id(), request.completed());
    }

    @Tool(description = "Mark MULTIPLE tasks as completed (true) or incomplete (false) by a list of their IDs. Call this when the user wants to finish, complete, or check off multiple tasks at once (e.g. 'all uncompleted tasks', 'these tasks').")
    public List<Task> setMultipleCompleted(MultipleTaskCompletedRequest request) {
        log.info("🚀 AI Tool called: setMultipleCompleted(ids={}, completed={})", request.ids(), request.completed());
        return taskService.setMultipleCompleted(request.ids(), request.completed());
    }

    @Tool(description = "Search and filter tasks dynamically. Usage: ANY request asking to find, list, or match tasks out of the database. All parameters are optional, omit them if not specified.")
    public List<Task> searchTasks(SearchTaskRequest request) {
        log.info("🚀 AI Tool called: searchTasks(...)");
        return taskService.searchTasks(
                request.keyword(),
                request.completed(),
                request.favorite(),
                request.priority(),
                request.dueDateBefore(),
                request.dueDateAfter());
    }
}
