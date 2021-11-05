package guru.springframework.sfgrestbrewery.web.functional;

import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.controller.NotFoundException;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import reactor.core.publisher.Mono;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerHandlerV2 {
    private final BeerService beerService;
    private final Validator validator;

    public Mono<ServerResponse> getBeerById(ServerRequest request) {
        Integer beerId = Integer.valueOf(request.pathVariable("beerId"));
        Boolean showInventory = Boolean.valueOf(request.queryParam("showInventory").orElse("false"));
        return beerService.getById(beerId, showInventory)
            .flatMap(beerDto -> ServerResponse.ok().bodyValue(beerDto))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getBeerByUpc(ServerRequest request) {
        String beerUpc = request.pathVariable("upc");
        return beerService.getByUpc(beerUpc)
            .flatMap(beerDto -> ServerResponse.ok().bodyValue(beerDto))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> saveNewBeer(ServerRequest request) {
        Mono<BeerDto> beerDto = request.bodyToMono(BeerDto.class).doOnNext(this::validate);
        return beerService.saveNewBeer(beerDto)
            .flatMap(beer -> ServerResponse.ok()
                .header("location", BeerRouterConfig.BEER_V2_URL + "/" + beer.getId())
                .build());
    }

    public Mono<ServerResponse> updateBeer(ServerRequest request) {
        Integer beerId = Integer.valueOf(request.pathVariable("beerId"));
        return request.bodyToMono(BeerDto.class).doOnNext(this::validate)
            .flatMap(beerDto -> beerService.updateBeer(beerId, beerDto))
            .flatMap(updatedBeer -> updatedBeer.getId() != null ? ServerResponse.noContent().build() : ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> deleteById(ServerRequest request) {
        Integer beerId = Integer.valueOf(request.pathVariable("beerId"));
        return beerService.reactiveDeleteById(beerId)
            .flatMap(monoVoid -> ServerResponse.ok().build())
            .onErrorResume(error -> error instanceof NotFoundException, error -> ServerResponse.notFound().build());
    }

    private void validate(BeerDto beerDto) {
        Errors errors = new BeanPropertyBindingResult(beerDto, "beerDto");
        validator.validate(beerDto, errors);
        if (errors.hasErrors()) {
            throw new ServerWebInputException(errors.toString());
        }
    }
}
