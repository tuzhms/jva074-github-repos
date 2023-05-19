package ru.tuzhms.jva074;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.List;
import io.vavr.control.Try;
import io.vavr.jackson.datatype.VavrModule;
import ru.tuzhms.jva074.dto.ErrorBody;
import ru.tuzhms.jva074.dto.Repo;
import ru.tuzhms.jva074.dto.User;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class GitHubApi {
    private static final String GITHUB_API_HOST = "https://api.github.com/";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String githubToken;

    public GitHubApi(String githubToken) {
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new VavrModule());
        this.githubToken = githubToken;
    }

    public CompletableFuture<User> user(String username) {
        return getAsync(GITHUB_API_HOST + "users/" + URLEncoder.encode(username, StandardCharsets.UTF_8),
                new TypeReference<>() {});
    }

    public CompletableFuture<List<Repo>> userRepos(String username, int page) {
        final String url = GITHUB_API_HOST +
                String.format("users/%s/repos?type=all&sort=pushed&per_page=10&page=%d",
                        URLEncoder.encode(username, StandardCharsets.UTF_8),
                        Math.max(page, 1));

        return getAsync(url, new TypeReference<>() {});
    }

    public <T> CompletableFuture<T> getAsync(String url, TypeReference<T> type) {
        final HttpRequest request = request()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(parseResponse(type))
                .thenCompose(Try::toCompletableFuture);
    }

    private HttpRequest.Builder request() {
        final HttpRequest.Builder httpRequest = HttpRequest.newBuilder()
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .header("Accept", "application/vnd.github+json");

        if (githubToken != null) {
            return httpRequest.header("Authorization", "Bearer " + githubToken);
        }

        return httpRequest;
    }

    private <T> Function<HttpResponse<InputStream>, Try<T>> parseResponse(TypeReference<T> type) {
        return response -> {
//            System.out.printf("<<>> %s GET %s\n", Thread.currentThread().getName(), response.request().uri());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Try.of(response::body)
                        .mapTry(body -> objectMapper.readValue(body, type));
            } else {
                return Try.of(response::body)
                        .mapTry(body -> objectMapper.readValue(body, ErrorBody.class))
                        .map(ErrorBody::getMessage)
                        .map(message -> response.statusCode() + " " + message)
                        .map(GitHubException::new)
                        .flatMap(Try::failure);
            }
        };
    }
}
