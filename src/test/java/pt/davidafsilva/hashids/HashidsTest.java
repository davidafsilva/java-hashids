package pt.davidafsilva.hashids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author david
 */
@RunWith(Parameterized.class)
public class HashidsTest extends AbstractHashidsTest {

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {
            (Supplier<Hashids>) Hashids::getInstance,
            (Supplier<JsHashids>) () -> getJsAlgorithm("", "", 0),
            new long[]{1, 2, 3, 4, 5}
        },
    });
  }

  public HashidsTest(final Supplier<Hashids> algorithm, final Supplier<JsHashids> jsAlgorithm,
      final long[] input) {
    super(algorithm, jsAlgorithm, input);
  }

  @Test
  public void defaultInstance_encodeDecode() {
    final String encoded = algorithm.encode(input);
    assertThat("encoding mismatch", encoded, equalTo(jsAlgorithm.encode(input)));
    final long[] original = algorithm.decode(encoded);
    assertThat(original, equalTo(input));
  }
}
