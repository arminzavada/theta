package hu.bme.mit.inf.ttmc.analysis.tcfa;

import java.util.ArrayList;
import java.util.Collection;

import hu.bme.mit.inf.ttmc.analysis.AnalysisContext;
import hu.bme.mit.inf.ttmc.formalism.tcfa.TCFAEdge;
import hu.bme.mit.inf.ttmc.formalism.tcfa.TCFALoc;

public class TCFAAnalysisContext implements AnalysisContext<TCFAState<?>, TCFAAction> {

	@Override
	public Collection<TCFAAction> getEnabledActionsFor(final TCFAState<?> state) {
		final Collection<TCFAAction> tcfaActions = new ArrayList<>();
		final TCFALoc loc = state.getLoc();

		for (final TCFAEdge outEdge : loc.getOutEdges()) {
			tcfaActions.add(TCFAAction.discrete(outEdge));
		}

		if (!loc.isUrgent()) {
			tcfaActions.add(TCFAAction.delay(loc));
		}

		return tcfaActions;
	}

}
