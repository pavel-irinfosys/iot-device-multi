package com.irinfosys.websocket.multi.config;

import com.irinfosys.websocket.multi.service.RedisDeviceDataSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisDeviceDataConfig {

  @Bean
  public ChannelTopic deviceDataTopic() {
    return new ChannelTopic("device-data-events");
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      RedisDeviceDataSubscriber subscriber,
      ChannelTopic deviceDataTopic
  ) {
    RedisMessageListenerContainer container =
        new RedisMessageListenerContainer();

    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(subscriber, deviceDataTopic);

    return container;
  }
}
