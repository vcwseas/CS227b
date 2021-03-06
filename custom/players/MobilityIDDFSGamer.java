package ass1;

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

public class MobilityIDDFSGamer extends StateMachineGamer {

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

		int limit=0;

		List<Move> moves = m.findLegals(r, s);


		Move bestMove = moves.get(0);

		if (m.findLegals(r, s).size()==1) return bestMove;

		int alpha = 0;  //best score
		int beta = 100; //worst score'

		while (true) {
			for (int i = 0; i < moves.size(); i++) {
				if (timeout - System.currentTimeMillis() < 3000) {
					return bestMove;
				}
				int tempBeta = minScore(m,r,s, moves.get(i), alpha, beta, 0, limit, timeout); //temp beta

				if (tempBeta == 100) {
					return moves.get(i);
				}

				if (tempBeta > alpha) {
					alpha = tempBeta;
					bestMove = moves.get(i);
				}
				// if tempAlpha < beta : beta = tempAlpha
			}
			limit++;
		}
	}

	private int maxScore(StateMachine m, Role r, MachineState s, int alpha, int beta, int level, int limit, long timeout) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}
		if (level>=limit) return mobility(m, r, s); //prefer states with more mobility;
		List<Move> moves = m.findLegals(r, s);
		for (int i = 0; i < moves.size(); i++) {
			if (timeout - System.currentTimeMillis() < 3000) {
				return alpha;
			}
			int tempScore = minScore(m,r,s,moves.get(i), alpha, beta, level, limit, timeout);

			alpha = Math.max(alpha, tempScore);  //max(alpha, beta)
			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	private int minScore(StateMachine m, Role r, MachineState s, Move maxMove, int alpha, int beta, int level, int limit, long timeout) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}

		List<List<Move>> jointMoves = m.getLegalJointMoves(s, r, maxMove);
		for (List<Move> move : jointMoves) {
			if (timeout - System.currentTimeMillis() < 3000) {
				return beta;
			}
			MachineState nextState = m.getNextState(s, move);
			int tempAlpha = maxScore(m,r,nextState, alpha, beta, level+1, limit, timeout);
			beta = Math.min(beta,  tempAlpha);
			if (beta <= alpha) {
				return alpha;
			}
		}
		return beta;
	}

	private int mobility(StateMachine m, Role r, MachineState s) throws MoveDefinitionException{
		List<Move> moves = m.findLegals(r, s);
		List<Move> allMoves = m.findActions(r);
		float score = (float) moves.size() / (float) allMoves.size() * 100;
		return (int) score;

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
		return "MobilityIDDFSGamer";
	}

}