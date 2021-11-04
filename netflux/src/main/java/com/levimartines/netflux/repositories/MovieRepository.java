package com.levimartines.netflux.repositories;

import com.levimartines.netflux.domain.Movie;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
}
