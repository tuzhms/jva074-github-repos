package ru.tuzhms.jva074;

import ru.tuzhms.jva074.dto.Repo;
import ru.tuzhms.jva074.dto.User;

public class GitHubRepos {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("GITHUB_TOKEN=token java ru.tuzhms.jva074.GitHubRepos username");
            System.exit(1);
        }

        final String username = args[0];
        final GitHubApi api = new GitHubApi(System.getenv("GITHUB_TOKEN"));
        final GitHubRepoService service = new GitHubRepoService(api);

        service.loadUserWithRepos(username)
                .whenComplete((user, throwable) -> {
                    if (throwable == null) {
                        printUser(user);
                    } else {
                        throwable.printStackTrace();
                    }
                })
                .join();
    }

    private static void printUser(User user) {
        System.out.printf("\u001B[1;94m%s\u001B[0m", user.getLogin());
        System.out.printf(" - %s", user.getName());
        if (user.getEmail() != null) System.out.printf(" <%s>", user.getEmail());
        System.out.println();

        user.getRepos().forEach(GitHubRepos::printRepo);
    }

    private static void printRepo(Repo repo) {
        System.out.printf("\t\u001B[0;92m%s\u001B[0m", repo.getName());
        if (repo.getDescription() == null) {
            System.out.printf(" - %s", repo.getHtml_url());
            System.out.println();
        } else {
            System.out.printf(" - %s", repo.getDescription());
            System.out.println();
            System.out.printf("\t  %s", repo.getHtml_url());
            System.out.println();
        }

        repo.getContributors().forEach(GitHubRepos::printContributor);
    }

    private static void printContributor(User user) {
        System.out.printf("\t\t\u001B[0;93m%s\u001B[0m", user.getLogin());
        System.out.printf(" - %s", user.getName());
        if (user.getEmail() != null) System.out.printf(" <%s>", user.getEmail());
        System.out.println();
    }
}
