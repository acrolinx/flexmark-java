package com.vladsch.flexmark.util.collection;

import static org.junit.Assert.assertEquals;

import java.util.function.BiFunction;
import org.junit.Test;

public class MaxAggregatorTest {
  private static Integer reduce(
      BiFunction<Integer, Integer, Integer> aggregator, Integer... items) {
    Integer aggregate = null;
    for (Integer item : items) {
      aggregate = aggregator.apply(aggregate, item);
    }
    return aggregate;
  }

  @Test
  public void test_Basic() {
    assertEquals((Integer) null, reduce(MinAggregator.INSTANCE));
    assertEquals((Integer) null, reduce(MinAggregator.INSTANCE, (Integer) null));
    assertEquals(-5, (int) reduce(MinAggregator.INSTANCE, -1, -2, -5, 0, 1));
    assertEquals(-5, (int) reduce(MinAggregator.INSTANCE, -1, -2, -5, 0, 1, 5));
  }
}
