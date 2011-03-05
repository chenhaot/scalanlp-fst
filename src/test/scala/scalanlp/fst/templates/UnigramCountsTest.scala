package scalanlp.fst.templates

import scalanlp.fst._;
import scalanlp.math._;


import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.scalatest.prop._;

import org.scalacheck._;

/**
 * Tests the UnigramModel
 *
 * @author dlwh
 */
@RunWith(classOf[JUnitRunner])
class UnigramCountsTest extends FunSuite with Checkers {

  test("constant automaton") {
    import Automaton._;
    import Semiring.LogSpace.doubleIsLogSpace;

    val auto = constant("Hello",0.0);
    val template = new UnigramModel[Double,Char]('#', "Hello".toSet);
    val counts = ExpectedCounts.counts(auto,template);

    assert( counts(true)(true)('H') === 0.0);
    assert( counts(true)(true)('e') === 0.0);
    assert( counts(true)(true)('l') === doubleIsLogSpace.plus(0.0,0.0));
    assert( counts(true)(true)('o') === 0.0);
    assert( counts(true)(false)('\0') === 0.0);
  }

}