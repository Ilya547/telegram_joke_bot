package io.ilyaTGbot.demoSpringBot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Data
public class Joke {

    @Column(length = 2550000)
    private String body;

    private String category;

    @Id
    private Integer id;

    private double rating;



}
