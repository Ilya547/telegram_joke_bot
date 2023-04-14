package io.ilyaTGbot.demoSpringBot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Data
@PropertySource("classpath:application.properties")
public class BotConfig {

    @Value("${telegram.botName}")
    String botUserName;

    @Value("${telegram.botToken}")
    String token;

    @Value("${telegram.botOwner}")
    Long ownerId;
}
