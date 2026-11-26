package vn.hoadon.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    @Bean
    public HttpClient aiHttpClient(AiProperties properties) {
        int timeout = Math.max(1, properties.getGemini().getConnectTimeoutSeconds());
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Bean
    public Clock aiClock() {
        return Clock.systemUTC();
    }
}
