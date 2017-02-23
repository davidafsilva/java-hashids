package pt.davidafsilva.hashids;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Implementation of the <a href="http://hashids.org/">Hashids</a> protocol.
 * The algorithm has the following properties:
 * <li>
 * <li>Generation of short, unique, case-sensitive and non-sequential hashes</li>
 * <li>Hashes of natural numbers</li>
 * <li>Additional entropy through salt usage</li>
 * <li>Configurable hash size</li>
 * <li>Deterministic hash computation given the same input/parametrization</li>
 * </li>
 *
 * The underlying implementation shall be, in most cases, compatible/interchangeable with the
 * <a href="https://github.com/ivanakimov/hashids.js">standard implementation</a> provided by the
 * author of the protocol. Compatibility with other implementations is expected as long as they
 * cope with the standard implementation.
 *
 * @author david
 */
public class Hashids {

  // the default instance
  private static final class DefaultInstanceHolder {

    private static final Hashids DEFAULT_INSTANCE = newInstance(new char[0]);
  }

  // algorithm constant definitions
  private static final int LOTTERY_MOD = 100;
  private static final double GUARD_THRESHOLD = 12;
  private static final double SEPARATOR_THRESHOLD = 3.5;
  private static final int MIN_ALPHABET_LENGTH = 16;
  private static final Pattern HEX_VALUES_PATTERN = Pattern.compile("[\\w\\W]{1,12}");

  // algorithm defaults
  public static final char[] DEFAULT_ALPHABET = {
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
      'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'
  };
  private static final char[] DEFAULT_SEPARATORS = {
      'c', 'f', 'h', 'i', 's', 't', 'u', 'C', 'F', 'H', 'I', 'S', 'T', 'U'
  };

  // algorithm properties
  private final char[] alphabet;
  private final char[] separators;
  private final char[] salt;
  private final char[] guards;
  private final int minLength;

  // auxiliary structure for fast reads
  private final Set<Character> separatorsSet;

  /**
   * Creates a new instance of the algorithm with the given configuration.
   *
   * @param salt      the salt that shall be used for entropy
   * @param alphabet  the alphabet that the algorithm must follow while computing hashes
   * @param minLength the minimum length of the hashes produced by the algorithm
   */
  private Hashids(final char[] salt, final char[] alphabet, final int minLength) {
    this.minLength = minLength;
    this.salt = Arrays.copyOf(salt, salt.length);

    // filter and shuffle separators
    char[] tmpSeparators = shuffle(filterSeparators(DEFAULT_SEPARATORS, alphabet), this.salt);

    // validate and filter the alphabet
    char[] tmpAlphabet = validateAndFilterAlphabet(alphabet, tmpSeparators);

    // check separator threshold
    if (tmpSeparators.length == 0 ||
        (tmpAlphabet.length / tmpSeparators.length) > SEPARATOR_THRESHOLD) {
      final int minSeparatorsSize = (int) Math.ceil(tmpAlphabet.length / SEPARATOR_THRESHOLD);
      // check minimum size of separators
      if (minSeparatorsSize > tmpSeparators.length) {
        // fill separators from alphabet
        final int missingSeparators = minSeparatorsSize - tmpSeparators.length;
        tmpSeparators = Arrays.copyOf(tmpSeparators, tmpSeparators.length + missingSeparators);
        System.arraycopy(tmpAlphabet, 0, tmpSeparators,
            tmpSeparators.length - missingSeparators, missingSeparators);
        System.arraycopy(tmpAlphabet, 0, tmpSeparators,
            tmpSeparators.length - missingSeparators, missingSeparators);
        tmpAlphabet = Arrays.copyOfRange(tmpAlphabet, missingSeparators, tmpAlphabet.length);
      }
    }

    // shuffle the current alphabet
    shuffle(tmpAlphabet, this.salt);

    // check guards
    this.guards = new char[(int) Math.ceil(tmpAlphabet.length / GUARD_THRESHOLD)];
    if (alphabet.length < 3) {
      System.arraycopy(tmpSeparators, 0, guards, 0, guards.length);
      this.separators = Arrays.copyOfRange(tmpSeparators, guards.length, tmpSeparators.length);
      this.alphabet = tmpAlphabet;
    } else {
      System.arraycopy(tmpAlphabet, 0, guards, 0, guards.length);
      this.separators = tmpSeparators;
      this.alphabet = Arrays.copyOfRange(tmpAlphabet, guards.length, tmpAlphabet.length);
    }

    // create the separators set
    separatorsSet = IntStream.range(0, separators.length)
        .mapToObj(idx -> separators[idx])
        .collect(Collectors.toSet());
  }

