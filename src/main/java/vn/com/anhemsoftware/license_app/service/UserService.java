package vn.com.anhemsoftware.license_app.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserService
{
    UserDetails loadUserByUsername(String email);
}
