package com.levimartines.netflux.controllers;

import com.levimartines.netflux.domain.Movie;
import com.levimartines.netflux.domain.MovieEvent;
import com.levimartines.netflux.services.MovieService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/movies")
@RestController
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    public Mono<Movie> save(@RequestBody Movie movie) {
        movie.setId(null);
        return movieService.save(movie);
    }

    @GetMapping("/{id}")
    public Mono<Movie> findById(@PathVariable("id") String id) {
        return movieService.findById(id);
    }

    @GetMapping
    public Flux<Movie> findByAll() {
        return movieService.findAll();
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable("id") String id) {
        return movieService.delete(id);
    }


    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MovieEvent> streamMovieEvents(@PathVariable("id") String id) {
        return movieService.streamMovieEvents(id);
    }

}
