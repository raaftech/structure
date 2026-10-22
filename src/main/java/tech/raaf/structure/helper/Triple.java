package tech.raaf.structure.helper;

import java.util.Collection;
import java.util.stream.Stream;
import tech.raaf.structure.graph.Entity;

public class Triple<L, M, R> {

  private final L left;
  private final M middle;
  private final R right;

  public Triple(L left, M middle, R right) {
    if (left == null || middle == null || right == null) {
      throw new IllegalArgumentException("Null value passed");
    }
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  @SuppressWarnings("unchecked")
  public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
    return new Triple(left, middle, right);
  }

  public L getLeft() {
    return this.left;
  }

  public M getMiddle() {
    return this.middle;
  }

  public R getRight() {
    return this.right;
  }

  /**
   * If the {@link Triple#right right} field of this triple is a {@link Collection} so that the type of this {@link
   * Triple} is `Triple&lt;L, M, Collection&lt;?&gt;&gt;` return a {@link Stream} containing an element of type
   * `Triple&lt;L, M, {@link T T}&gt;` for every element in {@link Triple#right}.
   * {@link Triple Triple} objects are often used to
   * express relationships between {@link Entity} instances. The usual way to do this is
   * to have a triple where the right side is a set of UUIDs. In some cases it may be convenient to represent all
   * these relationships as separate Triple objects. Flatten does that.
   *
   * @param <T> The type of every {@link Triple#right right} field of every {@link Triple Triple} in the resulting
   *            {@link Stream}.
   * @return If {@link Triple#right right} is a {@link Collection} then return a stream containing a triple for every
   * element of {@link Triple#right right}. If {@link Triple#right right} is not a {@link Collection} then return a
   * stream containing only this element, but with {@link Triple#right right} cast to {@link T}.
   */
  public <T> Stream<Triple<L, M, T>> flatten() {
    if (right instanceof Collection<?>) {
      return ((Collection<T>) right).stream().map(el -> Triple.of(left, middle, el));
    } else {
      return Stream.of(Triple.of(left, middle, (T) right));
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (left == null ? 0 : left.hashCode());
    result = prime * result + (middle == null ? 0 : middle.hashCode());
    result = prime * result + (right == null ? 0 : right.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof Triple)) return false;
    Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
    if (left == null) {
      if (other.left != null) return false;
    } else if (!left.equals(other.left)) return false;
    if (middle == null) {
      if (other.middle != null) return false;
    } else if (!middle.equals(other.middle)) return false;
    if (right == null) {
      if (other.right != null) return false;
    } else if (!right.equals(other.right)) return false;
    return true;
  }
}
