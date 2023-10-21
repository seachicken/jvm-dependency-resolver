package inga.jvmdependencyloader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyLoaderTest {
    DependencyLoader loader;

    @BeforeEach
    void setUp() {
        loader = new DependencyLoader();
    }

    @AfterEach
    void tierDown() throws Exception {
        loader.close();
    }

    @Test
    void readMethodsForMaven() {
        var actual = loader.readMethods(
                "org.springframework.web.util.UriComponentsBuilder",
                getFixturesPath("spring-tutorials/lightrun/api-service")
        );
        assertThat(actual).size().isEqualTo(68);
    }

    @Test
    void readMethodsForGradle() {
        var actual = loader.readMethods(
                "org.joda.time.DateTime",
                getFixturesPath("spring-boot-realworld-example-app")
        );
        assertThat(actual).size().isEqualTo(89);
    }

    @Test
    void readHierarchyForMaven() {
        var actual = loader.readHierarchy(
                "java.lang.String",
                getFixturesPath("spring-tutorials/lightrun/api-service")
        );
        assertThat(actual).containsExactly(
                new Type("java.lang.Object", false),
                new Type("java.lang.String", false)
        );
    }

    @Test
    void readHierarchyForGradle() {
        var actual = loader.readHierarchy(
                "java.lang.String",
                getFixturesPath("spring-boot-realworld-example-app")
        );
        assertThat(actual).containsExactly(
                new Type("java.lang.Object", false),
                new Type("java.lang.String", false)
        );
    }

    private Path getFixturesPath(String path) {
        return Path.of(getClass().getClassLoader().getResource("fixtures/" + path).getFile());
    }
}