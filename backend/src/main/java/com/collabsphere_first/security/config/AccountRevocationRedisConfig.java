package com.collabsphere_first.security.config;

import com.collabsphere_first.security.event.AccountRevocationListener;
import com.collabsphere_first.security.service.AccountRevocationCoordinator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class AccountRevocationRedisConfig {

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisMessageListenerContainer accountRevocationListenerContainer(
            RedisConnectionFactory connectionFactory,
            AccountRevocationListener listener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic(AccountRevocationCoordinator.REVOCATION_CHANNEL));
        return container;
    }
}
