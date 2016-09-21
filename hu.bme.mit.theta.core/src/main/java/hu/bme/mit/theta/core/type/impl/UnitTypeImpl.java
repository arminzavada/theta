package hu.bme.mit.theta.core.type.impl;

import java.util.Optional;

import hu.bme.mit.theta.core.expr.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.UnitType;
import hu.bme.mit.theta.core.utils.TypeVisitor;

final class UnitTypeImpl implements UnitType {

	private static final int HASH_SEED = 5519;

	private static final String TYPE_LABEL = "Unit";

	UnitTypeImpl() {
	}

	@Override
	public final LitExpr<UnitType> getAny() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public boolean isLeq(final Type type) {
		return this.equals(type);
	}

	@Override
	public Optional<? extends Type> meet(final Type type) {
		if (type instanceof UnitType) {
			return Optional.of(this);
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<? extends Type> join(final Type type) {
		if (type instanceof UnitType) {
			return Optional.of(this);
		} else {
			return Optional.empty();
		}
	}

	@Override
	public final <P, R> R accept(final TypeVisitor<? super P, ? extends R> visitor, final P param) {
		return visitor.visit(this, param);
	}

	@Override
	public final int hashCode() {
		return HASH_SEED;
	}

	@Override
	public boolean equals(final Object obj) {
		return (obj instanceof UnitType);
	}

	@Override
	public final String toString() {
		return TYPE_LABEL;
	}

}