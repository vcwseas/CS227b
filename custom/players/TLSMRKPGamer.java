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

public class TLSMRKPGamer extends StateMachineGamer {
/* Time-Limited Search Monotonic Reward Keep Alive Gamer.
 * Reward heuristic: Exhaustively search all the joint moves possible and take the action that maximizes the value of the next state that
 * can be reached. Which state is actually reached depends on what moves other players make. Will prefer intermediate states to terminal states
 * if both have the same value.
 */

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
		List<Move> moves = m.findLegals(r, s);

		Move bestMove = moves.get(0);
		int alpha = 0;  //best score
		int beta = 100; //worst score
		for (int i = 0; i < moves.size(); i++) {
			if (timeout - System.currentTimeMillis() < 3000) {
				return OutOfTimeGetMove(m,r,s);
			}
			int tempBeta = minScore(m,r,s, moves.get(i), alpha, beta, timeout); //temp beta

			if (tempBeta == 100) {
				return moves.get(i);
			}

			if (tempBeta > alpha) {
				alpha = tempBeta;
				bestMove = moves.get(i);
			}
		}
		return bestMove;
	}

	private int maxScore(StateMachine m, Role r, MachineState s, int alpha, int beta, long timeout) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}
		List<Move> moves = m.findLegals(r, s);
		for (int i = 0; i < moves.size(); i++) {
			if (timeout - System.currentTimeMillis() < 3000) {
				return alpha;
			}
			int tempScore = minScore(m,r,s,moves.get(i), alpha, beta, timeout);

			alpha = Math.max(alpha, tempScore);  //max(alpha, beta)
			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	private int minScore(StateMachine m, Role r, MachineState s, Move maxMove, int alpha, int beta, long timeout) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}

		List<List<Move>> jointMoves = m.getLegalJointMoves(s, r, maxMove);
		for (List<Move> move : jointMoves) {
			if (timeout - System.currentTimeMillis() < 3000) {
				return beta;
			}
			MachineState nextState = m.getNextState(s, move);
			int tempAlpha = maxScore(m,r,nextState, alpha, beta, timeout);
			beta = Math.min(beta,  tempAlpha);
			if (beta <= alpha) {
				return alpha;
			}
		}
		return beta;
	}



	private Move OutOfTimeGetMove(StateMachine m, Role r, MachineState s) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{

		List<List<Move>> jointMoves = m.getLegalJointMoves(s);
		int score = 0;
		Move bestMove = m.findLegalx(r, s);
		int ourIndex = m.getRoleIndices().get(r);
		for (int i = 0; i < jointMoves.size(); i++) {
			List<Move> move = jointMoves.get(i);
			MachineState nextState = m.findNext(move, s);
			int temp = m.getGoal(nextState, r);
			if (temp == 100) {
				return move.get(ourIndex);
			}
			if (temp == score) {
				// in ties, prefer nonterminal to terminal
				bestMove = m.isTerminal(nextState) ? bestMove : move.get(ourIndex);
			}
			if (temp > score) {
				score = temp;
				bestMove = move.get(ourIndex);
			}
		}
		return bestMove;
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
		return "TLSMRKPGamer";
	}

}