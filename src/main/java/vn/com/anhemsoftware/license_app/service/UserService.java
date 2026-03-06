package vn.com.anhemsoftware.license_app.service;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService
{
    UserDetails loadUserByUsername(String email);
}
