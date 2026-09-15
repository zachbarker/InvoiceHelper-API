package com.invoiceflow;

import com.invoiceflow.common.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class InvoiceflowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceflowApiApplication.class, args);
    }
}
