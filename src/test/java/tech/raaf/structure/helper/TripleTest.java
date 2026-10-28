package tech.raaf.structure.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class TripleTest {

  static final String left = "left";
  static final String middle = "middle";
  static final String rightPrefix = "el";

  @Test
  public void flattenTripleWithCollection() throws Exception {
    /*
      Create a Triple containing a set of three strings as the right element. Immediately
      call flatten on this.
    */
    Stream<Triple<String, String, List<String>>> flattenedTripleStream = Triple.of(
      left,
      middle,
      Arrays.asList(rightPrefix + 1, rightPrefix + 2, rightPrefix + 3)
    ).flatten();
    // Collect the flattened stream to a list.
    List<Triple<String, String, List<String>>> flattenedTripleList = flattenedTripleStream.collect(Collectors.toList());
    /*
      Because the original triple's right element was a set of three strings, the
      resulting list should contain three triples.
    */
    assertEquals(3, flattenedTripleList.size());
    /*
      Make sure these triples equal to the left and right fields of the original triple
      and the elements of the original set as right fields.
    */
    IntStream.range(1, 4).forEach(i ->
      assertEquals(Triple.of(left, middle, rightPrefix + i), flattenedTripleList.get(i - 1))
    );
  }

  @Test
  /**
   * This test is the same as above, but now called on a Triple that does not have
   * a Collection as right field. This should simple return a stream of one triple with
   * the original right field cast to type parameter `T`, see {@link Triple#flatten()}.
   */
  public void flattenSimpleTriple() throws Exception {
    Triple<String, String, String> triple = Triple.of(left, middle, rightPrefix);
    Stream<Triple<String, String, String>> flattenedTripleStream = triple.flatten();
    List<Triple<String, String, String>> flattenedTripleList = flattenedTripleStream.collect(Collectors.toList());
    assertEquals(1, flattenedTripleList.size());
    assertEquals(triple, flattenedTripleList.get(0));
  }
}
