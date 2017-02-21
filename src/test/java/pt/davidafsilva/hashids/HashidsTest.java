package pt.davidafsilva.hashids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author david
 */
@RunWith(Parameterized.class)
public class HashidsTest extends AbstractHashidsTest {

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    // generate a 1-to-32 sized arrays
    final List<int[]> input = new ArrayList<>();
    for (int i = 0; i < 32; i++) {
      if (input.isEmpty()) {
        input.add(new int[]{i});
      } else {
        final int[] prev = input.get(input.size() - 1);
        final int[] curr = Arrays.copyOf(prev, prev.length + 1);
        curr[prev.length] = i;
        input.add(curr);
      }
    }

    // generate a supplier for all algorithm types
    final List<Supplier<?>[]> algorithms = new ArrayList<>();
    // no salt, no custom alphabet, no minLength
    algorithms.add(new Supplier<?>[]{
        () -> "NoSalt|NoAlphabet|NoMinLength",
        Hashids::getInstance,
        () -> getJsAlgorithm(null, null, 0)
    });
    // with salt, no custom alphabet, no minLength
    algorithms.add(new Supplier<?>[]{
        () -> "Salt|NoAlphabet|NoMinLength",
        () -> Hashids.newInstance("my awesome salt"),
        () -> getJsAlgorithm("my awesome salt", null, 0)
    });
    // no salt, with custom alphabet, no minLength
    algorithms.add(new Supplier<?>[]{
        () -> "NoSalt|Alphabet|NoMinLength",
        () -> Hashids.newInstance("", "1234567890abcdef"),
        () -> getJsAlgorithm(null, "1234567890abcdef", 0)
    });
    // no salt, no custom alphabet, with minLength
    algorithms.add(new Supplier<?>[]{
        () -> "NoSalt|NoAlphabet|MinLength",
        () -> Hashids.newInstance(new char[0], Hashids.DEFAULT_ALPHABET, 32),
        () -> getJsAlgorithm(null, null, 32)
    });
    // with salt, with custom alphabet, no minLength
    algorithms.add(new Supplier<?>[]{
        () -> "Salt|Alphabet|NoMinLength",
        () -> Hashids.newInstance("my awesome salt", "1234567890abcdef"),
        () -> getJsAlgorithm("my awesome salt", "1234567890abcdef", 0)
    });
    // no salt, with custom alphabet, with minLength
    algorithms.add(new Supplier<?>[]{
        () -> "NoSalt|Alphabet|MinLength",
        () -> Hashids.newInstance("", "1234567890abcdef", 32),
        () -> getJsAlgorithm(null, "1234567890abcdef", 32)
    });
    // with salt, no custom alphabet, with minLength
    algorithms.add(new Supplier<?>[]{
        () -> "Salt|NoAlphabet|MinLength",
        () -> Hashids.newInstance("my awesome salt".toCharArray(), Hashids.DEFAULT_ALPHABET, 32),
        () -> getJsAlgorithm("my awesome salt", null, 32)
    });
    // with salt, with custom alphabet, with minLength
    algorithms.add(new Supplier<?>[]{
        () -> "Salt|Alphabet|MinLength",
        () -> Hashids.newInstance("my awesome salt", "1234567890abcdef", 32),
        () -> getJsAlgorithm("my awesome salt", "1234567890abcdef", 32)
    });

    // generate the test input for each hash algorithm and test input
    return algorithms.stream()
        .flatMap(ha -> input.stream().map(ti -> new Object[]{
            // name
            String.format("algorithm(%s) with input(%s)", ha[0].get(), Arrays.toString(ti)),
            // algorithm, jsAlgorithm, input
            ha[1], ha[2], ti
        }))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unused")
  public HashidsTest(final String name, final Supplier<Hashids> algorithm,
      final Supplier<JsHashids> jsAlgorithm, final long[] input) {
    super(algorithm, jsAlgorithm, input);
  }

  @Test
  public void runTest() {
    final String encoded = algorithm.encode(input);
    assertThat("encoding mismatch", encoded, equalTo(jsAlgorithm.encode(input)));
    final long[] original = algorithm.decode(encoded);
    assertThat(original, equalTo(input));
  }
}
