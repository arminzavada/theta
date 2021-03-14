package hu.bme.mit.theta.xcfa.ir;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.Tuple3;
import hu.bme.mit.theta.common.Tuple4;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.stmt.Stmts;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntEqExpr;
import hu.bme.mit.theta.core.type.inttype.IntLitExpr;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.dsl.CallStmt;

import java.math.BigInteger;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static hu.bme.mit.theta.core.stmt.Stmts.Assign;
import static hu.bme.mit.theta.core.stmt.Stmts.Assume;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.*;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;
import static hu.bme.mit.theta.xcfa.ir.Utils.*;

public class NaiveInstructionHandler implements InstructionHandler{
    private XCFA.Process.Procedure.Location lastLoc;
    private Map<String, Expr<?>> valueLut;
    private Integer cnt;
    private final Tuple3<String, java.util.Optional<String>, List<Tuple2<String, String>>> function;
    private final XCFA.Process.Procedure.Builder procedureBuilder;
    private final SSAProvider ssa;
    private final Collection<String> processes;
    private final Map<String, VarDecl<?>> localVarLut;
    private final hu.bme.mit.theta.xcfa.XCFA.Process.Procedure.Location finalLoc;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<VarDecl<? extends Type>> retVar;
    private final Map<String, XCFA.Process.Procedure.Location> locationLut;
    private String block;


    private final Map<Tuple2<String, String>, Tuple3<XCFA.Process.Procedure.Location, XCFA.Process.Procedure.Location, List<Stmt>>> terminatorEdges = new HashMap<>();

    public NaiveInstructionHandler(Tuple3<String, Optional<String>, List<Tuple2<String, String>>> function, XCFA.Process.Procedure.Builder procedureBuilder, SSAProvider ssa, Collection<String> processes, Map<String, VarDecl<?>> localVarLut, XCFA.Process.Procedure.Location finalLoc, Optional<VarDecl<? extends Type>> retVar, Map<String, XCFA.Process.Procedure.Location> locationLut) {
        this.function = function;
        this.procedureBuilder = procedureBuilder;
        this.ssa = ssa;
        this.processes = processes;
        this.localVarLut = localVarLut;
        this.finalLoc = finalLoc;
        this.retVar = retVar;
        this.locationLut = locationLut;
    }

    @Override
    public void reinitClass(String block) {
        this.block = block;
        valueLut = new HashMap<>();
        cnt = 0;
        lastLoc = locationLut.get(block);
    }

