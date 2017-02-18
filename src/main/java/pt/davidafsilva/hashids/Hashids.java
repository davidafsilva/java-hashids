package pt.davidafsilva.hashids;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * TODO: change me
 *
 * @author david
 */
public class Hashids {

  private static final char[] DEFAULT_ALPHABET = {
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
      'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'
  };
  private static final char[] DEFAULT_SEPARATORS = {
      'c', 'f', 'h', 'i', 's', 't', 'u', 'C', 'F', 'H', 'I', 'S', 'T', 'U'
  };
  private static final int LOTTERY_MOD = 100;
  private static final double GUARD_THRESHOLD = 12;
  private static final double SEPARATOR_THRESHOLD = 3.5;
  private static final int MIN_ALPHABET_LENGTH = 16;

  private final char[] alphabet;
  private final char[] separators;
  private final char[] salt;
  private final char[] guards;
  private final int minLength;

  private Hashids(final char[] salt, final char[] alphabet, final int minLength) {
    this.minLength = minLength;
    this.salt = salt;

    // filter and shuffle separators
    char[] tmpSeparators = shuffle(filterSeparators(DEFAULT_SEPARATORS, alphabet), salt);

    // validate and filter the alphabet
    char[] tmpAlphabet = validateAndFilterAlphabet(alphabet, tmpSeparators);

    // check separator threshold
    if (tmpSeparators.length == 0 || (tmpAlphabet.length/tmpSeparators.length) > SEPARATOR_THRESHOLD) {
      final int minSeparatorsSize = (int) Math.ceil(tmpAlphabet.length / SEPARATOR_THRESHOLD);
      // check minimum size of separators
      if (minSeparatorsSize > tmpSeparators.length) {
        // fill separators from alphabet
        final int missingSeparators = minSeparatorsSize - tmpSeparators.length;
        tmpSeparators = Arrays.copyOf(tmpSeparators, tmpSeparators.length + missingSeparators);
        System.arraycopy(tmpAlphabet, 0, tmpSeparators, tmpSeparators.length-missingSeparators, missingSeparators);
        System.arraycopy(tmpAlphabet, 0, tmpSeparators, tmpSeparators.length-missingSeparators, missingSeparators);
        tmpAlphabet = Arrays.copyOfRange(tmpAlphabet, missingSeparators, tmpAlphabet.length);
      }
    }

    // shuffle the current alphabet
    tmpAlphabet = shuffle(tmpAlphabet, salt);

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

    System.out.println("alphabet: " + String.valueOf(this.alphabet));
    System.out.println("separators: " + String.valueOf(this.separators));
    System.out.println("guards: " + String.valueOf(this.guards));
  }

  public static Hashids of(String salt) {
    return of(salt.toCharArray(), DEFAULT_ALPHABET, -1);
  }

  public static Hashids of(char[] salt) {
    return of(salt, DEFAULT_ALPHABET, -1);
  }

  public static Hashids of(String salt, String alphabet) {
    return of(salt.toCharArray(), alphabet.toCharArray(), -1);
  }

  public static Hashids of(char[] salt, char[] alphabet) {
    return of(salt, alphabet, -1);
  }

  public static Hashids of(String salt, int minLength) {
    return of(salt.toCharArray(), DEFAULT_ALPHABET, minLength);
  }

  public static Hashids of(char[] salt, int minLength) {
    return of(salt, DEFAULT_ALPHABET, minLength);
  }

  public static Hashids of(String salt, String alphabet, int minLength) {
    return of(salt.toCharArray(), alphabet.toCharArray(), minLength);
  }

  public static Hashids of(char[] salt, char[] alphabet, int minLength) {
    return new Hashids(salt, alphabet, minLength);
  }

  //-------------------------
  // Encode
  //-------------------------

