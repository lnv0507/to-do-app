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
     * Kiểm tra task đến hạn mỗi 60 giây (1 phút)
     * Có thể điều chỉnh cron expression theo nhu cầu
     */
    @Scheduled(fixedRate = 60000) // 60000ms = 60s = 1 phút
    public void checkAndNotifyDueTasks() {
        try {
            List<Task> dueTasks = taskService.getDueTasks();

            if (!dueTasks.isEmpty()) {
                // Gửi danh sách task đến tất cả client đang subscribe /topic/tasks/due
                messagingTemplate.convertAndSend("/topic/tasks/due", dueTasks);
            }
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra task đến hạn", e);
        }
    }
}
