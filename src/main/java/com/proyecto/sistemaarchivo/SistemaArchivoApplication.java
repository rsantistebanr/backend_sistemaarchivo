package com.proyecto.sistemaarchivo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
// Forzamos a Spring a mirar en todas tus carpetas
public class SistemaArchivoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SistemaArchivoApplication.class, args);
    }
}