    @Override
    public void handleInstruction(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        checkState(!(lastLoc.isEndLoc() || lastLoc.isErrorLoc()), "No instruction can occur after a final or error location!");
        switch(instruction.get1()) {
            case "ret":
                ret(instruction);
                break;
            case "br":
                br(instruction);
                break;
            case "switch":
                sw(instruction);
                break;
            case "add":
                add(instruction);
                break;
            case "sub":
                sub(instruction);
                break;
            case "mul":
                mul(instruction);
                break;
            case "sdiv":
                div(instruction);
                break;
            case "srem":
                rem(instruction);
                break;
            case "alloca":
                alloca(instruction);
                break;
            case "load":
                load(instruction);
                break;
            case "store":
                store(instruction);
                break;
            case "icmp":
                cmp(valueLut, instruction);
                break;
            case "phi":
                phi(instruction);
                break;
            case "call":
                call(instruction);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + instruction.get1());
        }
    }


    private void call(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        int paramSize = instruction.get3().size();
        String funcName = instruction.get3().get(paramSize - 2).get2();
//        switch(funcName) {
//            default:
                System.out.println("Function call");
                if(instruction.get2().isPresent()) System.out.println("\tResVar: " + instruction.get2().get());
                System.out.println("\tName: " + funcName);
                System.out.println("\tParams: ");
                for(int i = 0 ; i < paramSize - 1; ++i) System.out.println("\t\t" + instruction.get3().get(i));
//                break;
//        }
    }

    /*
     * var = phi [expr label]*
     */
    private void phi(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        checkState(instruction.get3().size() % 2 == 0, "Phi node should have an even number of arguments");
        checkState(instruction.get2().isPresent(), "Return var must be present!");
        VarDecl<?> phiVar = createVariable(block +"_"+ cnt++, instruction.get2().get().get1());
        procedureBuilder.getLocalVars().put(phiVar, null);
        localVarLut.put(phiVar.getName(), phiVar);
        valueLut.put(instruction.get2().get().get2(), phiVar.getRef());
        for(int i = 0; i < (instruction.get3().size()) / 2; ++i) {
            String blockName = instruction.get3().get(2*i).get2();
            Expr<?> value = getExpr(instruction, 2*i + 1);
            checkState(terminatorEdges.containsKey(Tuple2.of(block, blockName)), "Edge does not exist!");
            terminatorEdges.get(Tuple2.of(block, blockName)).get3().add(Assign(phiVar, value));
        }
    }

    /*
     * var = cmp expr expr
     */
    private void cmp(Map<String, Expr<?>> valueLut, Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        int paramSize = instruction.get3().size();
        Expr<?> lhsExpr = getExpr(instruction, paramSize - 2);
        Expr<?> rhsExpr = getExpr(instruction, paramSize - 1);

        checkState(lhsExpr.getType() == IntType.getInstance(), "Cmp must compare integer types!");
        checkState(rhsExpr.getType() == IntType.getInstance(), "Cmp must compare integer types!");

        //noinspection unchecked
        Expr<IntType> lhs = (Expr<IntType>) lhsExpr;
        //noinspection unchecked
        Expr<IntType> rhs = (Expr<IntType>) rhsExpr;

        checkState(instruction.get2().isPresent(), "Instruction must have return variable");
        switch(instruction.get3().get(0).get2()) {
            case "eq": valueLut.put(instruction.get2().get().get2(), Eq(lhs, rhs)); break;
            case "ne": valueLut.put(instruction.get2().get().get2(), Neq(lhs, rhs)); break;
            case "ugt": case "sgt": valueLut.put(instruction.get2().get().get2(), Gt(lhs, rhs)); break;
            case "uge": case "sge": valueLut.put(instruction.get2().get().get2(), Geq(lhs, rhs)); break;
            case "ult": case "slt": valueLut.put(instruction.get2().get().get2(), Lt(lhs, rhs)); break;
            case "ule": case "sle": valueLut.put(instruction.get2().get().get2(), Leq(lhs, rhs)); break;
            default:
                throw new IllegalStateException("Unexpected value: " + instruction.get3().get(0).get2());
        }

    }


    /*
     * store expr expr
     */
    private void store(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        int paramSize = instruction.get3().size();
        checkState(paramSize == 2, "Store should have two arguments");
        XCFA.Process.Procedure.Location loc = new XCFA.Process.Procedure.Location(block + "_" + cnt++, new HashMap<>());
        Stmt stmt = Assign(localVarLut.get(instruction.get3().get(0).get2()), getExpr(instruction, paramSize - 1));
        XCFA.Process.Procedure.Edge edge = new XCFA.Process.Procedure.Edge(lastLoc, loc, List.of(stmt));
        procedureBuilder.addLoc(loc);
        procedureBuilder.addEdge(edge);
        lastLoc = loc;
    }

    /*
     * var = load expr
     */
    private void load(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        checkState(instruction.get2().isPresent(), "Load must load into a variable");
        valueLut.put(instruction.get2().get().get2(), localVarLut.get(instruction.get3().get(0).get2()).getRef());
    }

    /*
     * var = alloca
     */
    private void alloca(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        checkState(instruction.get2().isPresent(), "Alloca must have a variable tied to it");
        VarDecl<?> var = createVariable(instruction.get2().get().get2(), instruction.get2().get().get1());
        procedureBuilder.getLocalVars().put(var, null);
        localVarLut.put(instruction.get2().get().get2(), var);
        valueLut.put(instruction.get2().get().get2(), var.getRef());
    }

    /*
     * var = rem expr expr
     * var : int
     * expr : int
     */
    private void rem(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        int paramSize = instruction.get3().size();
        Expr<?> lhs = getExpr(instruction, paramSize - 2);
        Expr<?> rhs = getExpr(instruction, paramSize - 1);

        checkState(lhs.getType() == IntType.getInstance(), "Rem only supports integer types!");
        checkState(rhs.getType() == IntType.getInstance(), "Rem only supports integer types!");
        checkState(instruction.get2().isPresent(), "Instruction must have return variable");
        //noinspection unchecked
        valueLut.put(instruction.get2().get().get2(), Rem((Expr<IntType>) lhs, (Expr<IntType>) rhs));
    }

    /*
     * var = div expr expr
     * var : int
     * expr : int
     */
    private void div(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        int paramSize = instruction.get3().size();
        Expr<?> lhs = getExpr(instruction, paramSize - 2);
        Expr<?> rhs = getExpr(instruction, paramSize - 1);

        checkState(lhs.getType() == IntType.getInstance(), "Div only supports integer types!");
        checkState(rhs.getType() == IntType.getInstance(), "Div only supports integer types!");
        checkState(instruction.get2().isPresent(), "Instruction must have return variable");
        //noinspection unchecked
        valueLut.put(instruction.get2().get().get2(), Div((Expr<IntType>) lhs, (Expr<IntType>) rhs));
    }

    /*
     * var = mul expr expr
     * var : int
     * expr : int
     */
    private void mul(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        int paramSize = instruction.get3().size();
        Expr<?> lhs = getExpr(instruction, paramSize - 2);
        Expr<?> rhs = getExpr(instruction, paramSize - 1);

        checkState(lhs.getType() == IntType.getInstance(), "Mul only supports integer types!");
        checkState(rhs.getType() == IntType.getInstance(), "Mul only supports integer types!");
        checkState(instruction.get2().isPresent(), "Instruction must have return variable");
        //noinspection unchecked
        valueLut.put(instruction.get2().get().get2(), Mul((Expr<IntType>) lhs, (Expr<IntType>) rhs));
    }

    /*
     * var = sub expr expr
     * var : int
     * expr : int
     */
    private void sub(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        int paramSize = instruction.get3().size();
        Expr<?> lhs = getExpr(instruction, paramSize - 2);
        Expr<?> rhs = getExpr(instruction, paramSize - 1);

        checkState(lhs.getType() == IntType.getInstance(), "Sub only supports integer types!");
        checkState(rhs.getType() == IntType.getInstance(), "Sub only supports integer types!");
        checkState(instruction.get2().isPresent(), "Instruction must have return variable");
        //noinspection unchecked
        valueLut.put(instruction.get2().get().get2(), Sub((Expr<IntType>) lhs, (Expr<IntType>) rhs));
    }

    /*
     * var = add expr expr
     * var : int
     * expr : int
     */
    private void add(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        int paramSize = instruction.get3().size();
        Expr<?> lhs = getExpr(instruction, paramSize - 2);
        Expr<?> rhs = getExpr(instruction, paramSize - 1);

        checkState(lhs.getType() == IntType.getInstance(), "Add only supports integer types!");
        checkState(rhs.getType() == IntType.getInstance(), "Add only supports integer types!");
        checkState(instruction.get2().isPresent(), "Instruction must have return variable");
        //noinspection unchecked
        valueLut.put(instruction.get2().get().get2(), Add((Expr<IntType>) lhs, (Expr<IntType>) rhs));
    }

    /*
     * sw var label [const label]*
     * TODO: 3rd param?
     * var: int
     * const: int
     */
    private void sw(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        checkState(instruction.get3().size() % 2 == 0, "Switch has wrong number of arguments");
        Expr<?> varExpr = getExpr(instruction, 0);
        checkState(varExpr.getType() == IntType.getInstance(), "Var has to be an integer!");
        //noinspection unchecked
        Expr<IntType> var = (Expr<IntType>) varExpr;
        Expr<BoolType> defaultBranch = null;
        for (int i = 0; i < (instruction.get3().size() / 2) - 1; ++i) {
            XCFA.Process.Procedure.Location loc = locationLut.get(instruction.get3().get(2 + 2*i + 1).get2());
            Expr<?> constExpr = getExpr(instruction, 2 + 2 * i);
            checkState(constExpr.getType() == IntType.getInstance(), "Constant has to be an integer!");
            //noinspection unchecked
            IntEqExpr eq = Eq(var, (Expr<IntType>) constExpr);
            if(defaultBranch == null) defaultBranch = eq;
            else defaultBranch = BoolExprs.Or(defaultBranch, eq);
            AssumeStmt assume = Assume(eq);
            terminatorEdges.put(Tuple2.of(block, loc.getName()), Tuple3.of(lastLoc, loc, new ArrayList<>(List.of(assume))));
        }
        XCFA.Process.Procedure.Location loc = locationLut.get(instruction.get3().get(1).get2());
        XCFA.Process.Procedure.Edge edge = new XCFA.Process.Procedure.Edge(lastLoc, loc, List.of(Assume(BoolExprs.Not(defaultBranch))));
        procedureBuilder.addEdge(edge);
        lastLoc = finalLoc;
    }

    /*
     * br label;
     * br expr label label;
     */
    private void br(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        switch(instruction.get3().size()) {
            case 1:
                XCFA.Process.Procedure.Location loc = locationLut.get(instruction.get3().get(0).get2());
                XCFA.Process.Procedure.Edge edge = new XCFA.Process.Procedure.Edge(lastLoc, loc, new ArrayList<>());
                procedureBuilder.addEdge(edge);
                break;
            case 3:
                XCFA.Process.Procedure.Location loc1 = locationLut.get(instruction.get3().get(1).get2());
                XCFA.Process.Procedure.Location loc2 = locationLut.get(instruction.get3().get(2).get2());

                Expr<?> lhs = getExpr(instruction, 0);
                Expr<?> rhs = createConstant("i32 0");
                checkState(lhs.getType() == rhs.getType() && rhs.getType() == IntType.getInstance(), "Both expressions should be int types!");
                //noinspection unchecked
                AssumeStmt assume1 = Assume(Neq((Expr<IntType>) lhs, (Expr<IntType>) rhs));
                //noinspection unchecked
                AssumeStmt assume2 = Assume(Eq((Expr<IntType>) lhs, (Expr<IntType>) rhs));
                terminatorEdges.put(Tuple2.of(block, loc1.getName()), Tuple3.of(lastLoc, loc1, new ArrayList<>(List.of(assume1))));
                terminatorEdges.put(Tuple2.of(block, loc2.getName()), Tuple3.of(lastLoc, loc2, new ArrayList<>(List.of(assume2))));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + instruction.get3().size());
        }
        lastLoc = finalLoc;
    }

    /*
     * ret;
     * ret expr;
     */
    private void ret(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction) {
        List<Stmt> stmts = new ArrayList<>();
        switch(instruction.get3().size()) {
            case 0: checkState(retVar.isEmpty(), "Not returning a value from non-void function!"); break;
            case 1:
                checkState(retVar.isPresent(), "Returning a value from void function!");
                Stmt assignStmt = Assign(retVar.get(), cast(getExpr(instruction, 0), retVar.get().getType()));
                stmts.add(assignStmt);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + instruction.get3().size());
        }
        XCFA.Process.Procedure.Edge edge = new XCFA.Process.Procedure.Edge(lastLoc, finalLoc, stmts);
        procedureBuilder.addEdge(edge);
        lastLoc = finalLoc;
    }

    private <T extends Type> Stmt Assign(VarDecl<T> varDecl, Expr<? extends Type> expr) {
        checkState(varDecl.getType() == expr.getType(), "Cannot assign different types of expressions!");
        //noinspection unchecked
        return Stmts.Assign(varDecl, (Expr<T>) expr);
    }

    private Expr<? extends Type> getExpr(Tuple4<String, Optional<Tuple2<String, String>>, List<Tuple2<Optional<String>, String>>, Integer> instruction, int i) {
        Expr<? extends Type> expr;
        Tuple2<Optional<String>, String> param1 = instruction.get3().get(i);
        if (param1.get1().isEmpty()) {
            expr = createConstant(param1.get2());
        } else {
            expr = valueLut.get(param1.get2());
        }
        return expr;
    }

}
