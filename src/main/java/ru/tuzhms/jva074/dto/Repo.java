package ru.tuzhms.jva074.dto;

import io.vavr.collection.List;
import lombok.Data;

@Data
public class Repo {
    private Long id;
    private String name;
    private String description;
    private String html_url;
    private String contributors_url;
    private List<User> contributors = List.empty();

    public Repo setContributors(List<User> contributors) {
        this.contributors = contributors;
        return this;
    }
}
