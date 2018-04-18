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

public class AlphaBetaGamer extends StateMachineGamer {

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

		Move move = getBestMove(m,r,s);
		return move;
	}

	private Move getBestMove(StateMachine m, Role r, MachineState s) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		List<Move> moves;
		moves = m.findLegals(r, s);

		Move bestMove = moves.get(0);
		int bestScore = 0;
		for (int i = 0; i < moves.size(); i++) {
			int tempScore = minScore(m,r,s, moves.get(i),0, 100);

			if (tempScore == 100) {
				return moves.get(i);
			}

			if (tempScore > bestScore) {
				bestScore = tempScore;
				bestMove = moves.get(i);
			}
		}
		return bestMove;
	}

	private int maxScore(StateMachine m, Role r, MachineState s, int alpha, int beta) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}
		List<Move> moves = m.findLegals(r, s);
		for (int i = 0; i < moves.size(); i++) {
			int tempScore = minScore(m,r,s,moves.get(i),alpha, beta);
			alpha = Math.max(alpha, tempScore);  //max(alpha, beta)
			if (alpha >= beta) {
				return beta;
			}
		}
		return alpha;
	}

	private int minScore(StateMachine m, Role r, MachineState s, Move maxMove, int alpha, int beta) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException{
		if (m.isTerminal(s)) {
			return m.getGoal(s, r);
		}

		//find opponent
		Role opponent = r;
		for (Role ro : m.findRoles()) {
			if (!ro.equals(r)) {
				opponent = ro;
			}
		}

		List<Move> moves = m.findLegals(opponent, s);
		for (int i = 0; i < moves.size(); i++) {
			List<Move> move = new ArrayList<Move>();
			if (r.equals(m.getRoles().get(0))) {
				move.add(maxMove);
				move.add(moves.get(i));
			} else {
				move.add(moves.get(i));
				move.add(maxMove);
			}

			MachineState nextState = m.getNextState(s, move);
			int tempScore = maxScore(m,r,nextState, alpha, beta);

			beta = Math.min(beta, tempScore);
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
		return "AlphaBetaGamer";
	}

}