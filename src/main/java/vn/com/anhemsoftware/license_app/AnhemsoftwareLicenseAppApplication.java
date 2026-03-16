package vn.com.anhemsoftware.license_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

//@Profile("dev")
@SpringBootApplication
@EnableScheduling
public class AnhemsoftwareLicenseAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnhemsoftwareLicenseAppApplication.class, args);
	}

}
