package com.azul.eclipseocx2026.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@DataSourceDefinition(
    name = "java:app/ocx-db",
    className = "org.postgresql.ds.PGSimpleDataSource",
    serverName = "postgres",
    portNumber = 5432,
    databaseName = "ocx",
    user = "ocx",
    password = "ocx"
)
public class DataSourceConfig {
}
