package panda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PandaApplication {

	public static void main(String[] args) {
		SpringApplication.run(PandaApplication.class, args);
	}

}
