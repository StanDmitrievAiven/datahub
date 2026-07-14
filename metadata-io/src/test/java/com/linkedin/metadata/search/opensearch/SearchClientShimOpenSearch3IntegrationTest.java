package com.linkedin.metadata.search.opensearch;

import static org.testng.Assert.*;

import com.datahub.context.OperationFingerprint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.metadata.search.elasticsearch.client.shim.SearchClientShimUtil;
import com.linkedin.metadata.search.elasticsearch.client.shim.SearchClientShimUtil.ShimConfigurationBuilder;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim.SearchEngineType;
import io.datahubproject.metadata.context.OperationContext;
import io.datahubproject.test.metadata.context.TestOperationContexts;
import io.datahubproject.test.search.config.SearchCommonTestConfiguration;
import io.datahubproject.test.search.config.SearchTestContainerConfiguration;
import java.io.IOException;
import java.util.Map;
import org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.opensearch.action.admin.indices.refresh.RefreshResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testng.annotations.Test;

@Import({
  OpenSearch3Suite.class,
  SearchCommonTestConfiguration.class,
  SearchTestContainerConfiguration.class
})
public class SearchClientShimOpenSearch3IntegrationTest extends AbstractTestNGSpringContextTests {
  private static final String TEST_INDEX = "test-shim-opensearch3-index";

  @Autowired private GenericContainer<?> openSearchContainer;
  @Autowired private SearchClientShim<?> searchClientShim;

  private static final OperationContext OP_CONTEXT =
      TestOperationContexts.systemContextNoSearchAuthorization();

  @Test
  public void testShimCreation() {
    assertNotNull(searchClientShim);
    assertEquals(searchClientShim.getEngineType(), SearchEngineType.OPENSEARCH_3);
    Object nativeClient = searchClientShim.getNativeClient();
    assertNotNull(nativeClient);
    assertTrue(nativeClient instanceof org.opensearch.client.RestHighLevelClient);
  }

  @Test
  public void testClusterInfo() throws IOException {
    Map<String, String> clusterInfo = searchClientShim.getClusterInfo();
    assertNotNull(clusterInfo);
    assertEquals(clusterInfo.get("engine_type"), "opensearch");
    String version = clusterInfo.get("version");
    assertTrue(version.startsWith("3."), "Expected version to start with 3, got: " + version);
  }

  @Test
  public void testEngineVersion() throws IOException {
    String version = searchClientShim.getEngineVersion();
    assertNotNull(version);
    assertTrue(version.startsWith("3."), "Expected version to start with 3, got: " + version);
  }

  @Test
  public void testIndexAndSearch() throws IOException {
    CreateIndexRequest createRequest = new CreateIndexRequest(TEST_INDEX);
    CreateIndexResponse createResponse =
        searchClientShim.createIndex(
            OperationFingerprint.EMPTY, createRequest, RequestOptions.DEFAULT);
    assertTrue(createResponse.isAcknowledged());

    GetIndexRequest getRequest = new GetIndexRequest(TEST_INDEX);
    assertTrue(
        searchClientShim.indexExists(
            OperationFingerprint.EMPTY, getRequest, RequestOptions.DEFAULT));

    RefreshResponse refreshResponse =
        searchClientShim.refreshIndex(
            OperationFingerprint.EMPTY, new RefreshRequest(TEST_INDEX), RequestOptions.DEFAULT);
    assertNotNull(refreshResponse);

    SearchRequest searchRequest = new SearchRequest(TEST_INDEX);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    searchRequest.source(searchSourceBuilder);

    SearchResponse searchResponse =
        searchClientShim.search(OP_CONTEXT, searchRequest, RequestOptions.DEFAULT);
    assertNotNull(searchResponse.getHits());
  }

  @Test
  public void testAutoDetection() throws IOException {
    SearchClientShim.ShimConfiguration autoConfig =
        new ShimConfigurationBuilder()
            .withHost("localhost")
            .withPort(openSearchContainer.getMappedPort(9200))
            .withSSL(false)
            .withThreadCount(1)
            .withConnectionRequestTimeout(5000)
            .build();

    try (SearchClientShim<?> autoShim =
        SearchClientShimUtil.createShimWithAutoDetection(autoConfig, new ObjectMapper())) {
      assertEquals(autoShim.getEngineType(), SearchEngineType.OPENSEARCH_3);
      assertTrue(autoShim.getEngineVersion().startsWith("3."));
    }
  }
}
