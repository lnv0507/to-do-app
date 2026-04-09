package vn.com.anhemsoftware.license_app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "user_devices")
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "device_id", nullable = false, unique = true)
    String deviceId;

    @Column(name = "is_trusted")
    Boolean isTrusted;

    @Column(name = "last_active")
    Long lastActive;

    @Column(name = "last_ip")
    String lastIp;

    @Column(name = "created_at")
    Long createdAt;

    /**
     * JTI of this login session's refresh token — used to revoke when user clicks
     * "This wasn't me"
     */
    @Column(name = "session_jti")
    String sessionJti;

    /** Last User-Agent — updated silently when device token matches */
    @Column(name = "user_agent", length = 512)
    String userAgent;
}
