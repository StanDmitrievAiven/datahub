package io.datahubproject.test.search;

import static com.linkedin.metadata.DockerTestUtils.checkContainerEngine;

import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Testcontainers helper for OpenSearch 3.x (Path B). */
public class OpenSearch3TestContainer implements SearchTestContainer {
  private static final String OPENSEARCH_VERSION = "3.7.0";
  private static final String OPENSEARCH_IMAGE_NAME = "opensearchproject/opensearch";
  private static final String ENV_OPENSEARCH_IMAGE_FULL_NAME =
      System.getenv("OPENSEARCH3_IMAGE_FULL_NAME");
  private static final String OPENSEARCH_IMAGE_FULL_NAME =
      ENV_OPENSEARCH_IMAGE_FULL_NAME != null
          ? ENV_OPENSEARCH_IMAGE_FULL_NAME
          : OPENSEARCH_IMAGE_NAME + ":" + OPENSEARCH_VERSION;
  private static final DockerImageName DOCKER_IMAGE_NAME =
      DockerImageName.parse(OPENSEARCH_IMAGE_FULL_NAME)
          .asCompatibleSubstituteFor(OPENSEARCH_IMAGE_NAME);

  private static final String OPENSEARCH_HEAP =
      System.getProperty("testcontainers.opensearch.heap", "1024m");
  private static final String OPENSEARCH_JAVA_OPTS =
      "-Xms" + OPENSEARCH_HEAP + " -Xmx" + OPENSEARCH_HEAP;

  protected static final GenericContainer<?> OS_CONTAINER;
  private boolean isStarted = false;

  static {
    OS_CONTAINER = new OpensearchContainer(DOCKER_IMAGE_NAME);
    checkContainerEngine(OS_CONTAINER.getDockerClient());
    OS_CONTAINER
        .withEnv("OPENSEARCH_JAVA_OPTS", OPENSEARCH_JAVA_OPTS)
        .withEnv("DISABLE_SECURITY_PLUGIN", "true")
        .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
        .withStartupTimeout(STARTUP_TIMEOUT);
  }

  @Override
  public GenericContainer<?> startContainer() {
    if (!isStarted) {
      OS_CONTAINER.start();
      isStarted = true;
    }
    return OS_CONTAINER;
  }

  @Override
  public void stopContainer() {
    OS_CONTAINER.stop();
  }
}
