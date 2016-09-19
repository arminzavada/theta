package hu.bme.mit.theta.formalism.cfa.impl;

import java.util.ArrayList;
import java.util.List;

import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.formalism.cfa.CfaEdge;
import hu.bme.mit.theta.formalism.cfa.CfaLoc;

class MutableCfaEdge implements CfaEdge {

	private CfaLoc source;
	private CfaLoc target;
	private final List<Stmt> stmts;
	
	MutableCfaEdge(final MutableCfaLoc source, final MutableCfaLoc target) {
		this.source = source;
		this.target = target;
		stmts = new ArrayList<>();
	}

	////

	@Override
	public CfaLoc getSource() {
		return source;
	}

	@Override
	public CfaLoc getTarget() {
		return target;
	}

	@Override
	public List<Stmt> getStmts() {
		return stmts;
	}

	////

}
