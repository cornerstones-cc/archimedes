package cc.cornerstones.zero.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author bbottong
 */
@Configuration
public class SwaggerConfigurator {
    @Value("${private.swagger.title}")
    private String title;

    @Value("${private.swagger.description}")
    private String description;

    @Value("${private.swagger.contact.name}")
    private String contactName;

    @Value("${private.swagger.contact.url}")
    private String contactUrl;

    @Value("${private.swagger.contact.email}")
    private String contactEmail;

    @Value("${private.swagger.version}")
    private String version;

    @Value("${private.swagger.license.name}")
    private String licenseName;

    @Value("${private.swagger.license.url}")
    private String licenseUrl;

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(info());
    }

    public Info info() {
        return new Info()
                .title(this.title)
                .description(this.description)
                .contact(new Contact().name(this.contactName).url(this.contactUrl).email(this.contactEmail))
                .version(this.version)
                .license(new License().name(this.licenseName).url(this.licenseUrl));
    }
}