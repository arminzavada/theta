package hu.bme.mit.theta.formalism.sts.utils.impl;

import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.utils.impl.ExprUtils;
import hu.bme.mit.theta.formalism.sts.STS;
import hu.bme.mit.theta.formalism.sts.impl.StsImpl;
import hu.bme.mit.theta.formalism.sts.utils.STSTransformation;

public final class StsIteTransformation implements STSTransformation {

	@Override
	public STS transform(final STS system) {
		final StsImpl.Builder builder = new StsImpl.Builder();
		for (final Expr<? extends BoolType> expr : system.getInit())
			builder.addInit(ExprUtils.eliminateITE(expr));
		for (final Expr<? extends BoolType> expr : system.getInvar())
			builder.addInvar(ExprUtils.eliminateITE(expr));
		for (final Expr<? extends BoolType> expr : system.getTrans())
			builder.addTrans(ExprUtils.eliminateITE(expr));
		builder.setProp(ExprUtils.eliminateITE(system.getProp()));

		return builder.build();
	}

}