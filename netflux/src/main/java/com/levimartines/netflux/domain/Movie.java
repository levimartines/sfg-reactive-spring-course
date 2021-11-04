package com.levimartines.netflux.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie implements Serializable {

    @Id
    private String id;
    @NonNull
    private String title;

    public Movie(@NonNull String title) {
        this.title = title;
    }
}