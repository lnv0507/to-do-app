package vn.com.anhemsoftware.license_app.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import vn.com.anhemsoftware.license_app.entity.User;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-05T10:55:59+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toUser(SignInRequest signinRequest) {
        if ( signinRequest == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.email( signinRequest.email() );
        user.password( signinRequest.password() );

        return user.build();
    }

    @Override
    public User toUser(SignUpRequest signUpRequest) {
        if ( signUpRequest == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.email( signUpRequest.email() );
        user.password( signUpRequest.password() );
        user.fullName( signUpRequest.fullName() );
        user.phone( signUpRequest.phone() );
        user.address( signUpRequest.address() );

        return user.build();
    }
}
