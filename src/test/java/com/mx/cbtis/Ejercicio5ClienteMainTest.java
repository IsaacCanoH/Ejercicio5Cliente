package com.mx.cbtis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(
        classes = Ejercicio5ClienteApplication.class,
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.application.name=cliente-main-test")
class Ejercicio5ClienteMainTest {

    @Autowired
    private ConfigurableApplicationContext contexto;

    @Test
    void mainIniciaElContextoConLaConfiguracionDePrueba() {
        assertTrue(contexto.isActive());
        assertNotNull(contexto.getBean(Ejercicio5ClienteApplication.class));
        assertEquals("cliente-main-test", contexto.getEnvironment().getProperty("spring.application.name"));
    }
}

