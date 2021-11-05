package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.functional.BeerRouterConfig;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerStyleEnum;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientV2IT {

    public static final String BASE_URL = "http://localhost:8080";
    public static final String BEER_V2_PATH = "api/v2/beer";

    WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().wiretap(true)))
            .build();
    }

    @Nested
    class GetBeer {

        @Test
        void getBeerById() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            Mono<BeerDto> beerDtoMono = webClient.get().uri(BEER_V2_PATH + "/1")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

            beerDtoMono.subscribe(beer -> {
                assertNotNull(beer);
                assertNotNull(beer.getBeerName());
                countDownLatch.countDown();
            });
            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

        @Test
        void getBeerByIdNotFound() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            Mono<BeerDto> beerDtoMono = webClient.get().uri(BEER_V2_PATH + "/222")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

            beerDtoMono.subscribe(beer -> {
            }, throwable -> countDownLatch.countDown());
            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

        @Test
        void getBeerByUpc() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            Mono<BeerDto> beerDtoMono = webClient.get().uri(BEER_V2_PATH + "/beerUpc/" + BeerLoader.BEER_10_UPC)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

            beerDtoMono.subscribe(beer -> {
                assertNotNull(beer);
                assertNotNull(beer.getBeerName());
                countDownLatch.countDown();
            });
            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

        @Test
        void getBeerByUpcNotFound() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            Mono<BeerDto> beerDtoMono = webClient.get().uri(BEER_V2_PATH + "/beerUpc/32123213")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

            beerDtoMono.subscribe(beer -> {
            }, throwable -> countDownLatch.countDown());
            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

    }

    @Nested
    class PostBeer {

        @Test
        void postBeer() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            BeerDto validBeer = BeerDto.builder()
                .beerName("Heineken")
                .beerStyle(BeerStyleEnum.LAGER.name())
                .upc("12345231312")
                .price(new BigDecimal("10.12"))
                .build();
            Mono<ResponseEntity<Void>> responseEntityMono = webClient.post().uri(BEER_V2_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(validBeer))
                .retrieve().toBodilessEntity();
            responseEntityMono.publishOn(Schedulers.parallel()).subscribe(response -> {
                assertTrue(response.getStatusCode().is2xxSuccessful());
                countDownLatch.countDown();
            });
            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }


        @Test
        void postBeerBadRequest() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            BeerDto invalidBeer = BeerDto.builder().price(new BigDecimal("10.12")).build();
            Mono<ResponseEntity<Void>> responseEntityMono = webClient.post().uri(BEER_V2_PATH)
                .accept(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(invalidBeer))
                .retrieve().toBodilessEntity();

            responseEntityMono.subscribe(response -> {
            }, throwable -> {
                if (throwable instanceof WebClientResponseException.BadRequest) {
                    WebClientResponseException ex = (WebClientResponseException) throwable;
                    if (ex.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                        countDownLatch.countDown();
                    }
                }
            });
            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }
    }

    @Nested
    class PutBeer {

        @Test
        void putBeer() throws InterruptedException {
            BeerDto beerDto = BeerDto.builder()
                .beerName("Levi Beer")
                .price(new BigDecimal("0.01"))
                .beerStyle(BeerStyleEnum.IPA.name())
                .build();
            CountDownLatch countDownLatch = new CountDownLatch(2);

            webClient.put().uri(BEER_V2_PATH + "/12").accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(beerDto)).retrieve().toBodilessEntity()
                .subscribe(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    countDownLatch.countDown();
                });
            countDownLatch.await(1, TimeUnit.SECONDS);

            webClient.get().uri(BEER_V2_PATH + "/12").accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerDto.class)
                .subscribe(beer -> {
                    assertEquals(beer.getBeerName(), beer.getBeerName());
                    countDownLatch.countDown();
                });
            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

        @Test
        void testUpdateBeerNotFound() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            webClient.put().uri(BeerRouterConfig.BEER_V2_URL + "/999")
                .accept(MediaType.APPLICATION_JSON).body(BodyInserters
                .fromValue(BeerDto.builder()
                    .beerName("Levi Beer")
                    .upc("1233455")
                    .beerStyle("PALE_ALE")
                    .price(new BigDecimal("8.99"))
                    .build()))
                .retrieve().toBodilessEntity()
                .subscribe(responseEntity -> {
                    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
                }, throwable -> {
                    countDownLatch.countDown();
                });
            countDownLatch.await(1000, TimeUnit.MILLISECONDS);
            assertEquals(0, countDownLatch.getCount());
        }
    }

    @Nested
    class DeleteBeer {
        @Test
        void testDeleteBeer() {
            CountDownLatch countDownLatch = new CountDownLatch(2);

            webClient.delete().uri("/api/v2/beer/3")
                .retrieve().toBodilessEntity()
                .flatMap(responseEntity -> {
                    countDownLatch.countDown();
                    return webClient.get().uri("/api/v2/beer/3")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve().bodyToMono(BeerDto.class);
                })
                .subscribe(savedDto -> {
                }, throwable -> {
                    countDownLatch.countDown();
                });
        }

        @Test
        void testDeleteBeerNotFound() {
            webClient.delete().uri("/api/v2/beer/4")
                .retrieve().toBodilessEntity().block();

            assertThrows(WebClientResponseException.NotFound.class, () -> webClient.delete().uri("/api/v2/beer/4")
                .retrieve().toBodilessEntity().block());
        }
    }

}
