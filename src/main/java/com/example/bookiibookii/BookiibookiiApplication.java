package com.example.bookiibookii;

import com.example.bookiibookii.global.aws.AwsS3Properties;
import com.example.bookiibookii.global.config.ShareWebProperties;
import com.example.bookiibookii.global.notification.DiscordWebhookProperties;
import com.example.bookiibookii.domain.push.config.FirebasePushProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({
		AwsS3Properties.class,
		DiscordWebhookProperties.class,
		ShareWebProperties.class,
		FirebasePushProperties.class
})
public class BookiibookiiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookiibookiiApplication.class, args);
	}

}
