package ru.itmo.vehicle.config;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScan("ru.itmo.vehicle")
public class AppConfig {
    private final String databaseUrl;
    private final String databaseUser;
    private final String databasePassword;
    private final String ddlGeneration;

    public AppConfig(@Value("${app.database.url}") String databaseUrl,
                     @Value("${app.database.user}") String databaseUser,
                     @Value("${app.database.password}") String databasePassword,
                     @Value("${app.database.ddl-generation:create-or-extend-tables}") String ddlGeneration) {
        this.databaseUrl = databaseUrl;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
        this.ddlGeneration = ddlGeneration;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceUnitName("vehiclePU");
        emf.setPersistenceXmlLocation("classpath:META-INF/persistence.xml");
        emf.setPersistenceProviderClass(PersistenceProvider.class);
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setJpaDialect(new EclipseLinkJpaDialect());
        Properties properties = new Properties();
        properties.put("javax.persistence.jdbc.url", databaseUrl);
        properties.put("javax.persistence.jdbc.user", databaseUser);
        properties.put("javax.persistence.jdbc.password", databasePassword);
        properties.put("eclipselink.ddl-generation", ddlGeneration);
        emf.setJpaProperties(properties);
        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
