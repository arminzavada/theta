package hu.bme.mit.inf.ttmc.core.expr;

import hu.bme.mit.inf.ttmc.core.type.RatType;

public interface RatLitExpr extends LitExpr<RatType>, NullaryExpr<RatType>, Comparable<RatLitExpr> {

	public long getNum();

	public long getDenom();

}