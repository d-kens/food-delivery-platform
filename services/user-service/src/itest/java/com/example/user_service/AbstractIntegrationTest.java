package com.example.user_service;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NON_TEST)
@TestPropertySource(properties = {
        "outbox.relay.enabled=false",
        "spring.kafka.admin.auto-create=false",
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test",
        "spring.security.oauth2.client.registration.google.redirect-uri=http://localhost/login/oauth2/code/google"
})
public abstract class AbstractIntegrationTest {

    // Started once and shared by every subclass; Testcontainers removes it when the JVM exits.
    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    static {
        POSTGRES.start();
    }
}
