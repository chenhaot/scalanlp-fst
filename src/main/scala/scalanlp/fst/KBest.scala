package scalanlp.fst;

import scala.collection.mutable.PriorityQueue;
import scala.collection.mutable.ArrayBuffer;

import scalanlp.math._;

object KBest {
  def extract[W:Ordering,State,T](auto: Automaton[W,State,T]):Iterator[(Seq[T],W)] = {
    case class Derivation(str: ArrayBuffer[T], state: State, weight: W, atFinal: Boolean);
    implicit val orderDeriv = Ordering[W].on[Derivation](_.weight);

    val derivations = new Iterator[Derivation] {
      import auto.ring._;
      val pq = new PriorityQueue[Derivation];
      for( (state,w) <- auto.initialStateWeights) {
        pq += Derivation(ArrayBuffer.empty[T],state,w,false);
      }

      def hasNext:Boolean = {
        !pq.isEmpty
      }

      def next = {
        val deriv@Derivation(str,state,weight,atFinal) = pq.dequeue;
        if(!atFinal) {
          val finalWeight = auto.finalWeight(state);
          if(finalWeight != zero) {
            pq += Derivation(str,state,times(weight,finalWeight),true);
          }

          for( Arc(_,to,ch,_,w) <- auto.edgesFrom(state)) {
            val newDeriv = if(ch == auto.inAlpha.epsilon) str else (str.clone() += ch);
            pq += Derivation(newDeriv,to,times(weight,w),false);
          }
        }
        deriv;
      }
    }

    val kbest = for( deriv <- derivations if deriv.atFinal)
      yield (deriv.str,deriv.weight);

    kbest
  }

  def extractList[W:Ordering,State,T](auto: Automaton[W,State,T], num: Int):Seq[(Seq[T],W)] = {
    import auto.ring._;
    val heuristics = auto.reverse.allPathDistances;

    case class Derivation(str: ArrayBuffer[T], state: State, weight: W, heuristic: W, atFinal: Boolean);
    implicit val orderDeriv = Ordering[W].on[Derivation](_.heuristic);
    val kbest = new ArrayBuffer[(Seq[T],W)];


    val pq = new PriorityQueue[Derivation];
    for( (state,w) <- auto.initialStateWeights) {
      pq += Derivation(ArrayBuffer.empty,state,w,times(w,heuristics(state)),false);
    }

    while(!pq.isEmpty && kbest.length < num) {
      //println(pq.size);
      val deriv@Derivation(str,state,weight,_,atFinal) = pq.dequeue;
      if(!atFinal) {
        val finalWeight = auto.finalWeight(state);
        if(finalWeight != zero) {
          val finalScore = times(weight,finalWeight);
          pq += Derivation(str,state,finalScore,finalScore,true);
        }

        for( Arc(_,to,ch,_,w) <- auto.edgesFrom(state)) {
          val newDeriv = if(ch == auto.inAlpha.epsilon) str else (str.clone() += ch)
          val newWeight = times(weight,w);
          val newHeuristic = times(newWeight,heuristics(to));
          pq += Derivation(newDeriv,to,newWeight,newHeuristic,false);
        }
      } else {
        //println("emit");
        kbest += ((deriv.str,deriv.weight));
      }
    }

    kbest
  }
}

