package com.scarlxrd.payment_service.config.swagger;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${app.gateway-url:http://localhost:8080}") String gatewayUrl) {
        return new OpenAPI()
                .info(buildInfo())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(buildComponents())
                .externalDocs(new ExternalDocumentation()
                        .description("Repositório do projeto")
                        .url("https://github.com/Sc4rlxrd/payment-service"));
    }

    private Info buildInfo() {
        return new Info()
                .title("Payment-Service API")
                .description("""
                        Serviço responsável pelo processamento de pagamentos do BookCommerce.
                        
                        **Este serviço não expõe endpoints HTTP diretos.**
                        A comunicação é feita exclusivamente via RabbitMQ:
                        
                        **Consome:**
                        - `payment.process.queue` → processa o pagamento
                        
                        **Publica:**
                        - `payment.result.success` → pagamento aprovado (~71%)
                        - `payment.result.failed` → pagamento recusado (~20%)
                        - `payment.process.queue.dlq` → serviço indisponível (~9%)
                        """)
                .version("v1")
                .contact(new Contact()
                        .name("Scarlxrd")
                        .url("https://github.com/Sc4rlxrd")
                        .email("contato@exemplo.com"));
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
    }
}