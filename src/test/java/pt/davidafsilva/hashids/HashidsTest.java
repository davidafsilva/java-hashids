package pt.davidafsilva.hashids;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author david
 */
public class HashidsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void nullInput() {
    assertThat(Hashids.getInstance().encode((long[]) null), nullValue());
    assertThat(Hashids.getInstance().encodeHex(null), nullValue());
    assertThat(Hashids.getInstance().decode(null), nullValue());
    assertThat(Hashids.getInstance().decodeHex(null), nullValue());
  }

  @Test
  public void negativeInput() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("invalid number: -1");
    Hashids.getInstance().encode(-1);
  }

  @Test
  public void invalidAlphabetLength() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("alphabet must contain at least 16 unique characters: 6");
    Hashids.newInstance("salt", "123456");
  }

  @Test
  public void alphabetWithSpaces() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("alphabet must not contain spaces: index 1");
    Hashids.newInstance("salt", "1 234567890abcdefg");
  }

  @Test
  public void hexEncodeDecode() {
    final Hashids hashids = Hashids.newInstance("my awesome salt");
    final String encoded1 = hashids.encodeHex("507f1f77bcf86cd799439011");
    final String encoded2 = hashids.encodeHex("0x507f1f77bcf86cd799439011");
    final String encoded3 = hashids.encodeHex("0X507f1f77bcf86cd799439011");
    assertThat(encoded1, equalTo(encoded2));
    assertThat(encoded1, equalTo(encoded3));
    assertThat(encoded1, equalTo("R2qnd2vkOJTXm7XV7yq4"));
    final String decoded = hashids.decodeHex(encoded1);
    assertThat(decoded, equalTo("507f1f77bcf86cd799439011"));
  }
}
