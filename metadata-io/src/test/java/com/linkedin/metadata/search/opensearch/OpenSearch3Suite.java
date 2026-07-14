package com.linkedin.metadata.search.opensearch;

import io.datahubproject.test.search.OpenSearch3TestContainer;
import io.datahubproject.test.search.SearchContainerUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testng.annotations.AfterSuite;

@TestConfiguration
public class OpenSearch3Suite extends AbstractTestNGSpringContextTests {

  private static OpenSearch3TestContainer OPENSEARCH_TEST_CONTAINER = null;
  private static GenericContainer<?> container;

  @AfterSuite
  public void after() {
    if (OPENSEARCH_TEST_CONTAINER != null) {
      OPENSEARCH_TEST_CONTAINER.stopContainer();
    }
  }

  @Bean(name = "testSearchContainer")
  public GenericContainer<?> testSearchContainer() {
    if (container == null || !container.isRunning()) {
      OPENSEARCH_TEST_CONTAINER = new OpenSearch3TestContainer();
      container = OPENSEARCH_TEST_CONTAINER.startContainer();
      SearchContainerUtils.waitForClusterReady(container);
    }
    return container;
  }
}
