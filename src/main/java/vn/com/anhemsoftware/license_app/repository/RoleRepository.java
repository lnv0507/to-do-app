package vn.com.anhemsoftware.license_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.com.anhemsoftware.license_app.entity.Role;
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

}
