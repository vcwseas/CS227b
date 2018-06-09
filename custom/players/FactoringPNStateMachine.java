package ass1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;;

public class FactoringPNStateMachine extends StateMachine {

	PropNet PN;
	Role role;
	Set<Component> all;
	List<List<Move>> factors;
	boolean factored=false;

	public FactoringPNStateMachine(List<Gdl> rules,int players) throws InterruptedException {
		PN = OptimizingPropNetFactory.create(rules, true);
		if (players==1) {
			Set<Component> allComponents = new HashSet<Component>(PN.getComponents());
			Queue<Component> q = new LinkedList<Component>();
			Component t = PN.getTerminalProposition();
			q.add(t);
			allComponents.remove(t);

			for (Set<Proposition> s : PN.getGoalPropositions().values()) {
				for (Component c : s) {
					q.add(c);
					allComponents.remove(c);
				}
			}

			while (!q.isEmpty()) {
				Component c = q.remove();
				Set<Component> newComponents = c.getInputs();
				for (Component nC: newComponents) {
					if (allComponents.contains(nC)) {
						allComponents.remove(nC);
						q.add(nC);

					}
				}
			}

			Map<Proposition, Proposition> legalInputMap = PN.getLegalInputMap();
			for (Set<Proposition> s: PN.getLegalPropositions().values()) {
				for (Component c: s) {
					Component input = legalInputMap.get(c);
					if (!allComponents.contains(input)) {
						q.add(c);
						allComponents.remove(c);
					}
				}
			}

			while (!q.isEmpty()) {
				Component c = q.remove();
				Set<Component> newComponents = c.getInputs();
				for (Component nC: newComponents) {
					if (allComponents.contains(nC)) {
						allComponents.remove(nC);
						q.add(nC);
					}
				}
			}

			for (Component c: allComponents) {
				PN.removeComponent(c);
			}



			//second factoring

			Proposition term=PN.getTerminalProposition();

			if (term.getSingleInput() instanceof Or) {
				System.out.println("yay");

				factors= new ArrayList<List<Move>>();
				for (Component c: term.getSingleInput().getInputs()) {
					List<Move> f = new ArrayList<Move>();
					Set<Component> visited=new HashSet<>();
					helper(c,f,visited);
					factors.add(f);
				}
				factored=true;
			}
			System.out.println(factors.size());
			for (int i=0; i<factors.size(); i++) {
				System.out.println("factor" + i + factors.get(i));
			}




		}
	}

	void helper(Component c, List<Move> f, Set<Component> visited) {
		if (!visited.contains(c)) {
			visited.add(c);
			if (c instanceof Proposition) {
				Proposition p = (Proposition) c;
				if (PN.getInputPropositions().values().contains(p)) f.add(new Move(p.getName().get(1)));
			}
			for (Component cc: c.getInputs()) {
				helper(cc,f,visited);
			}
		}
	}

	public List<List<Move>> getFactors() {
		return factors;
	}

	public boolean isFactored() {
		return factored;
	}


	void setRole(Role r) {
		role=r;
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
		clearPN();
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
		clearPN();
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
		clearPN();
		markBases(s.getContents());
		Set<Proposition> legals=PN.getLegalPropositions().get(r);
		List<Move> moves=new ArrayList<Move>();
		for (Proposition p: legals) {
			if (propMark((Component) p)) moves.add(new Move(p.getName().getBody().get(1)));
		}
		clearPN();
		return moves;
	}

	private Set<GdlSentence> toDoes(List<Move> moves)
	{
		Set<GdlSentence> doeses = new HashSet<GdlSentence>();
		Map<Role, Integer> roleIndices = getRoleIndices();
		List<Role> roles=this.getRoles();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	@Override
	public MachineState getNextState(MachineState s, List<Move> moves) throws TransitionDefinitionException {
		clearPN();
		Set<GdlSentence> set= toDoes(moves);
		markBases(s.getContents());
		markActions(set);
		Set<GdlSentence> nextState=new HashSet<GdlSentence>();
		for (GdlSentence sent: PN.getBasePropositions().keySet()) {
			if (propMark(PN.getBasePropositions().get(sent).getSingleInput().getSingleInput())) nextState.add(sent);
		}
		clearPN();
		return new MachineState(nextState);
	}



}