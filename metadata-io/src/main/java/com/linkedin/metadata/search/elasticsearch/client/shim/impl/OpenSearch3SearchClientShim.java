package com.linkedin.metadata.search.elasticsearch.client.shim.impl;

import com.datahub.context.OperationFingerprint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.metadata.search.elasticsearch.client.shim.builder.opensearch3.OpenSearch3KnnQueryBuilder;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim.SearchEngineType;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim.ShimConfiguration;
import com.linkedin.metadata.utils.elasticsearch.shim.KnnSearchRequest;
import com.linkedin.metadata.utils.elasticsearch.shim.KnnSearchResponse;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.RestHighLevelClient;

/**
 * OpenSearch 3.x shim (Path B phase B1).
 *
 * <p>Reuses the OpenSearch 2.x REST high-level client stack for REST API compatibility with
 * OpenSearch 3.6+ clusters. kNN queries use {@link OpenSearch3KnnQueryBuilder} (spike D1).
 *
 * <p>Native {@code opensearch-java} migration is Path B phase B2.
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
    super(client, new ObjectMapper());
    this.engineType = SearchEngineType.OPENSEARCH_3;
  }

  @Nonnull
  @Override
  public SearchEngineType getEngineType() {
    return SearchEngineType.OPENSEARCH_3;
  }

  @Nonnull
  @Override
  public KnnSearchResponse searchKnn(
      @Nonnull OperationFingerprint opContext, @Nonnull KnnSearchRequest request)
      throws IOException {
    Map<String, Object> body = OpenSearch3KnnQueryBuilder.build(request);
    String requestBody = objectMapper.writeValueAsString(body);

    String endpoint = "/" + request.indexName() + "/_search";
    org.opensearch.client.Request lowLevelReq = new org.opensearch.client.Request("POST", endpoint);
    lowLevelReq.setJsonEntity(requestBody);
    String ignoreUnavailableStr = String.valueOf(request.ignoreUnavailable());
    lowLevelReq.addParameter("ignore_unavailable", ignoreUnavailableStr);
    lowLevelReq.addParameter("allow_no_indices", ignoreUnavailableStr);

    org.opensearch.client.Response response =
        getNativeClient().getLowLevelClient().performRequest(lowLevelReq);
    String responseBody = org.apache.http.util.EntityUtils.toString(response.getEntity(), "UTF-8");
    JsonNode responseJson = objectMapper.readTree(responseBody);

    return parseSearchKnnResponse(responseJson, objectMapper);
  }
}
