package guru.springframework.reactivebeerclient.client;

import guru.springframework.reactivebeerclient.config.WebClientConfig;
import guru.springframework.reactivebeerclient.model.BeerDto;
import guru.springframework.reactivebeerclient.model.BeerPagedList;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeerClientImplTest {

    BeerClient beerClient;

    @BeforeEach
    void setUp() {
        beerClient = new BeerClientImpl(new WebClientConfig().webClient());
    }

    @Test
    void listBeers() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(null, null, null, null, null);
        BeerPagedList beerList = beerPagedListMono.block();
        assertNotNull(beerList);
        assertTrue(beerList.getContent().size() > 0);
    }

    @Test
    void listBeersMax10() {
        Mono<BeerPagedList> beerPagedListMonoMax10 = beerClient.listBeers(1, 10, null, null, null);
        BeerPagedList beerList = beerPagedListMonoMax10.block();
        assertNotNull(beerList);
        assertEquals(10, beerList.getContent().size());
    }

    @Test
    void getBeerById() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(null, null, null, null, null);
        BeerPagedList beerList = beerPagedListMono.block();
        UUID beerId = beerList.getContent().get(0).getId();

        Mono<BeerDto> beerDtoMono = beerClient.getBeerById(beerId, true);
        BeerDto beerDto = beerDtoMono.block();
        assertEquals(beerId, beerDto.getId());
        assertNotNull(beerDto.getQuantityOnHand());

    }

    @Test
    void createBeer() {
        BeerDto beerDto = BeerDto.builder()
            .beerName("Brahma")
            .beerStyle("IPA")
            .upc("78931237892131")
            .price(new BigDecimal("9.99"))
            .build();
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.createBeer(beerDto);
        ResponseEntity<Void> responseEntity = responseEntityMono.block();
        assertEquals(201, responseEntity.getStatusCodeValue());
        assertNotNull(responseEntity.getHeaders().getLocation());
    }

    @Test
    void updateBeer() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(null, null, null, null, null);
        BeerPagedList beerList = beerPagedListMono.block();
        BeerDto beerDto = beerList.getContent().get(0);
        BeerDto updatedBeerDto = BeerDto.builder()
            .beerName("Brahma Puro Malte")
            .price(beerDto.getPrice())
            .beerStyle(beerDto.getBeerStyle())
            .build();
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.updateBeer(beerDto.getId(), updatedBeerDto);
        ResponseEntity<Void> responseEntity = responseEntityMono.block();
        assertEquals(204, responseEntity.getStatusCodeValue());
    }

    @Test
    void deleteBeerById() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(null, null, null, null, null);
        BeerPagedList beerList = beerPagedListMono.block();
        UUID id = beerList.getContent().get(0).getId();
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(id);
        ResponseEntity<Void> responseEntity = responseEntityMono.block();
        assertEquals(204, responseEntity.getStatusCodeValue());
    }

    @Test
    void deleteBeerByIdNotFound() {
        UUID id = UUID.randomUUID();
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(id);
        assertThrows(WebClientResponseException.class, () -> {
            ResponseEntity<Void> responseEntity = responseEntityMono.block();
            assertEquals(404, responseEntity.getStatusCodeValue());
        });
    }

    @Test
    void deleteBeerByIdHandleException() {
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(UUID.randomUUID());
        ResponseEntity<Void> responseEntity = responseEntityMono.onErrorResume(throwable -> {
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException exception = (WebClientResponseException) throwable;
                return Mono.just(ResponseEntity.status(exception.getStatusCode()).build());
            } else {
                throw new RuntimeException(throwable);
            }
        }).block();
        assertEquals(404, responseEntity.getStatusCodeValue());
    }

    @Test
    void getBeerByUPC() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(null, null, null, null, null);
        BeerPagedList beerList = beerPagedListMono.block();
        String beerUpc = beerList.getContent().stream().filter(obj -> obj.getUpc() != null).findFirst().get().getUpc();

        Mono<BeerDto> beerDtoMono = beerClient.getBeerByUPC(beerUpc);
        BeerDto beerDto = beerDtoMono.block();
        assertEquals(beerUpc, beerDto.getUpc());
    }
}