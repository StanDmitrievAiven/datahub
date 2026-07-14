package com.linkedin.metadata.search.opensearch;

import static org.testng.Assert.*;

import com.linkedin.metadata.search.elasticsearch.client.shim.SearchClientShimUtil.ShimConfigurationBuilder;
import com.linkedin.metadata.search.elasticsearch.client.shim.impl.OpenSearch3SearchClientShim;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim.SearchEngineType;
import java.util.Map;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Optional live smoke against Aiven OpenSearch 3. Skipped unless {@code AIVEN_OPENSEARCH_HOST} is
 * set (credentials via AIVEN_OPENSEARCH_* env vars; never commit secrets).
 */
public class OpenSearch3AivenSmokeTest {

  @BeforeClass
  public void requireAivenEnv() {
    if (System.getenv("AIVEN_OPENSEARCH_HOST") == null
        || System.getenv("AIVEN_OPENSEARCH_HOST").isBlank()) {
      throw new SkipException("Set AIVEN_OPENSEARCH_HOST to run Aiven OS3 smoke");
    }
  }

  @Test
  public void testOpenSearch3ShimAgainstAiven() throws Exception {
    String host = System.getenv("AIVEN_OPENSEARCH_HOST");
    int port = Integer.parseInt(System.getenv().getOrDefault("AIVEN_OPENSEARCH_PORT", "443"));
    String user = System.getenv().getOrDefault("AIVEN_OPENSEARCH_USERNAME", "");
    String pass = System.getenv().getOrDefault("AIVEN_OPENSEARCH_PASSWORD", "");

    SearchClientShim.ShimConfiguration config =
        new ShimConfigurationBuilder()
            .withEngineType(SearchEngineType.OPENSEARCH_3)
            .withHost(host)
            .withPort(port)
            .withSSL(true)
            .withCredentials(user.isBlank() ? null : user, pass.isBlank() ? null : pass)
            .withThreadCount(1)
            .withConnectionRequestTimeout(10000)
            .withSocketTimeout(30000)
            .build();

    try (OpenSearch3SearchClientShim shim = new OpenSearch3SearchClientShim(config)) {
      assertEquals(shim.getEngineType(), SearchEngineType.OPENSEARCH_3);
      String version = shim.getEngineVersion();
      assertTrue(version.startsWith("3."), "Expected OS3 version, got: " + version);
      Map<String, String> info = shim.getClusterInfo();
      assertEquals(info.get("engine_type"), "opensearch");
    }
  }
}
