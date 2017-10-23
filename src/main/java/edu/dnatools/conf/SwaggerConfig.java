package edu.dnatools.conf;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.models.dto.ApiInfo;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by luke on 6/4/16.
 */
@EnableSwagger
@Configuration
@ComponentScan("edu.dnatools.controller")
public class SwaggerConfig {

    public static final String DEFAULT_INCLUDE_PATTERNS = "/.*";

    @Autowired
    private SpringSwaggerConfig springSwaggerConfig;

    @Bean
    public SwaggerSpringMvcPlugin customSwaggerSpringMvcPlugin() {
        return new SwaggerSpringMvcPlugin(springSwaggerConfig)
                .apiInfo(apiInfo())
                .includePatterns(DEFAULT_INCLUDE_PATTERNS)
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "DNA Server Web API",
                "Testing",
                "https://www.gnu.org/licenses/gpl-3.0.en.html",
                "luke.czapla@frisch.org",
                "2017",
                "https://www.gnu.org/licenses/gpl-3.0.en.html"
        );
    }
}
