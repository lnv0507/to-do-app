package vn.com.anhemsoftware.license_app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.com.anhemsoftware.license_app.entity.User;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "address", ignore = true)
    User toUser(SignInRequest signinRequest);
    
    @Mapping(target = "id", ignore = true)
    User toUser(SignUpRequest signUpRequest);
}
