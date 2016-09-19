package hu.bme.mit.theta.formalism.tcfa;

import java.util.List;

import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.formalism.common.Edge;

public interface TcfaEdge extends Edge<TcfaLoc, TcfaEdge> {

	public List<Stmt> getStmts();

}
