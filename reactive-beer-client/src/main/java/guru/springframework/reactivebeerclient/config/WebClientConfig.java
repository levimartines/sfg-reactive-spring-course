package guru.springframework.reactivebeerclient.config;

import reactor.netty.http.client.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl(WebClientProperties.BASE_URL)
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().wiretap(true)))
            .build();
    }
}
