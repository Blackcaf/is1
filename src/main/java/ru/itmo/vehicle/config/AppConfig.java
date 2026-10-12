package ru.itmo.vehicle.config;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {"ru.itmo.vehicle.repo", "ru.itmo.vehicle.service"})
public class AppConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceUnitName("vehiclePU");
        emf.setPersistenceXmlLocation("classpath:META-INF/persistence.xml");
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setJpaDialect(new EclipseLinkJpaDialect());
        emf.setJpaPropertyMap(Map.of(
                "javax.persistence.jdbc.user", resolve("DB_USER", System.getProperty("user.name", "")),
                "javax.persistence.jdbc.password", resolve("DB_PASSWORD", "")
        ));
        return emf;
    }

    private static String resolve(String envKey, String def) {
        String v = System.getenv(envKey);
        if (v == null) v = System.getProperty(envKey);
        return v != null ? v : def;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(emf);
        return tm;
    }

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
