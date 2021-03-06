/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class ClosureUtil {
    private static final LinkedList<MethodNode> NONE = new LinkedList<MethodNode> ();

    private static boolean likeGetter(MethodNode method) {
        return method.getName().startsWith("get")
                && ClassHelper.VOID_TYPE != method.getReturnType()
                && method.getParameters().length == 0;
    }

    private static boolean likeSetter(MethodNode method) {
        return method.getName().startsWith("set")
                && ClassHelper.VOID_TYPE == method.getReturnType()
                && method.getParameters().length == 1;
    }

    public synchronized static List<MethodNode> isOneMethodAbstract (ClassNode node) {
        if ((node.getModifiers() & Opcodes.ACC_ABSTRACT) == 0 && !node.isInterface())
            return null;

        final ClassNodeCache.ClassNodeInfo info = ClassNodeCache.getClassNodeInfo(node);
        if (info.isOneMethodAbstract == null) {
            List<MethodNode> am = node.getAbstractMethods();

            if (am == null) {
                am = Collections.emptyList();
            }

            MethodNode one = null;
            for (Iterator<MethodNode> it = am.iterator(); it.hasNext();) {
                MethodNode mn = it.next();
                if (!likeGetter(mn) && !likeSetter(mn) && !traitMethod(mn) && !objectMethod(mn)) {
                    if (one != null) {
                        info.isOneMethodAbstract = NONE;
                        return null;
                    }
                    one = mn;
                    it.remove();
                }
            }

            if (one != null)
                am.add(0, one);
            else
                if (am.size() != 1) {
                    info.isOneMethodAbstract = NONE;
                    return null;
                }

            info.isOneMethodAbstract = am;
        }

        if (info.isOneMethodAbstract == NONE)
            return null;

        return info.isOneMethodAbstract;
    }

    private static boolean objectMethod(MethodNode mn) {
        return mn.getName().equals("equals") && mn.getReturnType().equals(ClassHelper.boolean_TYPE) && mn.getParameters() != null && mn.getParameters().length == 1
             ||mn.getName().equals("clone") && mn.getParameters() != null && mn.getParameters().length == 0;
    }

    private static boolean traitMethod(MethodNode mn) {
        return !mn.getAnnotations(TypeUtil.HAS_DEFAULT_IMPLEMENTATION).isEmpty();
    }

    public static MethodNode isMatch(List<MethodNode> one, ClosureClassNode closureType, ClassNode baseType, CompilerTransformer compiler) {
        class Mutation {
            final Parameter p;
            final ClassNode t;

            public Mutation(ClassNode t, Parameter p) {
                this.t = t;
                this.p = p;
            }

            void mutate () {
                p.setType(t);
            }
        }

        if(one == null)
            return null;

        List<Mutation> mutations = null;

        MethodNode missing = one.get(0);
        Parameter[] missingMethodParameters = missing.getParameters();
        List<MethodNode> methods = closureType.getDeclaredMethods("doCall");

        for (MethodNode method : methods) {
            
            Parameter[] closureParameters = method.getParameters();

            if (closureParameters.length != missingMethodParameters.length)
                continue;

            boolean match = true;
            for (int i = 0; i < closureParameters.length; i++) {
                Parameter closureParameter = closureParameters[i];
                Parameter missingMethodParameter = missingMethodParameters[i];

                ClassNode parameterType = missingMethodParameter.getType();
                parameterType = TypeUtil.getSubstitutedType(parameterType, missing.getDeclaringClass().redirect(), baseType);
                if (!TypeUtil.isDirectlyAssignableFrom(TypeUtil.wrapSafely(parameterType), TypeUtil.wrapSafely(closureParameter.getType()))) {
                    if (TypeUtil.isDirectlyAssignableFrom(TypeUtil.wrapSafely(closureParameter.getType()), TypeUtil.wrapSafely(parameterType))) {
                        if (mutations == null)
                            mutations = new LinkedList<Mutation> ();
                        mutations.add(new Mutation(parameterType, closureParameter));
                        continue;
                    }

                    match = false;
                    break;
                }
            }

            if (match) {
                if (mutations != null)
                    for (Mutation mutation : mutations) {
                        mutation.mutate();
                    }

                improveClosureType(closureType, baseType);
                if(!missing.getReturnType().equals(ClassHelper.VOID_TYPE)) {
                    if(ClassHelper.isPrimitiveType(missing.getReturnType())) {
                        if (method instanceof ClosureMethodNode.Dependent)
                            ((ClosureMethodNode.Dependent)method).getMaster().setReturnType(missing.getReturnType());
                        else
                            method.setReturnType(missing.getReturnType());
                    }
                    else {
                        ClassNode returnType = TypeUtil.wrapSafely(missing.getReturnType());
                        ClassNode ret = TypeUtil.getSubstitutedType(returnType, missing.getDeclaringClass(), baseType);
                        if ((returnType.equals(ret) || !returnType.isDerivedFrom(ret)) && !returnType.implementsInterface(ret)) {
                            returnType = ret;
                        }

                        if(returnType.equals(ClassHelper.OBJECT_TYPE))
                            returnType = TypeUtil.wrapSafely(missing.getReturnType());
                        if (method instanceof ClosureMethodNode.Dependent)
                            ((ClosureMethodNode.Dependent)method).getMaster().setReturnType(returnType);
                        else
                            method.setReturnType(returnType);
                    }
                }

                ArrayList<MethodNode> toCompile = new ArrayList<MethodNode>();
                for(MethodNode mn: closureType.getMethods()) {
                    if(mn.getReturnType().equals(TypeUtil.IMPROVE_TYPE)) {
                        toCompile.add(mn);
                    }
                    else {
                        if(!mn.getAnnotations(TypeUtil.IMPROVED_TYPES).isEmpty()) {
                            toCompile.add(mn);
                        }
                    }
                }
                for(MethodNode mn : toCompile) {
                    compiler.replaceMethodCode(closureType, mn);
                }
                compiler.replaceMethodCode(closureType, method);
                makeOneMethodClass(one, closureType, baseType, compiler, method);
                return method;
            }
        }
        return null;
    }

    public static Parameter[] eraseParameterTypes(Parameter[] parameters) {
        final Parameter[] ret = new Parameter[parameters.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new Parameter(TypeUtil.eraseTypeArguments(parameters[i].getType()), parameters[i].getName());
        }
        return ret;
    }

    private static void makeOneMethodClass(List<MethodNode> abstractMethods, final ClassNode closureType, ClassNode baseType, CompilerTransformer compiler, final MethodNode doCall) {
        boolean traitMethods = false;
        int k = 0;
        for (final MethodNode missed : abstractMethods) {
            final Parameter[] parameters = eraseParameterTypes(missed.getParameters());
            if (k == 0) {
               closureType.addMethod(
                    missed.getName(),
                    Opcodes.ACC_PUBLIC,
                    getSubstitutedReturnType(doCall, missed, closureType, baseType),
                    parameters,
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(
                            new BytecodeInstruction() {
                                public void visit(MethodVisitor mv) {
                                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                                    Parameter pp[] = parameters;
                                    for (int i = 0, k = 1; i != pp.length; ++i) {
                                        final ClassNode type = pp[i].getType();
                                        ClassNode expectedType = doCall.getParameters()[i].getType();
                                        if (ClassHelper.isPrimitiveType(type)) {
                                            if (type == ClassHelper.long_TYPE) {
                                                mv.visitVarInsn(Opcodes.LLOAD, k++);
                                                k++;
                                            } else if (type == ClassHelper.double_TYPE) {
                                                mv.visitVarInsn(Opcodes.DLOAD, k++);
                                                k++;
                                            } else if (type == ClassHelper.float_TYPE) {
                                                mv.visitVarInsn(Opcodes.FLOAD, k++);
                                            } else {
                                                mv.visitVarInsn(Opcodes.ILOAD, k++);
                                            }
                                            BytecodeExpr.box(type, mv);
                                            BytecodeExpr.cast(TypeUtil.wrapSafely(type), TypeUtil.wrapSafely(expectedType), mv);
                                        } else {
                                            mv.visitVarInsn(Opcodes.ALOAD, k++);
                                            BytecodeExpr.checkCast(TypeUtil.wrapSafely(expectedType), mv);
                                        }
                                        BytecodeExpr.unbox(expectedType, mv);
                                    }
                                    mv.visitMethodInsn(
                                            Opcodes.INVOKEVIRTUAL,
                                            BytecodeHelper.getClassInternalName(doCall.getDeclaringClass()),
                                            doCall.getName(),
                                            BytecodeHelper.getMethodDescriptor(doCall.getReturnType(), doCall.getParameters())
                                    );

                                    if (missed.getReturnType() != ClassHelper.VOID_TYPE && !missed.getReturnType().equals(doCall.getReturnType())) {
                                        BytecodeExpr.box(doCall.getReturnType(), mv);
                                        BytecodeExpr.checkCast(TypeUtil.wrapSafely(doCall.getReturnType()), mv);
                                        BytecodeExpr.unbox(missed.getReturnType(), mv);
                                    }
                                    BytecodeExpr.doReturn(mv, missed.getReturnType());
                                }
                            }
                    ));
            }
            else {
                if (traitMethod(missed)) {
                    traitMethods = true;
                }
                else if (likeGetter(missed)) {
                    String pname = missed.getName().substring(3);
                    pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                    final PropertyNode propertyNode = closureType.addProperty(pname, Opcodes.ACC_PUBLIC, missed.getReturnType(), null, null, null);
                    propertyNode.getField().addAnnotation(new AnnotationNode(TypeUtil.NO_EXTERNAL_INITIALIZATION));
                }
                else {
                    if (likeSetter(missed)) {
                        String pname = missed.getName().substring(3);
                        pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                        final PropertyNode propertyNode = closureType.addProperty(pname, Opcodes.ACC_PUBLIC, parameters[0].getType(), null, null, null);
                        propertyNode.getField().addAnnotation(new AnnotationNode(TypeUtil.NO_EXTERNAL_INITIALIZATION));
                    }
                }
            }
            k++;
        }

        if (traitMethods)
            TraitASTTransformFinal.improveAbstractMethods(closureType);
    }

    private static ClassNode getSubstitutedReturnType(MethodNode doCall, MethodNode missed, ClassNode closureType, ClassNode baseType) {
        ClassNode returnType = missed.getReturnType();
        if (missed.getParameters().length == doCall.getParameters().length) {
            int nParams = missed.getParameters().length;
            ClassNode declaringClass = missed.getDeclaringClass();
            GenericsType[] typeVars = declaringClass.getGenericsTypes();
            if (typeVars != null && typeVars.length > 0) {
                ClassNode[] formals = new ClassNode[nParams + 1];
                ClassNode[] actuals = new ClassNode[nParams + 1];
                for (int i = 0; i < nParams; i++) {
                    actuals[i] = doCall.getParameters()[i].getType();
                    formals[i] = missed.getParameters()[i].getType();
                }
                actuals[actuals.length - 1] = doCall.getReturnType();
                formals[formals.length - 1] = missed.getReturnType();
                ClassNode[] unified = TypeUnification.inferTypeArguments(typeVars, formals, actuals);
                ClassNode newBase = TypeUtil.withGenericTypes(baseType, unified);
                improveClosureType(closureType, newBase);
                returnType = TypeUtil.getSubstitutedType(returnType, declaringClass, newBase);
            }
        }
        return returnType;
    }

    public static void improveClosureType(final ClassNode closureType, ClassNode baseType) {
        if (baseType.isInterface()) {
            closureType.setInterfaces(new ClassNode[]{baseType});
            closureType.setSuperClass(ClassHelper.OBJECT_TYPE);
        } else {
            closureType.setInterfaces(baseType.equals(ClassHelper.CLOSURE_TYPE) ? new ClassNode[] {ClassHelper.GENERATED_CLOSURE_Type} : ClassNode.EMPTY_ARRAY);
            closureType.setSuperClass(baseType);
        }

        for(MethodNode mn: closureType.getMethods()) {
            CompileASTTransform.improveMethodTypes(mn);
        }
    }

    public static void createClosureConstructor(final ClassNode newType, final Parameter[] constrParams, Expression superArgs, CompilerTransformer compiler) {

        final ClassNode superClass = newType.getSuperClass();

        final Parameter[] finalConstrParams;
        final ArgumentListExpression superCallArgs = new ArgumentListExpression();
        if (superArgs != null) {
            final ArgumentListExpression args = (ArgumentListExpression) superArgs;
            if (args.getExpressions().size() > 0) {
                Parameter [] newParams = new Parameter [constrParams.length + args.getExpressions().size()];
                System.arraycopy(constrParams, 0, newParams, 0, constrParams.length);
                for (int i = 0; i != args.getExpressions().size(); ++i) {
                    final Parameter parameter = new Parameter(args.getExpressions().get(i).getType(), "$super$param$" + i);
                    newParams [i+constrParams.length] = parameter;
                    superCallArgs.addExpression(new VariableExpression(parameter));
                }
                finalConstrParams = newParams;
            }
            else
                finalConstrParams = constrParams;
        }
        else {
            if (superClass == ClassHelper.CLOSURE_TYPE) {
                if (constrParams.length > 0) {
                    superCallArgs.addExpression(new VariableExpression(constrParams[0]));
                    superCallArgs.addExpression(new VariableExpression(constrParams[0]));
                }
                else if(compiler.methodNode.isStatic() && !compiler.classNode.getName().endsWith("$TraitImpl")) {
                	ClassNode cn = compiler.classNode;
                    superCallArgs.addExpression(new ClassExpression(cn));
                    superCallArgs.addExpression(new ClassExpression(getOutermostClass(cn)));
                }
                else {
                    superCallArgs.addExpression(ConstantExpression.NULL);
                    superCallArgs.addExpression(ConstantExpression.NULL);
                }
            }
            finalConstrParams = constrParams;
        }

        ConstructorCallExpression superCall = new ConstructorCallExpression(ClassNode.SUPER, superCallArgs);

        BytecodeSequence fieldInit = new BytecodeSequence(new BytecodeInstruction() {
            public void visit(MethodVisitor mv) {
                for (int i = 0, k = 1; i != constrParams.length; i++) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);

                    final ClassNode type = constrParams[i].getType();
                    if (ClassHelper.isPrimitiveType(type)) {
                        if (type == ClassHelper.long_TYPE) {
                            mv.visitVarInsn(Opcodes.LLOAD, k++);
                            k++;
                        } else if (type == ClassHelper.double_TYPE) {
                            mv.visitVarInsn(Opcodes.DLOAD, k++);
                            k++;
                        } else if (type == ClassHelper.float_TYPE) {
                            mv.visitVarInsn(Opcodes.FLOAD, k++);
                        } else {
                            mv.visitVarInsn(Opcodes.ILOAD, k++);
                        }
                    } else {
                        mv.visitVarInsn(Opcodes.ALOAD, k++);
                    }
                    mv.visitFieldInsn(Opcodes.PUTFIELD, BytecodeHelper.getClassInternalName(newType), constrParams[i].getName(), BytecodeHelper.getTypeDescription(type));
                }
                mv.visitInsn(Opcodes.RETURN);
            }
        });


        BlockStatement code = new BlockStatement();
        code.addStatement(new ExpressionStatement(superCall));

        ConstructorNode cn = new ConstructorNode(
                    Opcodes.ACC_PUBLIC,
                    finalConstrParams,
                    ClassNode.EMPTY_ARRAY,
                code);
        newType.addConstructor(cn);

        code.addStatement(fieldInit);

        CleaningVerifier.getCleaningVerifier().visitClass(newType);

        compiler.replaceMethodCode(newType, cn);

        if (newType.getOuterClass() != null && newType.getMethods("methodMissing").isEmpty()) {
        	final ClassNode this0Type = 
        		(!compiler.methodNode.isStatic() || compiler.classNode.getName().endsWith("$TraitImpl")) ? 
        			newType.getOuterClass() : ClassHelper.CLASS_Type;
            newType.addMethod("methodMissing",
                    Opcodes.ACC_PUBLIC,
                    ClassHelper.OBJECT_TYPE,
                    new Parameter[] {
                            new Parameter(ClassHelper.STRING_TYPE, "name"),
                            new Parameter(ClassHelper.OBJECT_TYPE, "args")},
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(new BytecodeInstruction(){
                        public void visit(MethodVisitor mv) {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitFieldInsn(Opcodes.GETFIELD, BytecodeHelper.getClassInternalName(newType), "this$0", BytecodeHelper.getTypeDescription(this0Type));
                                mv.visitVarInsn(Opcodes.ALOAD, 1);
                                mv.visitVarInsn(Opcodes.ALOAD, 2);
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "invokeMethod", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
                                mv.visitInsn(Opcodes.ARETURN);
                        }
                    }));
        }
    }

    public static void addFields(ClosureExpression ce, ClassNode newType, CompilerTransformer compiler) {
        for(Iterator<Variable> it = ce.getVariableScope().getReferencedLocalVariablesIterator(); it.hasNext(); ) {
            Variable astVar = it.next();
            final Register var = compiler.compileStack.getRegister(astVar.getName(), false);

            ClassNode vtype;
            if (var != null) {
                vtype = compiler.getLocalVarInferenceTypes().get(astVar);
                if (vtype == null)
                   vtype = var.getType();
            }
            else {
                if (astVar instanceof VariableExpression && ((VariableExpression)astVar).getAccessedVariable() instanceof FieldNode) {
                    continue;
                }
                else
                    vtype = compiler.methodNode.getDeclaringClass().getField(astVar.getName()).getType();
            }

            if (newType.getDeclaredField(astVar.getName()) == null) {
                newType.addField(astVar.getName(), Opcodes.ACC_FINAL, vtype, null);
            }
        }
        ClassNodeCache.clearCache(newType);
    }

    public static Parameter[] createClosureConstructorParams(ClassNode newType, CompilerTransformer compiler) {
        List<FieldNode> fields = newType.getFields();

        final List<Parameter> constrParams = new ArrayList<Parameter>(fields.size());

        FieldNode ownerField = newType.getField("this$0");
        if (ownerField != null && 
        	(!compiler.methodNode.isStatic() || compiler.classNode.getName().endsWith("$TraitImpl")))
            constrParams.add(new Parameter(ownerField.getType(), "this$0"));
        for (int i = 0; i != fields.size(); ++i) {
            final FieldNode fieldNode = fields.get(i);
            if (!fieldNode.getName().equals("this$0") && !fieldNode.getName().equals("metaClass") && !fieldNode.getName().equals("$staticClassInfo") && fieldNode.getAnnotations(TypeUtil.NO_EXTERNAL_INITIALIZATION).isEmpty() && !fieldNode.isStatic())
                constrParams.add(new Parameter(fieldNode.getType(), fieldNode.getName()));
        }
        return constrParams.toArray(new Parameter[constrParams.size()]);
    }

    public static void instantiateClass(ClassNode type, CompilerTransformer compiler, Parameter[] constrParams, Expression superArgs, MethodVisitor mv) {
        TraitASTTransformFinal.improveAbstractMethods(type);
        type.getModule().addClass(type);

        final String classInternalName = BytecodeHelper.getClassInternalName(type);
        mv.visitTypeInsn(Opcodes.NEW, classInternalName);
        mv.visitInsn(Opcodes.DUP);

        final ConstructorNode constructorNode = type.getDeclaredConstructors().get(0);

        for (int i = 0; i != constrParams.length; i++) {
            String name = constrParams[i].getName();

            if ("this$0".equals(name)) {
                mv.visitVarInsn(Opcodes.ALOAD,0);
            }
            else {
                final Register var = compiler.compileStack.getRegister(name, false);
                if (var != null) {
                    FieldNode field = type.getDeclaredField(name);
                    BytecodeExpr.load(field.getType(), var.getIndex(), mv);
                    if (!constrParams[i].getType().equals(var.getType()) && !ClassHelper.isPrimitiveType(field.getType())) {
                        BytecodeExpr.checkCast(constrParams[i].getType(), mv);
                    }
                }
                else {
                    FieldNode field = compiler.methodNode.getDeclaringClass().getDeclaredField(name);
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    if (field == null) // @Field
                        name = compiler.methodNode.getName() + "$" + name;
                    mv.visitFieldInsn(Opcodes.GETFIELD, BytecodeHelper.getClassInternalName(compiler.methodNode.getDeclaringClass()), name, BytecodeHelper.getTypeDescription(constrParams[i].getType()));
                }
            }
        }

        if (superArgs != null) {
            final List<Expression> list = ((ArgumentListExpression) superArgs).getExpressions();
            for (int i = 0; i != list.size(); ++i)
                ((BytecodeExpr)list.get(i)).visit(mv);
        }

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classInternalName, "<init>", BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, constructorNode.getParameters()));
    }
    
    private static ClassNode getOutermostClass(ClassNode classNode) {
        ClassNode outermostClass = classNode;
        
        while (outermostClass instanceof InnerClassNode) {
            outermostClass = outermostClass.getOuterClass();
        }
        
        return outermostClass;
    }
}
