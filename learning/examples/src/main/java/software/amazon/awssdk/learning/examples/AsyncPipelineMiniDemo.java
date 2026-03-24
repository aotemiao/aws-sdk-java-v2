package software.amazon.awssdk.learning.examples;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Staged async pipeline with {@code thenCompose} (sequential dependency) vs {@code thenCombine}
 * (parallel branches). Compare with SDK async clients returning {@code CompletableFuture}.
 */
public final class AsyncPipelineMiniDemo {

    private AsyncPipelineMiniDemo() {
    }

    public static void run() {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<String> pipeline =
                CompletableFuture.supplyAsync(() -> loadUserId(), pool)
                                 .thenCompose(userId -> loadProfile(userId, pool))
                                 .thenApply(profile -> "greeting:" + profile);

            System.out.println(pipeline.join());

            CompletableFuture<String> parallel =
                CompletableFuture.supplyAsync(() -> slow("A", 50), pool)
                                 .thenCombine(
                                     CompletableFuture.supplyAsync(() -> slow("B", 50), pool),
                                     (a, b) -> a + "+" + b);
            System.out.println(parallel.join());
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String loadUserId() {
        return "user-42";
    }

    private static CompletableFuture<String> loadProfile(String userId, ExecutorService pool) {
        return CompletableFuture.supplyAsync(() -> "profile(" + userId + ")", pool);
    }

    private static String slow(String label, int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return label + "-interrupted";
        }
        return label;
    }
}
