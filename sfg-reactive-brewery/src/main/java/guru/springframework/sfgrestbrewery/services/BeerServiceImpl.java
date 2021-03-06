package guru.springframework.sfgrestbrewery.services;

import guru.springframework.sfgrestbrewery.domain.Beer;
import guru.springframework.sfgrestbrewery.repositories.BeerRepository;
import guru.springframework.sfgrestbrewery.web.controller.NotFoundException;
import guru.springframework.sfgrestbrewery.web.mappers.BeerMapper;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import guru.springframework.sfgrestbrewery.web.model.BeerStyleEnum;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.empty;
import static org.springframework.data.relational.core.query.Query.query;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeerServiceImpl implements BeerService {
    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;
    private final R2dbcEntityTemplate template;

    @Cacheable(cacheNames = "beerListCache", condition = "#showInventoryOnHand == false ")
    @Override
    public Mono<BeerPagedList> listBeers(String beerName, BeerStyleEnum beerStyle, PageRequest pageRequest, Boolean showInventoryOnHand) {
        Query query = empty();
        if (!ObjectUtils.isEmpty(beerName) && !ObjectUtils.isEmpty(beerStyle)) {
            query = query(where("beerName").like("%" + beerName + "%").and("beerStyle").is(beerStyle));
        } else if (!ObjectUtils.isEmpty(beerName) && ObjectUtils.isEmpty(beerStyle)) {
            query = query(where("beerName").like("%" + beerName + "%"));
        } else if (ObjectUtils.isEmpty(beerName) && !ObjectUtils.isEmpty(beerStyle)) {
            query = query(where("beerStyle").is(beerStyle));
        }

        return template.select(Beer.class)
            .matching(query.with(pageRequest))
            .all()
            .map(beerMapper::beerToBeerDto)
            .collect(Collectors.toList())
            .map(beers -> new BeerPagedList(beers, PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize()), beers.size()));
    }

    @Cacheable(cacheNames = "beerCache", key = "#beerId", condition = "#showInventoryOnHand == false ")
    @Override
    public Mono<BeerDto> getById(Integer beerId, Boolean showInventoryOnHand) {
        if (showInventoryOnHand) {
            return beerRepository.findById(beerId).map(beerMapper::beerToBeerDtoWithInventory);
        } else {
            return beerRepository.findById(beerId).map(beerMapper::beerToBeerDto);
        }
    }

    @Override
    public Mono<BeerDto> saveNewBeer(BeerDto beerDto) {
        Beer beer = beerMapper.beerDtoToBeer(beerDto);
        return beerRepository.save(beer).map(beerMapper::beerToBeerDto);
    }

    @Override
    public Mono<BeerDto> saveNewBeer(Mono<BeerDto> beerDto) {
        return beerDto.map(beerMapper::beerDtoToBeer)
            .flatMap(beerRepository::save)
            .map(beerMapper::beerToBeerDto);
    }

    @Override
    public Mono<BeerDto> updateBeer(Integer beerId, BeerDto beerDto) {
        return beerRepository.findById(beerId)
            .defaultIfEmpty(new Beer())
            .map(beer -> {
                beer.setBeerName(beerDto.getBeerName());
                beer.setBeerStyle(BeerStyleEnum.valueOf(beerDto.getBeerStyle()));
                beer.setPrice(beerDto.getPrice());
                beer.setUpc(beerDto.getUpc());
                return beer;
            })
            .flatMap(updatedBeer -> updatedBeer.getId() == null ? Mono.just(updatedBeer) : beerRepository.save(updatedBeer))
            .map(beerMapper::beerToBeerDto);
    }

    @Cacheable(cacheNames = "beerUpcCache")
    @Override
    public Mono<BeerDto> getByUpc(String upc) {
        return beerRepository.findByUpc(upc).map(beerMapper::beerToBeerDto);
    }

    @Override
    public void deleteBeerById(Integer beerId) {
        beerRepository.deleteById(beerId).subscribe();
    }

    public Mono<Void> reactiveDeleteById(Integer beerId) {
        return beerRepository.findById(beerId)
            .switchIfEmpty(Mono.error(new NotFoundException()))
            .map(Beer::getId)
            .flatMap(beerRepository::deleteById);
    }
}