  //-------------------------
  // Static factory methods
  //-------------------------

  /**
   * Returns the default instance of the algorithm which uses the following parameters:
   * <table summary="Algorithm parametrization">
   * <tr>
   * <th>Parameter</th>
   * <th>Value</th>
   * </tr>
   * <tr>
   * <td><b>salt</b></td>
   * <td>--</td>
   * </tr>
   * <tr>
   * <td><b>alphabet</b></td>
   * <td>abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890</td>
   * </tr>
   * <tr>
   * <td><b>minLength</b></td>
   * <td>--</td>
   * </tr>
   * </table>
   *
   * @return the default algorithm instance
   */
  public static Hashids getInstance() {
    return DefaultInstanceHolder.DEFAULT_INSTANCE;
  }

  /**
   * Returns a new instance of the algorithm with the given salt and the {@link #DEFAULT_ALPHABET
   * default alphabet} with no minimum hash length.
   *
   * @param salt the salt to be used as entropy for the algorithm
   * @return a new instance of the algorithm
   */
  public static Hashids newInstance(final String salt) {
    return newInstance(salt.toCharArray(), DEFAULT_ALPHABET, -1);
  }

  /**
   * Returns a new instance of the algorithm with the given salt and the {@link #DEFAULT_ALPHABET
   * default alphabet} with no minimum hash length.
   *
   * @param salt the salt to be used as entropy for the algorithm
   * @return a new instance of the algorithm
   */
  public static Hashids newInstance(final char[] salt) {
    return newInstance(salt, DEFAULT_ALPHABET, -1);
  }

  /**
   * Returns a new instance of the algorithm with the given salt and the alphabet and no minimum
   * hash length.
   *
   * @param salt     the salt to be used as entropy for the algorithm
   * @param alphabet the alphabet to be used for the hash generation
   * @return a new instance of the algorithm
   */
  public static Hashids newInstance(final String salt, final String alphabet) {
    return newInstance(salt.toCharArray(), alphabet.toCharArray(), -1);
  }

  /**
   * Returns a new instance of the algorithm with the given salt and the alphabet and no minimum
   * hash length.
   *
   * @param salt     the salt to be used as entropy for the algorithm
   * @param alphabet the alphabet to be used for the hash generation
   * @return a new instance of the algorithm
   */
  public static Hashids newInstance(final char[] salt, final char[] alphabet) {
    return newInstance(salt, alphabet, -1);
  }

  /**
   * Returns a new instance of the algorithm with the given salt and the {@link #DEFAULT_ALPHABET
   * default alphabet} with {@code minLength} as the minimum hash length.
   *
   * @param salt      the salt to be used as entropy for the algorithm
   * @param minLength the minimum hash length
   * @return a new instance of the algorithm
   */
  public static Hashids newInstance(final String salt, final int minLength) {
    return newInstance(salt.toCharArray(), DEFAULT_ALPHABET, minLength);
  }

  /**
   * Returns a new instance of the algorithm with the given salt and the {@link #DEFAULT_ALPHABET
   * default alphabet} with {@code minLength} as the minimum hash length.
   *
   * @param salt      the salt to be used as entropy for the algorithm
   * @param minLength the minimum hash length
   * @return a new instance of the algorithm
   */
  public static Hashids newInstance(final char[] salt, final int minLength) {
    return newInstance(salt, DEFAULT_ALPHABET, minLength);
  }

  /**
   * Returns a new instance of the algorithm with the given salt and the alphabet and
   * {@code minLength} as the minimum hash length.
   *
   * @param salt      the salt to be used as entropy for the algorithm
   * @param alphabet  the alphabet to be used for the hash generation
   * @param minLength the minimum hash length
   * @return a new instance of the algorithm
   */
  public static Hashids newInstance(final String salt, final String alphabet, final int minLength) {
    return newInstance(salt.toCharArray(), alphabet.toCharArray(), minLength);
  }

