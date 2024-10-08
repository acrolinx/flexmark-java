package com.vladsch.flexmark.util.collection;

import static org.junit.Assert.assertEquals;

import java.util.function.BiFunction;
import org.junit.Test;

public class MinAggregatorTest {
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
    assertEquals((Integer) null, reduce(MaxAggregator.INSTANCE));
    assertEquals((Integer) null, reduce(MaxAggregator.INSTANCE, (Integer) null));
    assertEquals((Integer) 1, reduce(MaxAggregator.INSTANCE, -1, -2, -5, 0, 1));
    assertEquals((Integer) 5, reduce(MaxAggregator.INSTANCE, -1, -2, -5, 0, 1, 5));
  }
}
