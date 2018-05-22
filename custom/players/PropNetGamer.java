package ass1;

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
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;;


public class PropNetGamer extends StateMachineGamer {
	int numCharges=10;
	int expCnst=30;
	double avgDepth;
	double currDepth;
	double dc=0;
	private StateMachine m;
	private MachineState s;
	private Role r;
	private int safety = 3000;
	PNStateMachine PNSM;

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

//	private double metaDC(StateMachine m, MachineState s, int i) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
//		if (PNSM.findTerminalp(s)) return i+1;
//		List<Move> jointMoves = m.getRandomJointMove(s);
//		return metaDC(m,PNSM.getNextState(jointMoves, s),i+1);
//	}

	private double recursionlessMetaDC(StateMachine m, MachineState s,long timeout) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		double i=0;
		while (!PNSM.isTerminal(s) && timeout - System.currentTimeMillis() > this.safety) {
			List<Move> jointMoves = PNSM.getRandomJointMove(s);
			s=m.getNextState(s, jointMoves);
			i++;
		}
		return i+1;
	}

	private double expCnst() {
		return (double) Math.max(10, 40-60/(avgDepth)*currDepth);
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		r = getRole();
		m = getStateMachine();


		try {
			PNSM = new PNStateMachine(getMatch().getGame().getRules());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		avgDepth=0;
		currDepth=0;

		double count=0;
		double levels=0;

		while (timeout - System.currentTimeMillis() > this.safety) {
			levels+=recursionlessMetaDC(m, m.getInitialState(),timeout);
			count++;

		}
		avgDepth=levels/count*3/2;


 	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		s = getCurrentState();
		List<Move> lm = PNSM.getLegalMoves(s, r);
		if (lm.size() == 1) return lm.get(0);

		currDepth++;
		System.out.println(currDepth);
		System.out.println(expCnst());

		if (PNSM.getRoles().size()==1) return getSPmove(timeout);
		Node root = new Node(null, 0, s);
		Move move = getBestMove(root, timeout);
		System.out.println(dc);
		return move;
	}

	//
	//
	//  Singleplayer
	//
	//

	private Move getSPmove (long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		SPNode root = new SPNode(null, 0, s);
		while (timeout - System.currentTimeMillis() > this.safety) {
			SPNode newNode = selectSP(root);
			expandSP(newNode);
			int score=0;
			for (int i=0; i<numCharges; i++) {
				score += depthCharge(m, r, newNode.state);
			}
			score/=numCharges;
			BP(newNode, score);
		}

		double bestScore = 0;
		int bestMoveIndex = 0;

		for (SPNode subNode: root.children) {
			double testScore = (double) subNode.utility / subNode.visits;
			System.out.println(root.legalMoves.get(subNode.moveNumber) + " U" + subNode.utility + " V" + subNode.visits + " Score " + testScore);
			if (testScore > bestScore) {
				bestScore = testScore;
				bestMoveIndex = subNode.moveNumber;
			}
		}
		return root.legalMoves.get(bestMoveIndex);
	}

	public void expandSP(SPNode node) throws MoveDefinitionException, TransitionDefinitionException {
		node.expand(node);
	}

	private void BP(SPNode node, int score) {
		node.utility += score;
		node.visits += 1;
		if (node.parent!=null) BP(node.parent,score);
	}

	class SPNode {
		public SPNode parent = null;
		public List<Move> legalMoves;
		public List<SPNode> children = new ArrayList<SPNode>();
		public int moveNumber;
		public MachineState state;
		public double utility = 0;
		public double visits = 0;

		public SPNode(SPNode parent, int moveNumber, MachineState state)  {
			this.parent = parent;
			this.state = state;
			this.moveNumber = moveNumber;
		}

		public void expand(SPNode node) throws MoveDefinitionException, TransitionDefinitionException {
			if (PNSM.isTerminal(node.state)) {
				return;
			}
			this.legalMoves = PNSM.getLegalMoves(this.state, r);

			for (int i = 0; i< this.legalMoves.size(); i++) {
				SPNode subNode = new SPNode(this, i, m.getNextState(node.state, PNSM.getLegalJointMoves(node.state, r, this.legalMoves.get(i)).get(0)));
				this.children.add(subNode);
			}
		}

		public SPNode getParent() {
			return this.parent;
		}

		public boolean isLeaf() {
			return this.children.isEmpty();
		}
	}

	private SPNode selectSP(SPNode node) {
		if (node.isLeaf()) {
			return node;
		}
		double bestintscore=0;
		SPNode bestintnode=node.children.get(0);
		for (SPNode subNode: node.children) {
			if (subNode.visits == 0) {
				bestintnode=subNode;
				break;
			}
			double tempScore = SPselectfn(subNode);
			if (tempScore > bestintscore) {
				bestintscore = tempScore;
				bestintnode = subNode;
			}
		}
		return selectSP(bestintnode);
	}

	private double SPselectfn(SPNode n) {
		double exploit = (double) n.utility/n.visits;
		double explore = expCnst*Math.sqrt(2 * Math.log(n.parent.visits)/n.visits);
		return exploit + explore;
	}



	//
	//
	//  Multiplayer
	//
	//

	private Move getBestMove(Node root, long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		dc=0;
		while (timeout - System.currentTimeMillis() > this.safety) {
			Node newNode = select(root);
			expand(newNode);
			int score=0;
			for (int i=0; i<numCharges; i++) {
				dc++;
				score += depthCharge(m, r, newNode.state);
			}
			score/=numCharges;
			BPtoN(newNode, score);
		}
		return myBestMove(root);
	}

	private Move myBestMove(Node root) {
		double bestScore = 0;
		int bestMoveIndex = 0;

		for (moveNode subNode: root.children) {
			double testScore = (double) subNode.utility / subNode.visits;
			System.out.println(root.legalMoves.get(subNode.moveNumber) + " U" + subNode.utility + " V" + subNode.visits + " Score " + testScore);
			if (testScore > bestScore) {
				bestScore = testScore;
				bestMoveIndex = subNode.moveNumber;
			}
		}
		return root.legalMoves.get(bestMoveIndex);
	}

	private int depthCharge(StateMachine m, Role r, MachineState s) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (PNSM.isTerminal(s)) {
			return PNSM.getGoal(s,r);
		}
		List<Move> jointMoves = PNSM.getRandomJointMove(s);
		return depthCharge(m,r,m.getNextState(s, jointMoves));
	}

	private void BPtoN(Node node, int score) {
		node.utility += score;
		node.visits += 1;
		if (node.parent!=null) BPtoMN(node.parent,score);
	}

	private void BPtoMN(moveNode node, int score) {
		node.utility += score;
		node.visits += 1;
		BPtoN(node.parent,score);
	}

	private Node select(Node node) {
		if (node.isLeaf()) {
			return node;
		}
		double bestintscore=0;
		moveNode bestintnode=node.children.get(0);
		for (moveNode subNode: node.children) {
			if (subNode.visits == 0) {
				bestintnode=subNode;
				break;
			}
			double tempScore = selectfn(subNode);
			if (tempScore > bestintscore) {
				bestintscore = tempScore;
				bestintnode = subNode;
			}
		}
		double bestScore = 0;
		Node bestNode=bestintnode.children.get(0);
		for (Node subNode: bestintnode.children) {
				if (subNode.visits == 0) {
					return subNode;
				}
				double testScore = selectfnmin(subNode);
				if (testScore > bestScore) {
					bestScore = testScore;
					bestNode = subNode;
				}
		}
		return select(bestNode);
	}

	public void expand(Node node) throws MoveDefinitionException, TransitionDefinitionException {
		node.expand(node);
	}

	private double selectfn(moveNode n) {
		double exploit = (double) n.utility/n.visits;
		double explore = expCnst()*Math.sqrt(2 * Math.log(n.parent.visits)/n.visits);
		return exploit + explore;
	}

	private double selectfnmin(Node n) {
		double exploit = (double) n.utility/n.visits;
		double explore = expCnst()*Math.sqrt(2 * Math.log(n.parent.visits)/n.visits);
		return -exploit + explore;
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
		return "PropNetGamer";
	}

	class Node {
		public moveNode parent = null;
		public List<Move> legalMoves;
		public List<moveNode> children = new ArrayList<moveNode>();
		public int moveNumber;
		public MachineState state;
		public double utility = 0;
		public double visits = 0;

		public Node(moveNode parent, int moveNumber, MachineState state)  {
			this.parent = parent;
			this.state = state;
			this.moveNumber = moveNumber;
		}

		public void expand(Node node) throws MoveDefinitionException, TransitionDefinitionException {
			if (PNSM.findTerminalp(node.state)) {
				return;
			}
			this.legalMoves = PNSM.getLegalMoves(this.state, r);

			for (int i = 0; i< this.legalMoves.size(); i++) {
				moveNode subNode = new moveNode(this, this.legalMoves.get(i), i);
				this.children.add(subNode);
			}
		}

		public moveNode getParent() {
			return this.parent;
		}

		public boolean isLeaf() {
			return this.children.isEmpty();
		}

	}

	class moveNode {
		List<Node> children = new ArrayList<Node>();
		public double utility = 0;
		public double visits = 0;
		public int moveNumber;
		public Node parent;
		public moveNode(Node parent, Move move, int moveNumber) throws MoveDefinitionException, TransitionDefinitionException {
			this.parent=parent;
			List<List<Move>> jointMoves = m.getLegalJointMoves(parent.state, r, move);
			for (List<Move> jointMove: jointMoves) {
				MachineState nextState = m.getNextState(parent. state, jointMove);
				Node nextNode = new Node(this, moveNumber, nextState);
				children.add(nextNode);
			}
			this.moveNumber = moveNumber;
		}
	}

}