  /**
   * Returns a new instance of the algorithm with the given salt and the alphabet and
   * {@code minLength} as the minimum hash length.
   *
   * @param salt      the salt to be used as entropy for the algorithm
   * @param alphabet  the alphabet to be used for the hash generation
   * @param minLength the minimum hash length
   * @return a new instance of the algorithm
   */
  public static Hashids newInstance(final char[] salt, final char[] alphabet, final int minLength) {
    return new Hashids(salt, alphabet, minLength);
  }

  //-------------------------
  // Encode
  //-------------------------


  /**
   * Encodes the given numbers in hexadecimal format based on this instance configuration.
   *
   * @param hexNumbers the numbers in hex to be encoded
   * @return the resultant hash of the encoding of {@code hexNumbers}, {@code null} if {@code
   * numbers}
   * is {@code null}.
   * @throws IllegalArgumentException if any of the numbers is not supported
   */
  public String encodeHex(final String hexNumbers) {
    if (hexNumbers == null) {
      return null;
    }

    // remove the prefix, if present
    final String hex = hexNumbers.startsWith("0x") || hexNumbers.startsWith("0X") ?
        hexNumbers.substring(2) : hexNumbers;

    // get the associated long value and encode it
    LongStream values = LongStream.empty();
    final Matcher matcher = HEX_VALUES_PATTERN.matcher(hex);
    while (matcher.find()) {
      final long value = new BigInteger("1" + matcher.group(), 16).longValue();
      values = LongStream.concat(values, LongStream.of(value));
    }

    return encode(values.toArray());
  }

  /**
   * Encodes the given {@code numbers} based on this instance configuration.
   *
   * @param numbers the numbers to be encoded
   * @return the resultant hash of the encoding of {@code numbers}, {@code null} if {@code numbers}
   * is {@code null}.
   * @throws IllegalArgumentException if any of the numbers is not supported
   */
  public String encode(final long... numbers) {
    if (numbers == null) {
      return null;
    }

    // copy alphabet
    final char[] currentAlphabet = Arrays.copyOf(alphabet, alphabet.length);

    // determine the lottery number
    final long lotteryId = LongStream.range(0, numbers.length)
        .reduce(0, (state, i) -> {
          final long number = numbers[(int) i];
          if (number < 0) {
            throw new IllegalArgumentException("invalid number: " + number);
          }
          return state + number % (i + LOTTERY_MOD);
        });
    final char lottery = currentAlphabet[(int) (lotteryId % currentAlphabet.length)];

    // encode each number
    final StringBuilder global = new StringBuilder();
    IntStream.range(0, numbers.length)
        .forEach(idx -> {
          // derive alphabet
          deriveNewAlphabet(currentAlphabet, salt, lottery);

          // encode
          final int initialLength = global.length();
          translate(numbers[idx], currentAlphabet, global, initialLength);

          // prepend the lottery
          if (idx == 0) {
            global.insert(0, lottery);
          }

          // append the separator, if more numbers are pending encoding
          if (idx + 1 < numbers.length) {
            long n = numbers[idx] % (global.charAt(initialLength) + 1);
            global.append(separators[(int) (n % separators.length)]);
          }
        });

    // add the guards, if there's any space left
    if (minLength > global.length()) {
      int guardIdx = (int) ((lotteryId + lottery) % guards.length);
      global.insert(0, guards[guardIdx]);
      if (minLength > global.length()) {
        guardIdx = (int) ((lotteryId + global.charAt(2)) % guards.length);
        global.append(guards[guardIdx]);
      }
    }

    // add the necessary padding
    int paddingLeft = minLength - global.length();
    while (paddingLeft > 0) {
      shuffle(currentAlphabet, Arrays.copyOf(currentAlphabet, currentAlphabet.length));

      final int alphabetHalfSize = currentAlphabet.length / 2;
      final int initialSize = global.length();
      if (paddingLeft > currentAlphabet.length) {
        // entire alphabet with the current encoding in the middle of it
        int offset = alphabetHalfSize + (currentAlphabet.length % 2 == 0 ? 0 : 1);

        global.insert(0, currentAlphabet, alphabetHalfSize, offset);
        global.insert(offset + initialSize, currentAlphabet, 0, alphabetHalfSize);
        // decrease the padding left
        paddingLeft -= currentAlphabet.length;
      } else {
        // calculate the excess
        final int excess = currentAlphabet.length + global.length() - minLength;
        final int secondHalfStartOffset = alphabetHalfSize + Math.floorDiv(excess, 2);
        final int secondHalfLength = currentAlphabet.length - secondHalfStartOffset;
        final int firstHalfLength = paddingLeft - secondHalfLength;

        global.insert(0, currentAlphabet, secondHalfStartOffset, secondHalfLength);
        global.insert(secondHalfLength + initialSize, currentAlphabet, 0, firstHalfLength);

        paddingLeft = 0;
      }
    }

    return global.toString();
  }

