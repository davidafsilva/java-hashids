package pt.davidafsilva.hashids;

import java.io.InputStreamReader;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * @author david
 */
abstract class AbstractHashidsTest {

  // the test configuration
  final Hashids algorithm;
  final JsHashids jsAlgorithm;
  final long[] input;

  AbstractHashidsTest(final Supplier<Hashids> algorithm, final Supplier<JsHashids> jsAlgorithm,
      final long[] input) {
    this.algorithm = algorithm.get();
    this.jsAlgorithm = jsAlgorithm.get();
    this.input = input;
  }

  static JsHashids getJsAlgorithm(final String salt, final String alphabet,
      final int minLength) {
    try {
      // get the JS engine
      final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
      // load the hashids.js
      nashorn.eval(new InputStreamReader(AbstractHashidsTest.class.getResourceAsStream("/hashids.js")));

      // create the instantiation script
      final StringBuilder script = new StringBuilder("var hashids = new Hashids(");
      // salt
      if (salt != null && salt.length() > 0) script.append("'").append(salt).append("',");
      else script.append("undefined,");
      // minLength
      script.append(minLength).append(",");
      // alphabet
      if (alphabet != null && alphabet.length() > 0) script.append("'").append(alphabet).append("'");
      else script.append("undefined");
      script.append(");");

      // evaluate the script
      nashorn.eval(script.toString());

      // return the algorithm bridge
      return new JsHashids(nashorn);
    } catch (final Exception e) {
      throw new RuntimeException("unable to initialize JS algorithm", e);
    }
  }

  final static class JsHashids {

    private final ScriptEngine engine;

    private JsHashids(final ScriptEngine engine) {this.engine = engine;}

    String encode(long[] numbers) {
      final String numbersStr = LongStream.of(numbers)
          .mapToObj(String::valueOf)
          .collect(Collectors.joining(","));
      return eval("hashids.encode(" + numbersStr + ");");
    }

    long[] decode(String hash) {
      return eval("hashids.decode('" + hash + "');");
    }

    @SuppressWarnings("unchecked")
    <T> T eval(final String script) {
      try {
        return (T) engine.eval(script);
      } catch (final Exception e) {
        throw new AssertionError("unexpected exception", e);
      }
    }
  }

}
