package ass1;

import java.util.ArrayList;
import java.util.HashSet;
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
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;;

public class PNStateMachine extends StateMachine {

	PropNet PN;

	public PNStateMachine(List<Gdl> rules) throws InterruptedException {
		PN = OptimizingPropNetFactory.create(rules, true);
	}

	@Override
	public List<Role> getRoles() {
		return PN.getRoles();
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

	private void clearPN() {
		for (Proposition p: PN.getPropositions()) {
			p.setValue(false);
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

	@Override
	public List<Move> findActions(Role role) throws MoveDefinitionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(List<Gdl> description) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getGoal(MachineState s, Role r) throws GoalDefinitionException {
		int goal=0;
		markBases(s.getContents());
		Set<Proposition> goals=PN.getGoalPropositions().get(r);
		for (Proposition p: goals) {
			if (propMark(p)) goal = Integer.parseInt(p.getName().getBody().get(1).toString());
		}
		clearPN();
		return goal;
	}

	@Override
	public boolean isTerminal(MachineState s) {
		markBases(s.getContents());
		boolean b = propMark(PN.getTerminalProposition());
		clearPN();
		return b;
	}

	@Override
	public MachineState getInitialState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Move> getLegalMoves(MachineState s, Role r) throws MoveDefinitionException {
		markBases(s.getContents());
		Set<Proposition> legals=PN.getLegalPropositions().get(r);
		List<Move> moves=new ArrayList<Move>();
		for (Proposition p: legals) {
			if (propMark((Component) p)) moves.add(new Move(p.getName().getBody().get(1)));
		}
		clearPN();
		return moves;
	}

	@Override
	public MachineState getNextState(MachineState s, List<Move> moves) throws TransitionDefinitionException {
		Set<GdlSentence> set= new HashSet<GdlSentence>();
		for (Move move: moves) {
			set.add(move.getContents().toSentence());
		}
		markActions(set);
		markBases(s.getContents());

		Set<GdlSentence> nextState=new HashSet<GdlSentence>();

		for (GdlSentence sent: PN.getBasePropositions().keySet()) {
			if (propMark(PN.getBasePropositions().get(sent).getSingleInput().getSingleInput())) nextState.add(sent);
		}
		clearPN();
		return new MachineState(nextState);
	}



}