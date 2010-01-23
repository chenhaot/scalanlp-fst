package scalanlp.fst

import scalala.Scalala.{iArrayToVector=>_, _};
import scalala.tensor.sparse._
import scalanlp.counters.LogCounters._;
import scalanlp.math.Semiring
import scalanlp.math.Numerics._;import scalanlp.util.Index


class UnigramSemiring[@specialized("Char") T:Alphabet](chars: Set[T], beginningUnigram:T, cheatOnEquals:Boolean=false) {

  val charIndex = Index[T]();
  val beginningUnigramId = charIndex(beginningUnigram)
  for( ch <- chars) { charIndex(ch) }

  case class Elem(totalProb: Double, counts: SparseVector) {
    def decode: LogDoubleCounter[T] = {
      aggregate( counts.activeElements.map { (iv:(Int,Double)) => (charIndex.get(iv._1),iv._2)});
    }
  }

  private def mnorm(x: SparseVector, y: SparseVector): Boolean = {
    var i = 0;
    while(i < x.size) {
      if(!Semiring.LogSpace.doubleIsLogSpace.closeTo(x(i),y(i))) return false
      i += 1;
    }
    true
  }

  implicit val ring: Semiring[Elem] = new Semiring[Elem] {

    override def closeTo(x: Elem, y: Elem) = {
      import Semiring.LogSpace.doubleIsLogSpace;
      val ret = doubleIsLogSpace.closeTo(x.totalProb, y.totalProb) &&
                (cheatOnEquals || mnorm(x.counts,y.counts))
      ret
    }

    def plus(x: Elem, y: Elem) = {
      val newProb = logSum(x.totalProb,y.totalProb);
      
      val counts = x.counts.copy;
      logAddInPlace(counts,y.counts)
      Elem(newProb, counts);
    }

    override def maybe_+=(x:Elem, y: Elem) = if(x.totalProb == Double.NegativeInfinity) (y,closeTo(zero,y)) else {
      import scalanlp.math.Semiring.LogSpace.doubleIsLogSpace
      if(doubleIsLogSpace.closeTo(x.totalProb,logSum(x.totalProb,y.totalProb))) {
        (x,true)
      } else {
        logAddInPlace(x.counts,y.counts);
        (Elem(logSum(x.totalProb,y.totalProb),x.counts),false);
      }
    }


    def times(x: Elem, y: Elem) = {
      val newProb = x.totalProb + y.totalProb;

      val counts = x.counts + y.totalProb value;
      counts(beginningUnigramId) = Double.NegativeInfinity;
      logAddInPlace(counts,y.counts,x.totalProb);

      val r = Elem(newProb, counts);
      r
    }

    def closure(x: Elem) = {
      import Semiring.LogSpace.doubleIsLogSpace.{closure=>logClosure};
      val p_* = logClosure(x.totalProb);

      // counts. We're just going to assume that we're only closing over counts in position 0
      // XXX TODO
      val counts = x.counts + 2 * p_*;

      val r = Elem(p_*,counts);
      r
    }

    val one = {
      Elem(0.0, mkSparseVector);
    }

    val zero = Elem(-1.0/0.0, mkSparseVector);

  }


  def promote[S](a: Arc[Double,S,T]) = {
    val counts = mkSparseVector;
    if (a.label != implicitly[Alphabet[T]].epsilon) {
      counts(charIndex(a.label)) = a.weight;
    }
    Elem(a.weight, counts);
  }

  def promoteOnlyWeight(w: Double) = if(w == Double.NegativeInfinity) ring.zero else {
    val vec = mkSparseVector;
    vec(beginningUnigramId) = w;
    Elem(w, vec);
  }

  private def mkSparseVector = {
    val r = new SparseVector(charIndex.size);
    r.default = Double.NegativeInfinity;
    r
  }

  private def logAdd(to: SparseVector, from: SparseVector, scale: Double=0.0) = {
    val ret = to.copy;
    logAddInPlace(ret,from,scale);
    ret;
  }


  private def logAddInPlace(to: SparseVector, from: SparseVector, scale: Double=0.0) {
    if (scale != Double.NegativeInfinity) {
      var offset = 0;
      while( offset < from.used) {
        val k = from.index(offset);
        val v  = from.data(offset);
        to(k) = logSum(to(k),v + scale);
        offset += 1;
      }
    }
  }

}
