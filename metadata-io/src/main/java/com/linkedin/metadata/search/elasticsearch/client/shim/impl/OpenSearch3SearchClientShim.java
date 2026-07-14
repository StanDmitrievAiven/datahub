package com.linkedin.metadata.search.elasticsearch.client.shim.impl;

import com.linkedin.metadata.utils.elasticsearch.SearchClientShim.SearchEngineType;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim.ShimConfiguration;
import java.io.IOException;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.RestHighLevelClient;

/**
 * OpenSearch 3.x shim (Path B phase B1).
 *
 * <p>Reuses the OpenSearch 2.x REST high-level client stack for REST API compatibility with
 * OpenSearch 3.6+ clusters. Native {@code opensearch-java} migration is Path B phase B2.
 *
 * @see com.linkedin.metadata.search.elasticsearch.client.shim.impl.OpenSearch2SearchClientShim
 */
@Slf4j
public class OpenSearch3SearchClientShim extends OpenSearch2SearchClientShim {

  public OpenSearch3SearchClientShim(@Nonnull ShimConfiguration config) throws IOException {
    super(config);
    this.engineType = SearchEngineType.OPENSEARCH_3;
    log.info("Created OpenSearch 3.x shim for engine type: {}", engineType);
  }

  /** Package-private factory for tests; avoids spinning up a real OS connection. */
  static OpenSearch3SearchClientShim forTest(RestHighLevelClient client) {
    return new OpenSearch3SearchClientShim(client);
  }

  private OpenSearch3SearchClientShim(RestHighLevelClient client) {
    super(client, new com.fasterxml.jackson.databind.ObjectMapper());
    this.engineType = SearchEngineType.OPENSEARCH_3;
  }

  @Nonnull
  @Override
  public SearchEngineType getEngineType() {
    return SearchEngineType.OPENSEARCH_3;
  }
}
