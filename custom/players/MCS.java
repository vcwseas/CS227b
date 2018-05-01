package custom.players;

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

public class MCS extends StateMachineGamer {
	private int counter = 0;
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
		// TODO Auto-generated method stub
		StateMachine m = getStateMachine();
		Role r = getRole();
		MachineState s = getCurrentState();

		Move move = getBestMove(m,r,s, timeout);
		return move;
	}

	private Move getBestMove(StateMachine m, Role r, MachineState s, long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		//Iterative deepening DFS with mobility heuristic

		int numCharges = 10;
		int limit = 3;
		List<Move> moves = m.findLegals(r, s);
		if (moves.size() == 1) {
			return moves.get(0);
		}
		Move bestMove = moves.get(0);
		float alpha = 0;  //best score
		float beta = 100; //worst score'

		for (int i = 0; i < moves.size(); i++) {
			if (timeout - System.currentTimeMillis() < 3000) {
				System.out.println(counter);
				return bestMove;
			}
			float tempBeta = minScore(m,r,s, moves.get(i), alpha, beta, 0, limit, numCharges, timeout); //temp beta

			if (tempBeta == 100) {
				return moves.get(i);
			}

			if (tempBeta > alpha) {
				alpha = tempBeta;
				bestMove = moves.get(i);
			}
			// if tempAlpha < beta : beta = tempAlpha
		}
		System.out.println(counter);
		return bestMove;
	}

	private float monteCarlo(StateMachine m, Role r, MachineState s, int numCharges) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		float total = 0;
		for (int i = 0; i < numCharges; i++) {
			total += depthCharge(m, r, s);
		}
		counter += numCharges;
		return total / numCharges;
	}

	private int depthCharge(StateMachine m, Role r, MachineState s) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}
		List<Move> jointMoves = m.getRandomJointMove(s);
		return depthCharge(m,r,m.getNextState(s, jointMoves));
	}

	private float maxScore(StateMachine m, Role r, MachineState s, float alpha, float beta, int level, int limit, int numCharges, long timeout) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}
		if (level>=limit) return monteCarlo(m,r,s, numCharges); //returns reward
		List<Move> moves = m.findLegals(r, s);
		for (int i = 0; i < moves.size(); i++) {
			if (timeout - System.currentTimeMillis() < 3000) {
				return alpha;
			}
			float tempScore = minScore(m,r,s,moves.get(i), alpha, beta, level, limit, numCharges, timeout);

			alpha = Math.max(alpha, tempScore);  //max(alpha, beta)
			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	private float minScore(StateMachine m, Role r, MachineState s, Move maxMove, float alpha, float beta, int level, int limit, int numCharges, long timeout) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}

		List<List<Move>> jointMoves = m.getLegalJointMoves(s, r, maxMove);
		for (List<Move> move : jointMoves) {
			if (timeout - System.currentTimeMillis() < 3000) {
				return beta;
			}
			MachineState nextState = m.getNextState(s, move);
			float tempAlpha = maxScore(m,r,nextState, alpha, beta, level+1, limit, numCharges, timeout);
			beta = Math.min(beta,  tempAlpha);
			if (beta <= alpha) {
				return alpha;
			}
		}
		return beta;
	}



	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "MCSGamer";
	}

}