  public String encode(final long... numbers) {
    if (numbers == null) return null;

    // copy alphabet
    final char[] currentAlphabet = Arrays.copyOf(alphabet, alphabet.length);

    // determine the lottery number
    final long lotteryId = LongStream.range(0, numbers.length)
        .reduce(0, (state, i) -> state + numbers[(int)i] % (i + LOTTERY_MOD));
    final char lottery = currentAlphabet[(int) (lotteryId % currentAlphabet.length)];

    // encode each number
    final StringBuilder global = new StringBuilder();
    IntStream.range(0, numbers.length)
        .forEach(idx -> {
          // derive alphabet
          deriveNewAlphabet(currentAlphabet, salt, lottery);

          // encode
          final int initialLength = global.length();
          translate(numbers[idx], lottery, currentAlphabet, global, initialLength);

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
        global.insert(0, guards[guardIdx]);
      }
    }

    // add the necessary padding
    int paddingLeft = minLength - global.length();
    while (paddingLeft > 0) {
      final char[] paddingAlphabet = shuffle(alphabet, alphabet);
      final int alphabetHalfSize = paddingAlphabet.length / 2;
      final int initialSize = global.length();
      if (paddingLeft > paddingAlphabet.length) {
        // entire alphabet with the current encoding in the middle of it
        int offset = alphabetHalfSize + paddingAlphabet.length % 2 == 0 ? 0 : 1;
        global.insert(0, paddingAlphabet, alphabetHalfSize, offset);
        global.insert(offset + initialSize, paddingAlphabet, 0, alphabetHalfSize);
        // decrease the padding left
        paddingLeft -= paddingAlphabet.length;
      } else {
        // calculate the excess
        final int excess = paddingAlphabet.length + global.length() - minLength;
        // insert the first half
        final int firstHalfStartOffset = excess/2;
        final int firstHalfSize = paddingAlphabet.length - alphabetHalfSize - firstHalfStartOffset;
        global.insert(0, paddingAlphabet, firstHalfStartOffset, firstHalfSize);
        // insert the second half
        final int secondHalfEndOffset = minLength - firstHalfSize - initialSize;
        global.insert(firstHalfSize + initialSize, paddingAlphabet, 0, secondHalfEndOffset);
        paddingLeft = 0;
      }
    }

    return global.toString();
  }

  //-------------------------
  // Decode
  //-------------------------

  public long[] decode(final String hash) {
    if (hash == null) return null;

    // create a set of the guards
    final Set<Character> guardsSet = IntStream.range(0, guards.length)
        .mapToObj(idx -> guards[idx])
        .collect(Collectors.toSet());
    // count the total guards used
    final int[] guardsIdx = IntStream.range(0, hash.length())
        .filter(idx -> guardsSet.contains(hash.charAt(idx)))
        .toArray();
    // get the start index base on the guards count
    final int startIdx = guardsIdx.length == 1 || guardsIdx.length == 2 ? guardsIdx[1] : 0;

    LongStream decoded = LongStream.empty();
    // parse the hash
    if (hash.length() > 0) {
      final char lottery = hash.charAt(startIdx);

      // create the separators set
      // FIXME do this at construction
      final Set<Character> separatorsSet = IntStream.range(0, separators.length)
          .mapToObj(idx -> separators[idx])
          .collect(Collectors.toSet());

      // create the initial accumulation string
      final int length = hash.length() - guardsIdx.length - 1;
      StringBuilder block = new StringBuilder(length);

      // create the base salt
      final char[] decodeSalt = new char[alphabet.length];
      decodeSalt[0] = lottery;
      final int saltLength = salt.length >= alphabet.length ? alphabet.length-1 : salt.length;
      System.arraycopy(salt, 0, decodeSalt, 1, saltLength);
      final int saltLeft = alphabet.length - saltLength - 1;

      // the alphabet
      char[] currentAlphabet = alphabet;

      for (int i=startIdx+1; i<hash.length(); i++) {
        if (guardsSet.contains(hash.charAt(i))) continue;
        if (separatorsSet.contains(hash.charAt(i))) {
          // the end of this block

          // create the salt
          if (saltLeft > 0) {
            System.arraycopy(currentAlphabet, 0, decodeSalt,
                alphabet.length-saltLeft, saltLength);
          }

          // shuffle the alphabet
          currentAlphabet = shuffle(currentAlphabet, decodeSalt);

          // prepend the decoded value
          final long n = translate(block.toString().toCharArray(), currentAlphabet);
          decoded = LongStream.concat(LongStream.of(n), decoded);

          // create a new block
          block = new StringBuilder(length);
        }

        block.append(hash.charAt(i));
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

  private StringBuilder translate(final long n, final char lottery,
      final char[] alphabet, final StringBuilder sb, final int start) {
    if (n <= 0) {
      throw new IllegalArgumentException("Invalid number: " + n);
    }

    long input = n;
    while (input > 0) {
      // prepend the chosen char
      sb.insert(start, alphabet[(int) (input % alphabet.length)]);

      // trim the input
      input = input / alphabet.length;
    }

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
          if (!invalid.contains(c)) seen.add(c);
        });

    // check if got the same untouched alphabet
    if (seen.size() == alphabet.length) {
      return alphabet;
    }

    // create a new alphabet without the duplicates
    final char[] uniqueAlphabet = new char[seen.size()];
    int idx = 0;
    for (char c : seen) uniqueAlphabet[idx++] = c;
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