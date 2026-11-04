package com.collabsphere_first.system.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemHealthControllerTest {

    @Test
    void returnsServiceUnavailableWhenDependenciesCannotBeVerified() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection database = mock(Connection.class);
        RedisConnectionFactory redis = mock(RedisConnectionFactory.class);
        RedisConnection cache = mock(RedisConnection.class);
        when(dataSource.getConnection()).thenReturn(database);
        when(database.isValid(2)).thenReturn(false);
        when(redis.getConnection()).thenReturn(cache);

        var response = new SystemHealthController(dataSource, redis).getHealth();

        assertEquals(503, response.getStatusCode().value());
        assertEquals("degraded", response.getBody().get("status"));
        assertEquals("unavailable", response.getBody().get("database"));
    }
}
