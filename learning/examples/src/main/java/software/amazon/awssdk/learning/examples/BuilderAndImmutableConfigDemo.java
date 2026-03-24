package software.amazon.awssdk.learning.examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mirrors SDK habits: fluent builder, immutable built object, defensive copy of collections.
 * Experiment: mutate the list after {@code build()} and see inner state stay stable.
 */
public final class BuilderAndImmutableConfigDemo {

    private BuilderAndImmutableConfigDemo() {
    }

    public static void run() {
        List<String> tags = new ArrayList<>();
        tags.add("a");

        RequestConfig cfg = RequestConfig.builder()
                                         .bucket("demo-bucket")
                                         .tags(tags)
                                         .addTag("b")
                                         .build();

        tags.add("evil-after-build");

        System.out.println("External list after tamper: " + tags);
        System.out.println("Immutable config tags: " + cfg.tags());
    }

    /** Immutable snapshot after build. */
    public static final class RequestConfig {
        private final String bucket;
        private final List<String> tags;

        private RequestConfig(Builder b) {
            this.bucket = b.bucket;
            this.tags = Collections.unmodifiableList(new ArrayList<>(b.tags));
        }

        public static Builder builder() {
            return new Builder();
        }

        public String bucket() {
            return bucket;
        }

        public List<String> tags() {
            return tags;
        }

        public static final class Builder {
            private String bucket;
            private final List<String> tags = new ArrayList<>();

            public Builder bucket(String bucket) {
                this.bucket = bucket;
                return this;
            }

            public Builder addTag(String tag) {
                this.tags.add(tag);
                return this;
            }

            /** Last call wins for bulk tags; copies at build time. */
            public Builder tags(List<String> tags) {
                this.tags.clear();
                this.tags.addAll(tags);
                return this;
            }

            public RequestConfig build() {
                return new RequestConfig(this);
            }
        }
    }
}
