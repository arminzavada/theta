/*
 *  Copyright 2023 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.core.utils;

import com.google.common.collect.ImmutableList;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.IfStmt;
import hu.bme.mit.theta.core.stmt.LoopStmt;
import hu.bme.mit.theta.core.stmt.NonDetStmt;
import hu.bme.mit.theta.core.stmt.OrtStmt;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.StmtVisitor;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.anytype.Exprs;
import hu.bme.mit.theta.core.type.anytype.IteExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.booltype.SmartBoolExprs;
import hu.bme.mit.theta.core.type.fptype.FpType;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.utils.indexings.VarIndexing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;
import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Geq;
import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Ite;
import static hu.bme.mit.theta.core.type.anytype.Exprs.Prime;
import static hu.bme.mit.theta.core.type.anytype.Exprs.Ref;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Iff;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Or;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.fptype.FpExprs.FpAssign;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Leq;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

final class StmtToExprTransformer {

    private StmtToExprTransformer() {
    }

    static StmtUnfoldResult toExpr(final Stmt stmt, final VarIndexing indexing) {
        return stmt.accept(StmtToExprVisitor.INSTANCE, indexing);
    }

    static StmtUnfoldResult toExpr(final List<? extends Stmt> stmts, final VarIndexing indexing) {
        final Collection<Expr<BoolType>> resultExprs = new ArrayList<>();
        VarIndexing resultIndexing = indexing;

        for (final Stmt stmt : stmts) {
            final StmtUnfoldResult subResult = toExpr(stmt, resultIndexing);
            resultExprs.addAll(subResult.exprs);
            resultIndexing = subResult.indexing;
        }

        return StmtUnfoldResult.of(resultExprs, resultIndexing);
    }

    ////////

    private static class StmtToExprVisitor implements StmtVisitor<VarIndexing, StmtUnfoldResult> {

        private static final StmtToExprVisitor INSTANCE = new StmtToExprVisitor();

        private StmtToExprVisitor() {
        }

        @Override
        public StmtUnfoldResult visit(final SkipStmt stmt, final VarIndexing indexing) {
            return StmtUnfoldResult.of(ImmutableList.of(True()), indexing);
        }

        @Override
        public StmtUnfoldResult visit(final AssumeStmt stmt, final VarIndexing indexing) {
            final Expr<BoolType> cond = stmt.getCond();
            final Expr<BoolType> expr = ExprUtils.applyPrimes(cond, indexing);
            return StmtUnfoldResult.of(ImmutableList.of(expr), indexing);
        }

        @Override
        public <DeclType extends Type> StmtUnfoldResult visit(final HavocStmt<DeclType> stmt,
                                                              final VarIndexing indexing) {
            final VarDecl<?> varDecl = stmt.getVarDecl();
            final VarIndexing newIndexing = indexing.inc(varDecl);
            return StmtUnfoldResult.of(ImmutableList.of(True()), newIndexing);
        }

        @Override
        public <DeclType extends Type> StmtUnfoldResult visit(final AssignStmt<DeclType> stmt,
                                                              final VarIndexing indexing) {
            final VarDecl<DeclType> varDecl = stmt.getVarDecl();
            final VarIndexing newIndexing = indexing.inc(varDecl);
            final Expr<DeclType> rhs = ExprUtils.applyPrimes(stmt.getExpr(), indexing);
            final Expr<DeclType> lhs = ExprUtils.applyPrimes(varDecl.getRef(), newIndexing);

            final Expr<BoolType> expr;
            if (varDecl.getType() instanceof FpType) {
                expr = FpAssign(TypeUtils.cast(lhs, (FpType) varDecl.getType()),
                        TypeUtils.cast(rhs, (FpType) varDecl.getType()));
            } else {
                expr = Eq(lhs, rhs);
            }
            return StmtUnfoldResult.of(ImmutableList.of(expr), newIndexing);
        }

        @Override
        public StmtUnfoldResult visit(SequenceStmt sequenceStmt, VarIndexing indexing) {
            final StmtUnfoldResult result = toExpr(sequenceStmt.getStmts(), indexing);
            return StmtUnfoldResult.of(ImmutableList.of(And(result.getExprs())),
                    result.getIndexing());
        }

        @Override
        public StmtUnfoldResult visit(NonDetStmt nonDetStmt, VarIndexing indexing) {
            final List<Expr<BoolType>> choices = new ArrayList<>();

            final List<VarIndexing> indexings = new ArrayList<>();
            VarIndexing jointIndexing = indexing;
            int count = 0;
            var tempVar = VarPoolUtil.requestInt();
            for (var stmt : nonDetStmt.getStmts()) {
                final Expr<BoolType> tempExpr = Eq(
                        ExprUtils.applyPrimes(tempVar.getRef(), indexing),
                        Int(count++)
                );
                final StmtUnfoldResult result = toExpr(stmt, indexing.inc(tempVar));
                choices.add(And(tempExpr, And(result.exprs)));
                indexings.add(result.indexing);
                jointIndexing = jointIndexing.join(result.indexing);
            }

            final var branchExprs = fixVariablePrimes(choices, indexings, jointIndexing);

            final var choiceExpr = Or(branchExprs);
            final Expr<BoolType> expr;

            if (nonDetStmt.getElze() != null) {
                final var tempExpr = Eq(
                        ExprUtils.applyPrimes(tempVar.getRef(), indexing), Int(count)
                );
                final var result = toExpr(nonDetStmt.getElze(), indexing.inc(tempVar));
                final var elzeIndexing = result.indexing;
                final var elzeExpr = And(tempExpr, And(result.exprs));
                final var elzeJointIndexing = jointIndexing.join(elzeIndexing);

                final var expressions = fixVariablePrimes(
                        ImmutableList.of(choiceExpr, elzeExpr),
                        ImmutableList.of(jointIndexing, elzeIndexing),
                        elzeJointIndexing
                );

                final var choiceExprExtended = expressions.get(0);
                final var elzeExprExtended = expressions.get(1);

                final var choiceVar = VarPoolUtil.requestBool();
                final var iffExpr = Iff(choiceExprExtended, Ref(choiceVar));
                final var iteExpr = IteExpr.of(Ref(choiceVar), Ref(choiceVar), elzeExprExtended);
                final var tempValidExpr = And(
                        Leq(Ref(tempVar), Int(count)),
                        Geq(Ref(tempVar), Int(0))
                );

                expr = And(iffExpr, iteExpr, tempValidExpr);

                VarPoolUtil.returnBool(choiceVar);
            } else {
                expr = choiceExpr;
            }

            VarPoolUtil.returnInt(tempVar);
            return StmtUnfoldResult.of(ImmutableList.of(expr), jointIndexing);
        }

        private static List<Expr<BoolType>> fixVariablePrimes(List<Expr<BoolType>> branches, List<VarIndexing> indexings, VarIndexing jointIndexing) {
            final var vars = ExprUtils.getVars(branches);
            final var branchExprs = new ArrayList<Expr<BoolType>>();
            for (int i = 0; i < branches.size(); i++) {
                final var exprs = new ArrayList<Expr<BoolType>>();
                exprs.add(branches.get(i));
                for (var declaration : vars) {
                    int currentBranchIndex = indexings.get(i).get(declaration);
                    int jointIndex = jointIndexing.get(declaration);
                    if (currentBranchIndex < jointIndex) {
                        if (currentBranchIndex > 0) {
                            exprs.add(Eq(
                                    Prime(declaration.getRef(), currentBranchIndex),
                                    Prime(declaration.getRef(), jointIndex)
                            ));
                        } else {
                            exprs.add(Eq(declaration.getRef(), Prime(declaration.getRef(), jointIndex)));
                        }
                    }
                }
                branchExprs.add(And(exprs));
            }
            return branchExprs;
        }

        @Override
        public StmtUnfoldResult visit(IfStmt ifStmt, VarIndexing indexing) {
            final Expr<BoolType> cond = ifStmt.getCond();
            final Expr<BoolType> condExpr = ExprUtils.applyPrimes(cond, indexing);

            final StmtUnfoldResult thenResult = toExpr(ifStmt.getThen(),
                    indexing.transform().build());
            final StmtUnfoldResult elzeResult = toExpr(ifStmt.getElze(),
                    indexing.transform().build());

            final VarIndexing thenIndexing = thenResult.indexing;
            final VarIndexing elzeIndexing = elzeResult.indexing;

            final Expr<BoolType> thenExpr = And(thenResult.getExprs());
            final Expr<BoolType> elzeExpr = And(elzeResult.getExprs());

            VarIndexing jointIndexing = thenIndexing.join(elzeIndexing);

            final var expressions = fixVariablePrimes(
                    ImmutableList.of(thenExpr, elzeExpr),
                    ImmutableList.of(thenIndexing, elzeIndexing),
                    jointIndexing
            );

            final Expr<BoolType> thenExprExtended = expressions.get(0);
            final Expr<BoolType> elzeExprExtended = expressions.get(1);

            final Expr<BoolType> ite = cast(Ite(condExpr, thenExprExtended, elzeExprExtended), Bool());
            return StmtUnfoldResult.of(ImmutableList.of(ite), jointIndexing);
        }

        @Override
        public StmtUnfoldResult visit(OrtStmt ortStmt, VarIndexing indexing) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StmtUnfoldResult visit(LoopStmt stmt, VarIndexing indexing) {
            throw new UnsupportedOperationException(
                    String.format("Loop statement %s was not unrolled", stmt));
        }
    }
}
