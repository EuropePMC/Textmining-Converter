package uk.ac.ebi.literature.textminingapi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootApplication(exclude = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@PropertySources({ @PropertySource("classpath:application-utility.properties"),
	@PropertySource("classpath:application-utility-${spring.profiles.active}.properties"),
	})
public class TextminingApiConverterApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(TextminingApiConverterApplication.class);
		app.run(args);
	}

	public void run(String... args) throws Exception {

	}

}
