package com.legally.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Option A: Spring Boot connects directly to the PostgreSQL database on the
 * Cloud SQL instance provisioned by Firebase SQL Connect (no SQL Connect GraphQL SDK).
 */
@Configuration
@ConditionalOnProperty(name = "legally.database.mode", havingValue = "cloud-sql")
@EnableConfigurationProperties(DataSourceProperties.class)
public class CloudSqlDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudSqlDataSourceConfig.class);

    private static final String SOCKET_FACTORY = "com.google.cloud.sql.postgres.SocketFactory";

    private final LegallyProperties legallyProperties;
    private final DataSourceProperties dataSourceProperties;

    public CloudSqlDataSourceConfig(LegallyProperties legallyProperties, DataSourceProperties dataSourceProperties) {
        this.legallyProperties = legallyProperties;
        this.dataSourceProperties = dataSourceProperties;
    }

    @Bean
    @Primary
    public DataSource cloudSqlDataSource() {
        LegallyProperties.Database db = legallyProperties.getDatabase();
        String instance = db.getCloudSqlInstanceConnectionName();
        if (!StringUtils.hasText(instance)) {
            throw new IllegalStateException(
                    "legally.database.mode=cloud-sql requires CLOUD_SQL_INSTANCE_CONNECTION_NAME "
                            + "(format: project-id:region:instance-id)");
        }

        String databaseName = db.getName();
        String jdbcUrl = "jdbc:postgresql:///%s?cloudSqlInstance=%s&socketFactory=%s"
                .formatted(databaseName, instance, SOCKET_FACTORY);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dataSourceProperties.getUsername());
        config.setPassword(dataSourceProperties.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setConnectionTestQuery("SELECT 1");
        config.setInitializationFailTimeout(30_000);

        log.info("PostgreSQL: Cloud SQL socket mode — instance={}, database={}", instance, databaseName);
        HikariDataSource dataSource = new HikariDataSource(config);
        try (var conn = dataSource.getConnection()) {
            log.info("Cloud SQL connection verified");
        } catch (Exception e) {
            dataSource.close();
            throw new IllegalStateException(
                    "Could not connect to Cloud SQL. For IntelliJ/Postman use profile 'local' + Docker Postgres. "
                            + "For Cloud SQL locally run: gcloud auth application-default login", e);
        }
        return dataSource;
    }
}
