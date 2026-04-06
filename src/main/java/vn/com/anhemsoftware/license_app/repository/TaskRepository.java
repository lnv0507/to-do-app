package vn.com.anhemsoftware.license_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.com.anhemsoftware.license_app.entity.Task;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByDueDateLessThanEqualAndCompletedFalse(LocalDate date);

    List<Task> findByFavoriteTrue();

    List<Task> findByCompletedFalse();

}
