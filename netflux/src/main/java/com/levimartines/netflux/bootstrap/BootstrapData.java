package com.levimartines.netflux.bootstrap;

import com.levimartines.netflux.domain.Movie;
import com.levimartines.netflux.repositories.MovieRepository;
import reactor.core.publisher.Flux;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final MovieRepository movieRepository;

    @Override
    public void run(String... args) {
        movieRepository.deleteAll()
            .thenMany(Flux
                .just("Silence of the Lambdas", "Enter the Mono<Void> ", "Back to the Future", "Meet the Fluxes", "Lord of the Fluxes")
                .map(Movie::new)
                .flatMap(movieRepository::save)
            ).subscribe(null, null, () -> movieRepository.findAll().subscribe(System.out::println));
    }
}
