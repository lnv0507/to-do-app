package vn.com.anhemsoftware.license_app.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.com.anhemsoftware.license_app.entity.Task;
import vn.com.anhemsoftware.license_app.service.TaskService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskNotificationScheduler {

    private final TaskService taskService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Check for due tasks every 60 seconds (1 minute)
     * Cron expression can be adjusted as needed
     */
    @Scheduled(fixedRate = 60000) // 60000ms = 60s = 1 minute
    public void checkAndNotifyDueTasks() {
        try {
            List<Task> dueTasks = taskService.getDueTasks();

            if (!dueTasks.isEmpty()) {
                // Send task list to all clients subscribed to /topic/tasks/due
                messagingTemplate.convertAndSend("/topic/tasks/due", dueTasks);
            }
        } catch (Exception e) {
            log.error("Error checking for due tasks", e);
        }
    }
}
