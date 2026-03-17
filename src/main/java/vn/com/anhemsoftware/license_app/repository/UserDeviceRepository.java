package vn.com.anhemsoftware.license_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.com.anhemsoftware.license_app.entity.UserDevice;

import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<UserDevice> findByDeviceId(String deviceId);

    /** Dùng để kiểm tra IP của user có quen không khi device cookie vắng mặt */
    java.util.List<UserDevice> findAllByUserId(Long userId);
}
