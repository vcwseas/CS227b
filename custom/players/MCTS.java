package custom.players;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class MCTS extends StateMachineGamer {

	@Override
	public StateMachine getInitialStateMachine() {
		// From RandomGamer
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//no meta
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine m = getStateMachine();
		Role r = getRole();
		MachineState s = getCurrentState();

		Node n = new Node(null, null, s);
		Move move = getBestMove(n, m,r,s, timeout);
		return move;
	}

	private Move getBestMove(Node n, StateMachine m, Role r, MachineState s, long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		while (timeout - System.currentTimeMillis() > 3000) {
			Node newNode = select(n);
			expand(newNode, m, r, s);
			int score = depthCharge(m, r, newNode.state);
			backpropagate(newNode, score);
		}
		return n.myBestMove(r, m);
	}

	private Node select(Node n) {
		if (n.visits == 0) {
			return n;
		}
		for (Node child : n.children) {
			if (child.visits == 0) {
				return child;
			}
		}
		double score = 0;
		Node result = n;
		for (Node child : n.children) {
			double tempScore = selectfn(child);
			if (tempScore > score) {
				score = tempScore;
				result = child;
			}
		}
		return select(result);
	}

	private double selectfn(Node n) {
		double exploit = (double) n.utility/n.visits;
		double explore = Math.sqrt(2 * Math.log(n.parent.visits)/n.visits);
		return exploit + explore;
	}

	private void expand(Node n, StateMachine m, Role r, MachineState s) throws MoveDefinitionException, TransitionDefinitionException{
		List<List<Move>> jointMoves = m.getLegalJointMoves(s);
		for (List<Move> move: jointMoves) {
			MachineState nextState = m.findNext(move, s);
			n.addChild(move, nextState);
		}
	}

	private int depthCharge(StateMachine m, Role r, MachineState s) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}
		//approximates value of state by expected value (random moves for opponents)
		List<Move> jointMoves = m.getRandomJointMove(s);
		return depthCharge(m,r,m.getNextState(s, jointMoves));
	}

	private void backpropagate(Node n, int score) {
		n.incrementVisits();
		n.incrementUtility(score);
		if (n.parent != null) {
			backpropagate(n.parent, score);
		}
	}

	@Override
	public void stateMachineStop() {

	}

	@Override
	public void stateMachineAbort() {

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
	}

	@Override
	public String getName() {
		return "MCTSGamer";
	}

	class Node {
		//TODO:
		//may need to make opponent actions contingent on my actions
		//that is, may need to index children nodes by a dictionary {myMove : arrayList<Node> children}
		//where children in the dictionary are simulated by random opponent actions.

		Node parent;
		MachineState state; //the state this node represents
		List<Move> move; //move from parent node that resulted in this node
		int visits;
		ArrayList<Node> children;
		int utility;

		public Node(Node parent, List<Move> move, MachineState state) {
			this.parent = parent;
			this.move = move;
			this.state = state;
			this.visits = 0;
			this.children = new ArrayList<Node>();
			this.utility = 0;
		}

		public void incrementUtility (int u) {
			this.utility += u;
		}

		public void incrementVisits() {
			this.visits +=1;
		}

		public int getUtility() {
			return this.utility;
		}

		public int getVisits() {
			return this.visits;
		}

		public ArrayList<Node> getChildren() {
			return this.children;
		}

		public Node addChild(List<Move> move, MachineState state) {
			Node child = new Node(this, move, state);
			this.children.add(child);
			return child;
		}

		public Move myBestMove(Role r, StateMachine m) {
			//returns the move that results in the highest utility child
			int score = 0;
			int index = m.getRoleIndices().get(r);
			List<Move> jointMove = this.move;
			for (Node c : this.children) {
				if (c.utility > score) {
					score = c.utility;
					jointMove = c.move;
				}
			}
			return jointMove.get(index);
		}
	}

}