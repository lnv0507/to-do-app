package vn.com.anhemsoftware.license_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.com.anhemsoftware.license_app.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

}
