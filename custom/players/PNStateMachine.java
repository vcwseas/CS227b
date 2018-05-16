package ass1;

import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;;

public class PNStateMachine {

	PropNet PN;

	public PNStateMachine(List<Gdl> rules) throws InterruptedException {
		PN = OptimizingPropNetFactory.create(rules, true);
	}





	private void markBases(Set<GdlSentence> sentences){
		for (GdlSentence sent: PN.getBasePropositions().keySet()){
			PN.getBasePropositions().get(sent).setValue(sentences.contains(sent));
		}
	}

	private void markActions(Set<GdlSentence> sentences){
		for (GdlSentence sent: PN.getInputPropositions().keySet()){
			PN.getInputPropositions().get(sent).setValue(sentences.contains(sent));
		}
	}



	private boolean propMark(Component c){
		if (c instanceof Proposition){

			Proposition p = (Proposition) c;
			// view proposition
			if (p.getInputs().size() == 1 && !(p.getSingleInput() instanceof Transition)){
				return propMark(p.getSingleInput());
			}
			// base or input proposition
			else return p.getValue();
		}
		else if (c instanceof And){
			for (Component c2 : c.getInputs()){
				if (!propMark(c2)) return false;
			}
			return true;
		}
		else if (c instanceof Or){
			for (Component c2 : c.getInputs()){
				if (propMark(c2)) return true;
			}
			return false;
		}
		else if (c instanceof Constant){
			return c.getValue();
		}
		else if (c instanceof Not){
			return !propMark(c.getSingleInput());
		}
		else if (c instanceof Transition){
			return propMark(c.getSingleInput());
		}

		return false;
	}



}