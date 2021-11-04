package com.levimartines.netflux.services;

import com.levimartines.netflux.domain.Movie;
import com.levimartines.netflux.domain.MovieEvent;
import com.levimartines.netflux.repositories.MovieRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public Mono<Movie> save(Movie movie) {
        return movieRepository.save(movie);
    }

    public Mono<Movie> findById(String id) {
        return movieRepository.findById(id);
    }

    public Flux<Movie> findAll() {
        return movieRepository.findAll();
    }

    public Mono<Void> delete(String id) {
        return movieRepository.deleteById(id);
    }

    public Flux<MovieEvent> streamMovieEvents(String movieId) {
        return Flux.<MovieEvent>generate(eventSynchronousSink -> eventSynchronousSink
            .next(new MovieEvent(movieId, new Date())))
            .delayElements(Duration.ofSeconds(1));
    }
}
