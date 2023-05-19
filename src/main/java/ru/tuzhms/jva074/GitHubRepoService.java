package ru.tuzhms.jva074;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vavr.collection.List;
import ru.tuzhms.jva074.dto.Repo;
import ru.tuzhms.jva074.dto.User;

import java.util.concurrent.CompletableFuture;

public class GitHubRepoService {
    private final GitHubApi api;

    public GitHubRepoService(GitHubApi api) {
        this.api = api;
    }

    public CompletableFuture<User> loadUserWithRepos(String username) {
        final CompletableFuture<User> userFuture = api.user(username);

        final CompletableFuture<List<Repo>> reposFuture = api.userRepos(username, 1)
                .thenApply(repos -> repos.map(this::resolveRepo))
                .thenComposeAsync(this::sequenceCF);

        return userFuture.thenCombineAsync(reposFuture, User::setRepos);
    }

    private CompletableFuture<Repo> resolveRepo(Repo repo) {
        return loadContributors(repo).thenApply(repo::setContributors);
    }

    private CompletableFuture<List<User>> loadContributors(Repo repo) {
        return api.getAsync(repo.getContributors_url(), new TypeReference<List<User>>() {})
                .thenApply(users -> users.map(this::reloadUser))
                .thenComposeAsync(this::sequenceCF);
    }

    private CompletableFuture<User> reloadUser(User user) {
        return api.user(user.getLogin());
    }

    private <T> CompletableFuture<List<T>> sequenceCF(List<CompletableFuture<T>> futures) {
        return futures.foldLeft(CompletableFuture.completedFuture(List.empty()),
                (list, cf) -> list.thenCombineAsync(cf, List::append));
//        return CompletableFuture.allOf(futures.toJavaArray(CompletableFuture[]::new))
//                .thenApply(__ -> futures.map(CompletableFuture::join));
    }
}
