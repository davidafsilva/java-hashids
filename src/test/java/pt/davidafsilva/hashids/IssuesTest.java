package pt.davidafsilva.hashids;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author david
 */
public class IssuesTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void issue34() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Invalid alphabet for hash");
    final Hashids hashids = Hashids.newInstance("key-user",
        "abcdefghijklmnopqrstuvwxyz1234567890", 5);
    hashids.decode("as-buy7-kk");
  }

}