  //-------------------------
  // Decode
  //-------------------------

  /**
   * Decodes the given {@code hash} into his original hexadecimal representation based on this
   * instance configuration.
   *
   * @param hash the hash to be decoded
   * @return the original hexadecimal representation of the hash values with each numeric number
   * present in the hash, {@code null} if {@code numbers} is {@code null}.
   * @throws IllegalArgumentException if the hash is invalid.
   */
  public String decodeHex(final String hash) {
    if (hash == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();
    Arrays.stream(decode(hash))
        .mapToObj(Long::toHexString)
        .forEach(hex -> sb.append(hex, 1, hex.length()));
    return sb.toString();
  }

  /**
   * Decodes the given {@code hash} into his original numeric representation based on this instance
   * configuration.
   *
   * @param hash the hash to be decoded
   * @return an array of long values with each numeric number present in the hash, {@code null} if
   * {@code numbers} is {@code null}.
   * @throws IllegalArgumentException if the hash is invalid.
   */
  public long[] decode(final String hash) {
    if (hash == null) {
      return null;
    }

    // create a set of the guards
    final Set<Character> guardsSet = IntStream.range(0, guards.length)
        .mapToObj(idx -> guards[idx])
        .collect(Collectors.toSet());
    // count the total guards used
    final int[] guardsIdx = IntStream.range(0, hash.length())
        .filter(idx -> guardsSet.contains(hash.charAt(idx)))
        .toArray();
    // get the start/end index base on the guards count
    final int startIdx, endIdx;
    if (guardsIdx.length > 0) {
      startIdx = guardsIdx[0] + 1;
      endIdx = guardsIdx.length > 1 ? guardsIdx[1] : hash.length();
    } else {
      startIdx = 0;
      endIdx = hash.length();
    }

    LongStream decoded = LongStream.empty();
    // parse the hash
    if (hash.length() > 0) {
      final char lottery = hash.charAt(startIdx);

      // create the initial accumulation string
      final int length = hash.length() - guardsIdx.length - 1;
      StringBuilder block = new StringBuilder(length);

      // create the base salt
      final char[] decodeSalt = new char[alphabet.length];
      decodeSalt[0] = lottery;
      final int saltLength = salt.length >= alphabet.length ? alphabet.length - 1 : salt.length;
      System.arraycopy(salt, 0, decodeSalt, 1, saltLength);
      final int saltLeft = alphabet.length - saltLength - 1;

      // copy alphabet
      final char[] currentAlphabet = Arrays.copyOf(alphabet, alphabet.length);

      for (int i = startIdx + 1; i < endIdx; i++) {
        if (!separatorsSet.contains(hash.charAt(i))) {
          block.append(hash.charAt(i));
          // continue if we have not reached the end, yet
          if (i < endIdx - 1) {
            continue;
          }
        }

        if (block.length() > 0) {
          // create the salt
          if (saltLeft > 0) {
            System.arraycopy(currentAlphabet, 0, decodeSalt,
                alphabet.length - saltLeft, saltLeft);
          }

          // shuffle the alphabet
          shuffle(currentAlphabet, decodeSalt);

          // prepend the decoded value
          final long n = translate(block.toString().toCharArray(), currentAlphabet);
          decoded = LongStream.concat(decoded, LongStream.of(n));

          // create a new block
          block = new StringBuilder(length);
        }
      }
    }

    // validate the hash
    final long[] decodedValue = decoded.toArray();
    if (!Objects.equals(hash, encode(decodedValue))) {
      throw new IllegalArgumentException("invalid hash: " + hash);
    }

    return decodedValue;
  }

  // -------------------
  // Utility functions
  // -------------------

  private StringBuilder translate(final long n, final char[] alphabet,
      final StringBuilder sb, final int start) {
    long input = n;
    do {
      // prepend the chosen char
      sb.insert(start, alphabet[(int) (input % alphabet.length)]);

      // trim the input
      input = input / alphabet.length;
    } while (input > 0);

    return sb;
  }

  private long translate(final char[] hash, final char[] alphabet) {
    long number = 0;

    final Map<Character, Integer> alphabetMapping = IntStream.range(0, alphabet.length)
        .mapToObj(idx -> new Object[]{alphabet[idx], idx})
        .collect(Collectors.groupingBy(arr -> (Character) arr[0],
            Collectors.mapping(arr -> (Integer) arr[1],
                Collectors.reducing(null, (a, b) -> a == null ? b : a))));

    for (int i = 0; i < hash.length; ++i) {
      number += alphabetMapping.get(hash[i]) *
          (long) Math.pow(alphabet.length, hash.length - i - 1);
    }

    return number;
  }

  private char[] deriveNewAlphabet(final char[] alphabet, final char[] salt, final char lottery) {
    // create the new salt
    final char[] newSalt = new char[alphabet.length];

    // 1. lottery
    newSalt[0] = lottery;
    int spaceLeft = newSalt.length - 1;
    int offset = 1;
    // 2. salt
    if (salt.length > 0 && spaceLeft > 0) {
      int length = salt.length > spaceLeft ? spaceLeft : salt.length;
      System.arraycopy(salt, 0, newSalt, offset, length);
      spaceLeft -= length;
      offset += length;
    }
    // 3. alphabet
    if (spaceLeft > 0) {
      System.arraycopy(alphabet, 0, newSalt, offset, spaceLeft);
    }

    // shuffle
    return shuffle(alphabet, newSalt);
  }

  private char[] validateAndFilterAlphabet(final char[] alphabet, final char[] separators) {
    // validate size
    if (alphabet.length < MIN_ALPHABET_LENGTH) {
      throw new IllegalArgumentException(String.format("alphabet must contain at least %d unique " +
          "characters: %d", MIN_ALPHABET_LENGTH, alphabet.length));
    }

    final Set<Character> seen = new LinkedHashSet<>(alphabet.length);
    final Set<Character> invalid = IntStream.range(0, separators.length)
        .mapToObj(idx -> separators[idx])
        .collect(Collectors.toSet());

    // add to seen set (without duplicates)
    IntStream.range(0, alphabet.length)
        .forEach(i -> {
          if (alphabet[i] == ' ') {
            throw new IllegalArgumentException(String.format("alphabet must not contain spaces: " +
                "index %d", i));
          }
          final Character c = alphabet[i];
          if (!invalid.contains(c)) {
            seen.add(c);
          }
        });

    // check if got the same untouched alphabet
    if (seen.size() == alphabet.length) {
      return Arrays.copyOf(alphabet, alphabet.length);
    }

    // create a new alphabet without the duplicates
    final char[] uniqueAlphabet = new char[seen.size()];
    int idx = 0;
    for (char c : seen) {
      uniqueAlphabet[idx++] = c;
    }
    return uniqueAlphabet;
  }

  private char[] filterSeparators(final char[] separators, final char[] alphabet) {
    final Set<Character> valid = IntStream.range(0, alphabet.length)
        .mapToObj(idx -> alphabet[idx])
        .collect(Collectors.toSet());

    return IntStream.range(0, separators.length)
        .mapToObj(idx -> (separators[idx]))
        .filter(valid::contains)
        // ugly way to convert back to char[]
        .map(c -> Character.toString(c))
        .collect(Collectors.joining())
        .toCharArray();
  }

  private char[] shuffle(final char[] alphabet, final char[] salt) {
    for (int i = alphabet.length - 1, v = 0, p = 0, j, z; salt.length > 0 && i > 0; i--, v++) {
      v %= salt.length;
      p += z = salt[v];
      j = (z + v + p) % i;
      final char tmp = alphabet[j];
      alphabet[j] = alphabet[i];
      alphabet[i] = tmp;
    }
    return alphabet;
  }
}
