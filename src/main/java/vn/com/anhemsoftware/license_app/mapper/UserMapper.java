package vn.com.anhemsoftware.license_app.mapper;

import org.mapstruct.Mapper;
import vn.com.anhemsoftware.license_app.entity.User;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    User toUser(SignInRequest signinRequest);
    User toUser(SignUpRequest signUpRequest);
}
