package io.ilyaTGbot.demoSpringBot.model;

import org.springframework.data.repository.CrudRepository;

public interface JokeRepository extends CrudRepository<Joke, Integer> {
}
