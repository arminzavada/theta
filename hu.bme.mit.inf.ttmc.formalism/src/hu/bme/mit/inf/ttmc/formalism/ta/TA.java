package hu.bme.mit.inf.ttmc.formalism.ta;

import java.util.Collection;

import hu.bme.mit.inf.ttmc.formalism.common.automaton.Automaton;
import hu.bme.mit.inf.ttmc.formalism.common.decl.ClockDecl;

public interface TA extends Automaton<TALoc, TAEdge> {

	public Collection<? extends ClockDecl> getClocks();

}