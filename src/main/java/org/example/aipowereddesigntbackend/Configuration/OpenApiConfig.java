package org.example.aipowereddesigntbackend.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi(){
        return new OpenAPI()
                .info(new Info()
                        .title("ArchPilot")
                        .version("1.0")
                        .description("""
                                Need to have the system design  and architecture as a non-technical person.ArchPilot is your solution
                                
                                [Continue with Google](http://localhost:8082/oauth2/authorization/google)
                                [Continue with Github](http://localhost:8082/oauth2/authorization/github)
                                
                                """)
                        .contact(new Contact()
                                .url("https://archPilot.vercel.app")
                                .name("ArchPilot."))
                        .license(new License()
                                .name("Apache 2.0"))
                )
                .addServersItem(
                        new Server()
                                .url("http://localhost:8082/")
                                .description("Local ArchPilot Server")
                )
                .addServersItem(
                        new Server()
                                .url("https://ai-powered-system-design-architecture-assistant-production.up.railway.app/")
                                .description("Deployed ArchPilot Server")
                )
                ;

    }
}
