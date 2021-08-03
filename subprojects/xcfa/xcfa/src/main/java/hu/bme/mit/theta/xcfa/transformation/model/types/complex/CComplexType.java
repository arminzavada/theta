package hu.bme.mit.theta.xcfa.transformation.model.types.complex;

import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.xcfa.model.XcfaMetadata;
import hu.bme.mit.theta.xcfa.transformation.ArchitectureConfig;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.CInteger;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cbool.CBool;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cchar.CChar;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cchar.CSignedChar;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cchar.CUnsignedChar;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cint.CInt;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cint.CSignedInt;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cint.CUnsignedInt;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.clong.CLong;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.clong.CSignedLong;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.clong.CUnsignedLong;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.clonglong.CLongLong;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.clonglong.CSignedLongLong;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.clonglong.CUnsignedLongLong;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cshort.CShort;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cshort.CSignedShort;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.integer.cshort.CUnsignedShort;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.real.CDouble;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.real.CFloat;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.real.CLongDouble;
import hu.bme.mit.theta.xcfa.transformation.model.types.complex.real.CReal;
import hu.bme.mit.theta.xcfa.transformation.model.types.simple.CSimpleType;

import java.util.List;
import java.util.Optional;

import static hu.bme.mit.theta.xcfa.transformation.ArchitectureConfig.getCastVisitor;
import static hu.bme.mit.theta.xcfa.transformation.ArchitectureConfig.getLimitVisitor;
import static hu.bme.mit.theta.xcfa.transformation.ArchitectureConfig.getNullValueVisitor;
import static hu.bme.mit.theta.xcfa.transformation.ArchitectureConfig.getTypeVisitor;
import static hu.bme.mit.theta.xcfa.transformation.ArchitectureConfig.getUnitValueVisitor;

public abstract class CComplexType {
	private final CSimpleType origin;

	protected CComplexType(CSimpleType origin) {
		this.origin = origin;
	}

	public CSimpleType getOrigin() {
		return origin;
	}

	public LitExpr<?> getNullValue() {
		return this.accept(getNullValueVisitor(), null);
	}

	public Expr<?> getUnitValue() {
		return this.accept(getUnitValueVisitor(), null);
	}

	public AssumeStmt limit(Expr<?> expr) {
		return this.accept(getLimitVisitor(), expr);
	}

	public Expr<?> castTo(Expr<?> expr) {
		Expr<?> accept = this.accept(getCastVisitor(), expr);
		XcfaMetadata.create(accept, "cType", this);
		return accept;
	}

	public Type getSmtType() {
		return this.accept(getTypeVisitor(), null);
	}

	public CComplexType getSmallestCommonType(CComplexType type) {
		throw new RuntimeException("Common type is not applicable for this type!");
	}

	public String getTypeName() {
		throw new RuntimeException("Type name could not be queried from this type!");
	}

	public int width() {
		return ArchitectureConfig.architecture.getBitWidth(getTypeName());
	}

	public static CComplexType getSmallestCommonType(List<CComplexType> types) {
		CComplexType ret = getSignedInt();
		for (int i = 0; i < types.size(); i++) {
			ret = ret.getSmallestCommonType(types.get(i));
		}
		return ret;
	}

	public static CComplexType getType(Expr<?> expr) {
		Optional<Object> cTypeOptional = XcfaMetadata.getMetadataValue(expr,"cType");
		if(cTypeOptional.isPresent() && cTypeOptional.get() instanceof CComplexType) {
			return (CComplexType) cTypeOptional.get();
		} else throw new RuntimeException("Type not known!");
	}

	public static CComplexType getSignedInt() {
		return new CSignedInt(null);
	}
	public static CComplexType getUnsignedLongLong() { return new CUnsignedLongLong(null); }
	public static CComplexType getUnsignedLong() { return new CUnsignedLong(null); }
	public static CComplexType getUnsignedInt() { return new CUnsignedInt(null); }
	public static CComplexType getSignedLongLong() { return new CSignedLongLong(null); }
	public static CComplexType getSignedLong() { return new CSignedLong(null); }
	public static CComplexType getFloat() { return new CFloat(null); }
	public static CComplexType getDouble() { return new CDouble(null); }
	public static CComplexType getLongDouble() { return new CLongDouble(null); }

	public <T, R> R accept(CComplexTypeVisitor<T, R> visitor, T param) {
		return visitor.visit(this, param);
	}

	public static class CComplexTypeVisitor<T, R> {
		public R visit(CComplexType type, T param) {
			throw new UnsupportedOperationException("Not (yet) implemented");
		}
		public R visit(CVoid type, T param) {
			return visit(((CComplexType) type), param);
		}
		public R visit(CReal type, T param) {
			return visit(((CComplexType) type), param);
		}
		public R visit(CDouble type, T param) {
			return visit(((CReal) type), param);
		}
		public R visit(CFloat type, T param) {
			return visit(((CReal) type), param);
		}
		public R visit(CLongDouble type, T param) {
			return visit(((CReal) type), param);
		}
		public R visit(CInteger type, T param) {
			return visit(((CComplexType) type), param);
		}
		public R visit(CShort type, T param) {
			return visit(((CInteger) type), param);
		}
		public R visit(CSignedShort type, T param) {
			return visit(((CShort) type), param);
		}
		public R visit(CUnsignedShort type, T param) {
			return visit(((CShort) type), param);
		}
		public R visit(CLongLong type, T param) {
			return visit(((CInteger) type), param);
		}
		public R visit(CSignedLongLong type, T param) {
			return visit(((CLongLong) type), param);
		}
		public R visit(CUnsignedLongLong type, T param) {
			return visit(((CLongLong) type), param);
		}
		public R visit(CLong type, T param) {
			return visit(((CInteger) type), param);
		}
		public R visit(CUnsignedLong type, T param) {
			return visit(((CLong) type), param);
		}
		public R visit(CSignedLong type, T param) {
			return visit(((CLong) type), param);
		}
		public R visit(CInt type, T param) {
			return visit(((CInteger) type), param);
		}
		public R visit(CSignedInt type, T param) {
			return visit(((CInt) type), param);
		}
		public R visit(CUnsignedInt type, T param) {
			return visit(((CInt) type), param);
		}
		public R visit(CChar type, T param) {
			return visit(((CInteger) type), param);
		}
		public R visit(CSignedChar type, T param) {
			return visit(((CChar) type), param);
		}
		public R visit(CUnsignedChar type, T param) {
			return visit(((CChar) type), param);
		}
		public R visit(CBool type, T param) {
			return visit(((CInteger) type), param);
		}
	}

}
