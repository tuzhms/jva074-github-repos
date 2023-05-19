package ru.tuzhms.jva074.dto;

import io.vavr.collection.List;
import lombok.Data;

@Data
public class User {
    private String login;
    private Long id;
    private String name;
    private String email;
    private String url;
    private String html_url;
    private String location;
    private List<Repo> repos = List.empty();

    public User setRepos(List<Repo> repos) {
        this.repos = repos;
        return this;
    }
}
