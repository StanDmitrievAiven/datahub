package com.linkedin.metadata.search.elasticsearch.client.shim.builder.opensearch3;

import static org.testng.Assert.*;

import com.linkedin.metadata.utils.elasticsearch.shim.KnnSearchRequest;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

public class OpenSearch3KnnQueryBuilderTest {

  private static final String VECTOR_FIELD = "embeddings.demo.chunks.vector";
  private static final String NESTED_PATH = "embeddings.demo.chunks";

  @Test
  public void testUnfilteredUsesNestedKnn() {
    KnnSearchRequest req =
        KnnSearchRequest.builder()
            .indexName("idx")
            .vectorField(VECTOR_FIELD)
            .queryVector(new float[] {1.0f, 0.0f})
            .k(2)
            .build();

    Map<String, Object> body = OpenSearch3KnnQueryBuilder.build(req);
    @SuppressWarnings("unchecked")
    Map<String, Object> query = (Map<String, Object>) body.get("query");
    assertTrue(query.containsKey("nested"));
    assertFalse(query.containsKey("bool"));
  }

  @Test
  public void testFilterUsesBoolMustNestedAndFilter() {
    Map<String, Object> termFilter = Map.of("term", Map.of("tag", "keep"));
    KnnSearchRequest req =
        KnnSearchRequest.builder()
            .indexName("idx")
            .vectorField(VECTOR_FIELD)
            .queryVector(new float[] {1.0f, 0.0f})
            .k(2)
            .filter(termFilter)
            .build();

    Map<String, Object> body = OpenSearch3KnnQueryBuilder.build(req);
    @SuppressWarnings("unchecked")
    Map<String, Object> query = (Map<String, Object>) body.get("query");
    assertTrue(query.containsKey("bool"), "OS3 should wrap filtered knn in bool (spike D1)");

    @SuppressWarnings("unchecked")
    Map<String, Object> bool = (Map<String, Object>) query.get("bool");
    assertTrue(bool.containsKey("must"));
    assertTrue(bool.containsKey("filter"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> must = (List<Map<String, Object>>) bool.get("must");
    assertEquals(must.size(), 1);
    assertTrue(must.get(0).containsKey("nested"));

    @SuppressWarnings("unchecked")
    Map<String, Object> nested = (Map<String, Object>) must.get(0).get("nested");
    assertEquals(nested.get("path"), NESTED_PATH);

    @SuppressWarnings("unchecked")
    Map<String, Object> innerQuery = (Map<String, Object>) nested.get("query");
    @SuppressWarnings("unchecked")
    Map<String, Object> knn = (Map<String, Object>) innerQuery.get("knn");
    @SuppressWarnings("unchecked")
    Map<String, Object> knnParams = (Map<String, Object>) knn.get(VECTOR_FIELD);
    assertFalse(
        knnParams.containsKey("filter"), "filter must not be inside knn block on OS3 (spike D1)");
  }
}
