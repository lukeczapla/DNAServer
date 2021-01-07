package edu.dnatools;

/**
 * Created by luke on 6/9/17.
 */
import edu.dnatools.conf.SwaggerConfig;
import edu.dnatools.conf.WebSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.io.IOException;

/**
 * Created by luke on 5/31/16.
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("edu.dnatools")
@Import({WebSecurityConfig.class, SwaggerConfig.class})
public class Startup {

    public static void main(String[] args) {
        SpringApplication.run(Startup.class, args);
    }

}
