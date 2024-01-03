package at.rtr.rmbt.map;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories(basePackages = {"at.rtr.rmbt.map.repository"})
@EntityScan(basePackages = {"at.rtr.rmbt.map.model"})
@PropertySource({"classpath:git.properties"})
//@EnableConfigurationProperties(ApplicationProperties.class)
@ConfigurationPropertiesScan
public class MapServerConfiguration extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MapServerConfiguration.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MapServerConfiguration.class);
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:SystemMessages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }


}
