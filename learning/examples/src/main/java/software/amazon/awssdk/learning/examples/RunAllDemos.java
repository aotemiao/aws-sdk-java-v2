/*
 * Pedagogical examples for exploring SDK-style patterns locally.
 * Not affiliated with AWS product APIs; simplified for clarity.
 */
package software.amazon.awssdk.learning.examples;

public final class RunAllDemos {

    private RunAllDemos() {
    }

    public static void main(String[] args) {
        System.out.println("=== Builder & immutable config ===");
        BuilderAndImmutableConfigDemo.run();
        System.out.println();

        System.out.println("=== Http SPI-style client ===");
        HttpClientSpiDemo.run();
        System.out.println();

        System.out.println("=== Async pipeline (mini) ===");
        AsyncPipelineMiniDemo.run();
    }
}
