package com.linkedin.metadata.search.elasticsearch.client.shim.builder.opensearch3;

import com.linkedin.metadata.utils.elasticsearch.shim.KnnSearchRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Builds OpenSearch 3.x kNN query JSON.
 *
 * <p>Spike D1: placing {@code filter} inside the {@code knn} block (OS2 style) returned 0 hits on
 * OpenSearch 3.6/3.7. This builder keeps nested knn scoring and applies filters via a wrapping
 * {@code bool} query instead.
 */
public final class OpenSearch3KnnQueryBuilder {

  private OpenSearch3KnnQueryBuilder() {}

  @Nonnull
  public static Map<String, Object> build(@Nonnull KnnSearchRequest req) {
    String nestedPath = deriveNestedPath(req.vectorField());

    Map<String, Object> knnParams = new HashMap<>();
    knnParams.put("vector", toFloatList(req.queryVector()));
    knnParams.put("k", req.k());
    if (req.numCandidates() > req.k()) {
      knnParams.put("method_parameters", Map.of("ef_search", req.numCandidates()));
    }

    Map<String, Object> nestedKnn =
        Map.of(
            "nested",
            Map.of(
                "path",
                nestedPath,
                "score_mode",
                "max",
                "query",
                Map.of("knn", Map.of(req.vectorField(), knnParams))));

    Map<String, Object> query;
    if (req.filter().isPresent()) {
      List<Map<String, Object>> must = new ArrayList<>();
      must.add(nestedKnn);
      List<Object> filter = new ArrayList<>();
      filter.add(req.filter().get());
      query = Map.of("bool", Map.of("must", must, "filter", filter));
    } else {
      query = nestedKnn;
    }

    Map<String, Object> body = new HashMap<>();
    body.put("size", req.k());
    body.put("track_total_hits", false);
    if (!req.fieldsToFetch().isEmpty()) {
      body.put("_source", req.fieldsToFetch().toArray(new String[0]));
    }
    body.put("query", query);
    return body;
  }

  @Nonnull
  private static List<Float> toFloatList(float[] vec) {
    List<Float> out = new ArrayList<>(vec.length);
    for (float v : vec) {
      out.add(v);
    }
    return out;
  }

  @Nonnull
  private static String deriveNestedPath(@Nonnull String vectorField) {
    if (!vectorField.endsWith(".vector")) {
      throw new IllegalArgumentException(
          "Expected vectorField to end with .vector; got: " + vectorField);
    }
    String prefix = vectorField.substring(0, vectorField.length() - ".vector".length());
    if (prefix.isEmpty()) {
      throw new IllegalArgumentException(
          "Expected vectorField to have a non-empty nested path prefix; got: " + vectorField);
    }
    return prefix;
  }
}
