package com.opower.persistence.jpile.config;

import com.opower.persistence.jpile.reflection.CacheablePersistenceAnnotationInspector;
import com.opower.persistence.jpile.reflection.PersistenceAnnotationInspector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @author amir.raminfar
 */
@Configuration
@ImportResource("classpath:spring-context.xml")
public class JPileApplicationConfig {

    @Bean
    public PersistenceAnnotationInspector createPersistenceAnnotationInspector() {
        return new CacheablePersistenceAnnotationInspector();
    }
}
