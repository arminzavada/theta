package hu.bme.mit.inf.theta.core.expr;

import hu.bme.mit.inf.theta.core.decl.ParamDecl;
import hu.bme.mit.inf.theta.core.type.FuncType;
import hu.bme.mit.inf.theta.core.type.Type;

public interface FuncLitExpr<ParamType extends Type, ResultType extends Type> extends LitExpr<FuncType<ParamType, ResultType>> {

	public ParamDecl<? super ParamType> getParamDecl();

	public Expr<? extends ResultType> getResult();

}