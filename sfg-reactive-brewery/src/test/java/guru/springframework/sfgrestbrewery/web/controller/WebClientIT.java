package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientIT {

    public static final String BASE_URL = "http://localhost:8080";

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

            Mono<BeerDto> beerDtoMono = webClient.get().uri("api/v1/beer/1")
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

            Mono<BeerDto> beerDtoMono = webClient.get().uri("api/v1/beer/222")
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

            Mono<BeerDto> beerDtoMono = webClient.get().uri("api/v1/beerUpc/" + BeerLoader.BEER_10_UPC)
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
        void getBeerPage() throws InterruptedException {

            CountDownLatch countDownLatch = new CountDownLatch(1);

            Mono<BeerPagedList> beerPagedListMono = webClient.get().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);

            beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {
                beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));
                countDownLatch.countDown();
            });

            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

        @Test
        void getBeerPageFilterByName() throws InterruptedException {

            CountDownLatch countDownLatch = new CountDownLatch(1);

            Mono<BeerPagedList> beerPagedListMono = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/beer").queryParam("beerName", "Mango").build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);

            beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {
                beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));
                countDownLatch.countDown();
            });

            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

        @Test
        void getBeerPageFilterByStyle() throws InterruptedException {

            CountDownLatch countDownLatch = new CountDownLatch(1);

            Mono<BeerPagedList> beerPagedListMono = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/beer").queryParam("beerStyle", BeerStyleEnum.ALE).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);

            beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {
                beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));
                countDownLatch.countDown();
            });

            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

        @Test
        void getBeerPageFilterByPageSize() throws InterruptedException {

            CountDownLatch countDownLatch = new CountDownLatch(1);

            Mono<BeerPagedList> beerPagedListMono = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/beer").queryParam("pageSize", "5").build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);

            beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {
                beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));
                countDownLatch.countDown();
            });

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
                .beerName("Brahma Duplo Malter")
                .beerStyle(BeerStyleEnum.LAGER.name())
                .upc("12345312313")
                .price(new BigDecimal("10.12"))
                .build();
            Mono<ResponseEntity<Void>> responseEntityMono = webClient.post().uri("api/v1/beer")
                .bodyValue(validBeer)
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

            BeerDto validBeer = BeerDto.builder()
                .price(new BigDecimal("10.12"))
                .build();
            Mono<ResponseEntity<Void>> responseEntityMono = webClient.post().uri("api/v1/beer")
                .bodyValue(validBeer)
                .retrieve().toBodilessEntity();
            responseEntityMono.publishOn(Schedulers.parallel())
                .doOnError(t -> countDownLatch.countDown())
                .subscribe(response -> assertTrue(response.getStatusCode().is4xxClientError()));
            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }
    }

    @Nested
    class PutBeer {

        @Test
        void updateBeer() throws InterruptedException {

            CountDownLatch countDownLatch = new CountDownLatch(2);

            BeerDto updatePayload = BeerDto.builder().beerName("JTsUpdate")
                .beerStyle(BeerStyleEnum.IPA.name())
                .upc("12980371298")
                .price(new BigDecimal("0.09"))
                .build();

            webClient.put().uri("/api/v1/beer/1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(updatePayload))
                .retrieve().toBodilessEntity()
                .flatMap(responseEntity -> {
                    countDownLatch.countDown();
                    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
                    return webClient.get().uri("/api/v1/beer/1")
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve().bodyToMono(BeerDto.class);
                })
                .subscribe(savedDto -> {
                    assertEquals("JTsUpdate", savedDto.getBeerName());
                    countDownLatch.countDown();
                });

            countDownLatch.await(1000, TimeUnit.MILLISECONDS);
            assertEquals(0, countDownLatch.getCount());
        }


        @Test
        void updateBeerNotFound() throws InterruptedException {

            CountDownLatch countDownLatch = new CountDownLatch(1);

            BeerDto updatePayload = BeerDto.builder().beerName("JTsUpdate")
                .beerStyle(BeerStyleEnum.IPA.name())
                .upc("12980371298")
                .price(new BigDecimal("0.09"))
                .build();

            webClient.put().uri("/api/v1/beer/321312")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(updatePayload))
                .retrieve().toBodilessEntity()
                .subscribe(responseEntity -> {
                }, throwable -> {
                    if (throwable instanceof WebClientResponseException.NotFound) {
                        WebClientResponseException exception = (WebClientResponseException) throwable;
                        if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                            countDownLatch.countDown();
                        }
                    }
                });

            countDownLatch.await(1000, TimeUnit.MILLISECONDS);
            assertEquals(0, countDownLatch.getCount());
        }
    }

    @Nested
    class DeleteBeer {

        @Test
        void deleteBeer() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            webClient.delete().uri("api/v1/beer/11")
                .retrieve().toBodilessEntity()
                .subscribe(o -> countDownLatch.countDown());

            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }

        @Test
        void deleteBeerNotFound() throws InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);

            webClient.delete().uri("api/v1/beer/111")
                .retrieve().toBodilessEntity()
                .subscribe(o -> countDownLatch.countDown());

            countDownLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, countDownLatch.getCount());
        }
    }

}
