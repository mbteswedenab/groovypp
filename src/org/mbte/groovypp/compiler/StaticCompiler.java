package org.mbte.groovypp.compiler;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.Variable;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.BytecodeImproverMethodAdapter;
import org.mbte.groovypp.compiler.bytecode.LocalVarInferenceTypes;
import org.mbte.groovypp.compiler.bytecode.StackAwareMethodAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StaticCompiler extends CompilerTransformer implements Opcodes {
    private StaticMethodBytecode methodBytecode;

    // exception blocks list
    private List<Runnable> exceptionBlocks = new ArrayList<Runnable>();

    public StaticCompiler(SourceUnit su, StaticMethodBytecode methodBytecode, MethodVisitor mv, CompilerStack compileStack, TypePolicy policy) {
        super(su, methodBytecode.methodNode.getDeclaringClass(), methodBytecode.methodNode, mv, compileStack, policy);
        this.methodBytecode = methodBytecode;
    }

    protected Statement getCode() {
        return methodBytecode.code;
    }

    protected void setCode(Statement statement) {
        methodBytecode.code = statement;
    }

    protected SourceUnit getSourceUnit() {
        return methodBytecode.su;
    }

    private int lastLine = -1;

    @Override
    protected void visitStatement(Statement statement) {
        super.visitStatement(statement);

        int line = statement.getLineNumber();
        if (line >= 0 && mv != null && line != lastLine) {
            Label l = new Label();
            mv.visitLabel(l);
            mv.visitLineNumber(line, l);
            lastLine = line;
        }
    }

    @Override
    public void visitAssertStatement(AssertStatement statement) {
        Label noError = new Label();

        BytecodeExpr condition = transformLogical(statement.getBooleanExpression().getExpression(), noError, true);
        BytecodeExpr msgExpr = (BytecodeExpr) transform(statement.getMessageExpression());

        condition.visit(mv);
        mv.visitTypeInsn(NEW, "java/lang/AssertionError");
        mv.visitInsn(DUP);
        if (msgExpr != null)
            msgExpr.visit(mv);
        else
            mv.visitLdcInsn("<no message>");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V");
        mv.visitInsn(ATHROW);
        mv.visitLabel(noError);
    }

    private static final String DTT = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());

    public static void branch(BytecodeExpr be, int op, Label label, MethodVisitor mv) {
        // type non-primitive
        final ClassNode type = be.getType();

        if (type == ClassHelper.Boolean_TYPE) {
            be.unbox(ClassHelper.boolean_TYPE, mv);
        } else {
            if (ClassHelper.isPrimitiveType(type)) {
                // unwrapper - primitive
                if (type == ClassHelper.byte_TYPE
                        || type == ClassHelper.short_TYPE
                        || type == ClassHelper.char_TYPE
                        || type == ClassHelper.int_TYPE) {
                } else if (type == ClassHelper.long_TYPE) {
                    mv.visitInsn(L2I);
                } else if (type == ClassHelper.float_TYPE) {
                    mv.visitInsn(F2I);
                } else if (type == ClassHelper.double_TYPE) {
                    mv.visitInsn(D2I);
                }
            } else {
                mv.visitMethodInsn(INVOKESTATIC, DTT, "castToBoolean", "(Ljava/lang/Object;)Z");
            }
        }
        mv.visitJumpInsn(op, label);
    }

    @Override
    public void visitBlockStatement(BlockStatement block) {
        compileStack.pushVariableScope(block.getVariableScope());
        super.visitBlockStatement(block);
        compileStack.pop();
    }

    @Override
    public void visitBreakStatement(BreakStatement statement) {
        visitStatement(statement);

        String name = statement.getLabel();
        Label breakLabel = compileStack.getNamedBreakLabel(name);
        compileStack.applyFinallyBlocks(breakLabel, true);

        mv.visitJumpInsn(GOTO, breakLabel);
    }

    @Override
    public void visitExpressionStatement(ExpressionStatement statement) {
        visitStatement(statement);

        super.visitExpressionStatement(statement);

        final BytecodeExpr be = (BytecodeExpr) statement.getExpression();
        be.visit(mv);
        final ClassNode type = be.getType();
        if (type != ClassHelper.VOID_TYPE && type != ClassHelper.void_WRAPPER_TYPE) {
            if (type == ClassHelper.long_TYPE || type == ClassHelper.double_TYPE) {
                mv.visitInsn(POP2);
            } else {
                mv.visitInsn(POP);
            }
        }
    }

    private void visitForLoopWithCollection(ForStatement forLoop) {
        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());

        Variable variable = compileStack.defineVariable(forLoop.getVariable(), false);

        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();
        BytecodeHelper helper = new BytecodeHelper(mv);

        final BytecodeExpr collectionExpression = (BytecodeExpr) transform(forLoop.getCollectionExpression());

        ClassNode etype = ClassHelper.OBJECT_TYPE;
        if (collectionExpression.getType() == TypeUtil.INT_RANGE_TYPE) {
            variable.setType(ClassHelper.Integer_TYPE);
            etype = ClassHelper.Integer_TYPE;
        }

        MethodCallExpression iterator = new MethodCallExpression(
                collectionExpression, "iterator", new ArgumentListExpression());
        BytecodeExpr expr = (BytecodeExpr) transform(iterator);
        expr.visit(mv);

        final int iteratorIdx = compileStack.defineTemporaryVariable(
                "iterator", ClassHelper.make(java.util.Iterator.class), true);

        mv.visitLabel(continueLabel);
        mv.visitVarInsn(ALOAD, iteratorIdx);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
        mv.visitJumpInsn(IFEQ, breakLabel);

        mv.visitVarInsn(ALOAD, iteratorIdx);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
        if (etype != ClassHelper.OBJECT_TYPE)
            BytecodeExpr.checkCast(etype, mv);
        helper.storeVar(variable);

        forLoop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);
        mv.visitLabel(breakLabel);
        compileStack.pop();
    }

    private void visitForLoopWithClosures(ForStatement forLoop) {

        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());

        ClosureListExpression closureExpression = (ClosureListExpression) forLoop.getCollectionExpression();
        compileStack.pushVariableScope(closureExpression.getVariableScope());

        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();
        List<Expression> loopExpr = closureExpression.getExpressions();

        if (!(loopExpr.get(0) instanceof EmptyExpression)) {
            final BytecodeExpr initExpression = (BytecodeExpr) transform(loopExpr.get(0));
            initExpression.visit(mv);
            initExpression.pop(initExpression.getType(), mv);
        }

        Label cond = new Label();
        mv.visitLabel(cond);

        if (!(loopExpr.get(1) instanceof EmptyExpression)) {
            final BytecodeExpr binaryExpression = transformLogical(loopExpr.get(1), breakLabel, false);
            binaryExpression.visit(mv);
        }

        forLoop.getLoopBlock().visit(this);

        mv.visitLabel(continueLabel);

        if (!(loopExpr.get(2) instanceof EmptyExpression)) {
            final BytecodeExpr incrementExpression = (BytecodeExpr) transform(loopExpr.get(2));

            incrementExpression.visit(mv);
            final ClassNode type = incrementExpression.getType();
            if (type != ClassHelper.VOID_TYPE && type != ClassHelper.void_WRAPPER_TYPE) {
                if (type == ClassHelper.long_TYPE || type == ClassHelper.double_TYPE) {
                    mv.visitInsn(POP2);
                } else {
                    mv.visitInsn(POP);
                }
            }
        }

        mv.visitJumpInsn(GOTO, cond);
        mv.visitLabel(breakLabel);

        compileStack.pop();
        compileStack.pop();
    }

    @Override
    public void visitForLoop(ForStatement forLoop) {
        Parameter loopVar = forLoop.getVariable();
        if (loopVar == ForStatement.FOR_LOOP_DUMMY) {
            visitForLoopWithClosures(forLoop);
        } else {
            visitForLoopWithCollection(forLoop);
        }
    }

    @Override
    public void visitIfElse(IfStatement ifElse) {
        visitStatement(ifElse);

        final BooleanExpression ifExpr = ifElse.getBooleanExpression();
        final BytecodeExpr be = (BytecodeExpr) transform(ifExpr.getExpression());
        be.visit(mv);

        Label elseLabel = new Label();
        branch(be, IFEQ, elseLabel, mv);

        compileStack.pushBooleanExpression();
        ifElse.getIfBlock().visit(this);
        compileStack.pop();

        Label endLabel = new Label();
        if (ifElse.getElseBlock() != EmptyStatement.INSTANCE) {
            mv.visitJumpInsn(GOTO, endLabel);
        }

        mv.visitLabel(elseLabel);

        if (ifElse.getElseBlock() != EmptyStatement.INSTANCE) {
            compileStack.pushBooleanExpression();
            ifElse.getElseBlock().visit(this);
            compileStack.pop();

            mv.visitLabel(endLabel);
        }
    }

    @Override
    public void visitReturnStatement(ReturnStatement statement) {
        visitStatement(statement);

        super.visitReturnStatement(statement);

        final BytecodeExpr bytecodeExpr = (BytecodeExpr) statement.getExpression();
        bytecodeExpr.visit(mv);
        final ClassNode exprType = bytecodeExpr.getType();
        final ClassNode returnType = methodNode.getReturnType();
        if (returnType.equals(ClassHelper.VOID_TYPE)) {
            compileStack.applyFinallyBlocks();
        } else {
            if (bytecodeExpr.getType().equals(ClassHelper.VOID_TYPE)) {
                mv.visitInsn(ACONST_NULL);
            } else {
                bytecodeExpr.box(exprType, mv);
                bytecodeExpr.cast(TypeUtil.wrapSafely(exprType), TypeUtil.wrapSafely(returnType), mv);
            }

            if (compileStack.hasFinallyBlocks()) {
                int returnValueIdx = compileStack.defineTemporaryVariable("returnValue", ClassHelper.OBJECT_TYPE, true);
                compileStack.applyFinallyBlocks();
                mv.visitVarInsn(ALOAD, returnValueIdx);
            }
            bytecodeExpr.unbox(returnType, mv);
        }
        bytecodeExpr.doReturn(returnType, mv);
    }

    @Override
    public void visitWhileLoop(WhileStatement loop) {
        compileStack.pushLoop(loop.getStatementLabel());
        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();

        final BytecodeExpr be = transformLogical(loop.getBooleanExpression().getExpression(), breakLabel, false);

        mv.visitLabel(continueLabel);
        be.visit(mv);

        loop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);
        mv.visitLabel(breakLabel);

        compileStack.pop();
    }

    public void visitSwitch(SwitchStatement statement) {
        visitStatement(statement);

        BytecodeExpr cond = (BytecodeExpr) transform(statement.getExpression());
        cond.visit(mv);
        if (ClassHelper.isPrimitiveType(cond.getType()))
            cond.box(cond.getType(), mv);

        // switch does not have a continue label. use its parent's for continue
        Label breakLabel = compileStack.pushSwitch();

        int switchVariableIndex = compileStack.defineTemporaryVariable("switch", true);

        List caseStatements = statement.getCaseStatements();
        int caseCount = caseStatements.size();
        Label[] codeLabels = new Label[caseCount];
        Label[] condLabels = new Label[caseCount + 1];
        int i;
        for (i = 0; i < caseCount; i++) {
            codeLabels[i] = new Label();
            condLabels[i] = new Label();
        }

        Label defaultLabel = new Label();

        i = 0;
        for (Iterator iter = caseStatements.iterator(); iter.hasNext(); i++) {
            CaseStatement caseStatement = (CaseStatement) iter.next();

            mv.visitLabel(condLabels[i]);

            visitStatement(caseStatement);

            mv.visitVarInsn(ALOAD, switchVariableIndex);
            final BytecodeExpr option = (BytecodeExpr) transform(caseStatement.getExpression());
            option.visit(mv);
            if (ClassHelper.isPrimitiveType(option.getType()))
                option.box(option.getType(), mv);

            Label next = i == caseCount - 1 ? defaultLabel : condLabels[i + 1];

            Label notNull = new Label();
            mv.visitInsn(DUP);
            mv.visitJumpInsn(IFNONNULL, notNull);
            mv.visitJumpInsn(IF_ACMPEQ, codeLabels[i]);
            mv.visitJumpInsn(GOTO, next);

            mv.visitLabel(notNull);

            final BytecodeExpr caseValue = new BytecodeExpr(option, TypeUtil.wrapSafely(option.getType())) {
                protected void compile(MethodVisitor mv) {
                }
            };

            final BytecodeExpr switchValue = new BytecodeExpr(cond, TypeUtil.wrapSafely(cond.getType())) {
                protected void compile(MethodVisitor mv) {
                    mv.visitInsn(SWAP);
                }
            };
            MethodCallExpression exp = new MethodCallExpression(caseValue, "isCase", new ArgumentListExpression(switchValue));
            exp.setSourcePosition(caseValue);
            transformLogical(exp, codeLabels[i], true).visit(mv);
        }

        mv.visitJumpInsn(GOTO, defaultLabel);

        i = 0;
        for (Iterator iter = caseStatements.iterator(); iter.hasNext(); i++) {
            CaseStatement caseStatement = (CaseStatement) iter.next();
            visitStatement(caseStatement);
            mv.visitLabel(codeLabels[i]);
            caseStatement.getCode().visit(this);
        }

        mv.visitLabel(defaultLabel);
        statement.getDefaultStatement().visit(this);

        mv.visitLabel(breakLabel);

        compileStack.pop();
    }

    @Override
    public void visitCaseStatement(CaseStatement statement) {
    }

    @Override
    public void visitDoWhileLoop(DoWhileStatement loop) {
        visitStatement(loop);

        super.visitDoWhileLoop(loop);
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatement sync) {
        visitStatement(sync);

        super.visitSynchronizedStatement(sync);

        ((BytecodeExpr) sync.getExpression()).visit(mv);
        final int index = compileStack.defineTemporaryVariable("synchronized", ClassHelper.OBJECT_TYPE, true);

        final Label synchronizedStart = new Label();
        final Label synchronizedEnd = new Label();
        final Label catchAll = new Label();

        mv.visitVarInsn(ALOAD, index);
        mv.visitInsn(MONITORENTER);
        mv.visitLabel(synchronizedStart);

        Runnable finallyPart = new Runnable() {
            public void run() {
                mv.visitVarInsn(ALOAD, index);
                mv.visitInsn(MONITOREXIT);
            }
        };
        compileStack.pushFinallyBlock(finallyPart);

        sync.getCode().visit(this);

        finallyPart.run();
        mv.visitJumpInsn(GOTO, synchronizedEnd);
        ((StackAwareMethodAdapter) mv).startExceptionBlock(); // exception variable
        mv.visitLabel(catchAll);
        finallyPart.run();
        mv.visitInsn(ATHROW);
        mv.visitLabel(synchronizedEnd);

        compileStack.popFinallyBlock();
        exceptionBlocks.add(new Runnable() {
            public void run() {
                mv.visitTryCatchBlock(synchronizedStart, catchAll, catchAll, null);
            }
        });
    }

    @Override
    public void visitThrowStatement(ThrowStatement ts) {
        visitStatement(ts);

        super.visitThrowStatement(ts);
        ((BytecodeExpr) ts.getExpression()).visit(mv);
        mv.visitInsn(ATHROW);
    }

    public void visitContinueStatement(ContinueStatement statement) {
        visitStatement(statement);

        String name = statement.getLabel();
        Label continueLabel = compileStack.getContinueLabel();
        if (name != null) continueLabel = compileStack.getNamedContinueLabel(name);
        compileStack.applyFinallyBlocks(continueLabel, false);
        mv.visitJumpInsn(GOTO, continueLabel);
    }

    public void visitTryCatchFinally(TryCatchStatement statement) {
        visitStatement(statement);

        Statement tryStatement = statement.getTryStatement();
        final Statement finallyStatement = statement.getFinallyStatement();

        int anyExceptionIndex = compileStack.defineTemporaryVariable("exception", false);
        if (!finallyStatement.isEmpty()) {
            compileStack.pushFinallyBlock(
                    new Runnable() {
                        public void run() {
                            compileStack.pushFinallyBlockVisit(this);
                            finallyStatement.visit(StaticCompiler.this);
                            compileStack.popFinallyBlockVisit(this);
                        }
                    }
            );
        }

        // start try block, label needed for exception table
        final Label tryStart = new Label();
        mv.visitLabel(tryStart);
        tryStatement.visit(this);

        // goto finally part
        final Label finallyStart = new Label();
        mv.visitJumpInsn(GOTO, finallyStart);

        // marker needed for Exception table
        final Label greEnd = new Label();
        mv.visitLabel(greEnd);

        final Label tryEnd = new Label();
        mv.visitLabel(tryEnd);

        for (CatchStatement catchStatement : statement.getCatchStatements()) {
            ClassNode exceptionType = catchStatement.getExceptionType();
            // start catch block, label needed for exception table
            final Label catchStart = new Label();
            mv.visitLabel(catchStart);

            ((StackAwareMethodAdapter) mv).startExceptionBlock();

            // create exception variable and store the exception
            compileStack.pushState();
            compileStack.defineVariable(catchStatement.getVariable(), true);
            // handle catch body
            catchStatement.visit(this);
            compileStack.pop();
            // goto finally start
            mv.visitJumpInsn(GOTO, finallyStart);
            // add exception to table
            final String exceptionTypeInternalName = BytecodeHelper.getClassInternalName(exceptionType);
            exceptionBlocks.add(new Runnable() {
                public void run() {
                    mv.visitTryCatchBlock(tryStart, tryEnd, catchStart, exceptionTypeInternalName);
                }
            });
        }

        // marker needed for the exception table
        final Label endOfAllCatches = new Label();
        mv.visitLabel(endOfAllCatches);

        // remove the finally, don't let it visit itself
        if (!finallyStatement.isEmpty()) compileStack.popFinallyBlock();

        // start finally
        mv.visitLabel(finallyStart);
        finallyStatement.visit(this);
        // goto end of finally
        Label afterFinally = new Label();
        mv.visitJumpInsn(GOTO, afterFinally);

        // start a block catching any Exception
        final Label catchAny = new Label();
        mv.visitLabel(catchAny);
        ((StackAwareMethodAdapter) mv).startExceptionBlock();
        //store exception
        mv.visitVarInsn(ASTORE, anyExceptionIndex);
        finallyStatement.visit(this);
        // load the exception and rethrow it
        mv.visitVarInsn(ALOAD, anyExceptionIndex);
        mv.visitInsn(ATHROW);

        // end of all catches and finally parts
        mv.visitLabel(afterFinally);
        mv.visitInsn(NOP);

        // add catch any block to exception table
        exceptionBlocks.add(new Runnable() {
            public void run() {
                mv.visitTryCatchBlock(tryStart, endOfAllCatches, catchAny, null);
            }
        });
    }

    public void execute() {
        addReturnIfNeeded();
        compileStack.init(methodNode.getVariableScope(), methodNode.getParameters(), mv, methodNode.getDeclaringClass());
        getCode().visit(this);
        compileStack.clear();
        for (Runnable runnable : exceptionBlocks) {
            runnable.run();
        }
    }

    public void visitBytecodeSequence(BytecodeSequence sequence) {
        visitStatement(sequence);

        ((BytecodeInstruction) sequence.getInstructions().get(0)).visit(mv);
    }

    public LocalVarInferenceTypes getLocalVarInferenceTypes() {
        return ((BytecodeImproverMethodAdapter) mv).getLocalVarInferenceTypes();
    }
}
