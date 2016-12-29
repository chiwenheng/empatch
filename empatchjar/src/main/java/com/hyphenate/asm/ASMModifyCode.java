/**
 * Copyright (c) <2016> <easemob.com>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Created by linan on 16/11/17.
 */

package com.hyphenate.asm;

import com.hyphenate.asm.reflect.ParameterizedTypeImpl;
import com.hyphenate.asm.reflect.SignatureParser;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by linan on 16/11/18.
 */
public class ASMModifyCode  extends AbstractClassVisitor {
    public static final String TAG = "ASMModifyCode";

    private boolean addFieldIsPresent = false;
    private int addFieldAccess = Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC;// + Opcodes.ACC_FINAL;
    private String addFieldName = "$patch";
    private String addFieldDesc = null;
    private Object addFieldValue = null;

    String className = "";
    boolean isInterface = false;
    boolean isEnum = false;
    boolean hasOuterClass = false;
    String packageName = "";

    public ASMModifyCode(String packageName, ClassVisitor delegate) throws Exception {
        super(delegate);
        if (packageName.equals("")) {
            throw new Exception("package name can not be empty");
        }
        if (packageName.contains(".")) {
            throw new Exception("package name use '/' rather than '.'");
        }
        this.packageName = packageName;
        addFieldDesc = "L" + packageName + "/PatchReDirection;";
    }

    boolean needModify() {
        return isInterface == false && isEnum == false;
    }

    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        EMLog.d(TAG, "visit className:" + name);
        className = name;
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            isInterface = true;
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            isEnum = true;
        }
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        hasOuterClass = true;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (needModify()) {
            if (name.equals(this.addFieldName)) {
                addFieldIsPresent = true;
            }
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {
        if (needModify()) {
            if (!addFieldIsPresent) {
                FieldVisitor fv = super.visitField(addFieldAccess, addFieldName, addFieldDesc, null, addFieldValue);
                if (fv != null) {
                    fv.visitEnd();//不是原有的属性，故不会有事件发出的，自己 end 掉。
                }
            }
        }
        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        // TODO: bypass method parameters contains '...', which is length un-certain type, not sure what addFieldDesc will be like

        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (needModify() &&
                !name.equals("<init>") &&
                !name.equals("<clinit>") &&
                !name.equals("main") &&
                (Opcodes.ACC_BRIDGE & access) == 0 &&
                (Opcodes.ACC_SYNTHETIC & access) == 0) {

            List<Type> typeList = new SignatureParser().parseDesc(desc); // addFieldDesc is signature, and param signature is null
            if ((Opcodes.ACC_STATIC & access) == 0) {
                return new ClassMethodAdapter(mv, typeList);
            } else {
                return new StaticMethodAdapter(mv, typeList);
            }
        } else {
            return mv;
        }
    }

    class StaticMethodAdapter extends MethodVisitor {
        List<Type> mTypeList;
        public StaticMethodAdapter(MethodVisitor mv, List<Type> typeList)
        {
            super(Opcodes.ASM5, mv);
            mTypeList = typeList;
        }

        @Override
        public void visitCode() {
            mv.visitCode();

            final String PatchProxyClassName = packageName + "/PatchProxy";
            final String patchReDirectionSignature = "L" + packageName + "/PatchReDirection;";

            // android studio
//            mv.visitVarInsn(Opcodes.ALOAD, 0);
//            mv.visitFieldInsn(Opcodes.GETFIELD, className, addFieldName, "Lcom/com.hyphenate/PatchReDirection;");
            // ant
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, addFieldName, patchReDirectionSignature);

            Label l0 = new Label();
            mv.visitJumpInsn(Opcodes.IFNULL, l0);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitInsn(Opcodes.ACONST_NULL);

            // android studio
//            mv.visitVarInsn(Opcodes.ALOAD, 0);
//            mv.visitFieldInsn(Opcodes.GETFIELD, className, addFieldName, "Lcom/com.hyphenate/PatchReDirection;");
            // ant
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, addFieldName, patchReDirectionSignature);

            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, PatchProxyClassName, "isSupport", "([Ljava/lang/Object;Ljava/lang/Object;" + patchReDirectionSignature + "Z)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, l0);

            mv.visitIntInsn(Opcodes.BIPUSH, mTypeList.size() - 1);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

            // mTypeList last pos is return type
            // local variable pos 0 is 'this', local variable exists of stack frame, a concept of JVM.
            // double & long occupy two local variable stub, others occupy one stub
            // still, they can not share same Opcodes.ILOAD
            int localVarPos = 0;
            for (int i = 0; i < mTypeList.size() - 1; i++) {
                Type type = mTypeList.get(i);

                mv.visitInsn(Opcodes.DUP);
                mv.visitIntInsn(Opcodes.BIPUSH, i);
                if (type == byte.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                } else if (type == char.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                } else if (type == short.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                } else if (type == int.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                } else if (type == long.class) {
                    mv.visitVarInsn(Opcodes.LLOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                    localVarPos++;
                } else if (type == float.class) {
                    mv.visitVarInsn(Opcodes.FLOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                } else if (type == double.class) {
                    mv.visitVarInsn(Opcodes.DLOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                    localVarPos++;
                } else if (type == void.class) {
                    // should not execute
                } else if (type == boolean.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                } else if (type instanceof ParameterizedTypeImpl) {
                    mv.visitVarInsn(Opcodes.ALOAD, localVarPos);
                } else if (type instanceof com.hyphenate.asm.reflect.GenericArrayTypeImpl) {
                    mv.visitVarInsn(Opcodes.ALOAD, localVarPos);
                } else {
                    EMLog.d(TAG, "type:" + type);
                    throw new GenericSignatureFormatError("type:" + type);
                }
                mv.visitInsn(Opcodes.AASTORE);
                localVarPos++;
            }

            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, addFieldName, patchReDirectionSignature);
            mv.visitInsn(Opcodes.ICONST_1);

            Type returnType = mTypeList.get(mTypeList.size() - 1);

            if (returnType == void.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, PatchProxyClassName, "accessDispatchVoid", "([Ljava/lang/Object;Ljava/lang/Object;" + patchReDirectionSignature + "Z)V", false);
            } else {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, PatchProxyClassName, "accessDispatch", "([Ljava/lang/Object;Ljava/lang/Object;" + patchReDirectionSignature + "Z)Ljava/lang/Object;", false);
            }

            // resolve return type
            if (mTypeList.size() == 0) {
                throw new GenericSignatureFormatError("mTypeList size can't be 0");
            }
            if (returnType == byte.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType == char.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType == short.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType == int.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType == long.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                mv.visitInsn(Opcodes.LRETURN);
            } else if (returnType == float.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                mv.visitInsn(Opcodes.FRETURN);
            } else if (returnType == double.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                mv.visitInsn(Opcodes.DRETURN);
            } else if (returnType == void.class) {
                mv.visitInsn(Opcodes.RETURN);
            } else if (returnType == boolean.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType instanceof ParameterizedTypeImpl) {
                // TODO: class type cast to designate type, DONE
                mv.visitTypeInsn(Opcodes.CHECKCAST, ((ParameterizedTypeImpl)returnType).getRawTypeName());
                mv.visitInsn(Opcodes.ARETURN);
            } else if (returnType instanceof com.hyphenate.asm.reflect.GenericArrayTypeImpl) {
                com.hyphenate.asm.reflect.GenericArrayTypeImpl returnArray = (com.hyphenate.asm.reflect.GenericArrayTypeImpl)returnType;
                Type type = returnArray.getGenericComponentType();
                if (type == byte.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[B");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[B");
                } else if (type == char.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[C");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[C");
                } else if (type == short.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[S");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[S");
                } else if (type == int.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[I");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[I");
                } else if (type == long.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[J");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[J");
                } else if (type == float.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[F");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[F");
                } else if (type == double.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[D");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[D");
                } else if (type == boolean.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[Z");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[Z");
                } else if (type instanceof ParameterizedTypeImpl) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
                }
                mv.visitInsn(Opcodes.ARETURN);
            } else {
                EMLog.d(TAG, "type:" + returnType);
                throw new GenericSignatureFormatError("type:" + returnType);
            }

            mv.visitLabel(l0);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

    }

    class ClassMethodAdapter extends MethodVisitor {
        List<Type> mTypeList;

        public ClassMethodAdapter(MethodVisitor mv, List<Type> typeList)
        {
            super(Opcodes.ASM5, mv);
            mTypeList = typeList;
        }

        @Override
        public void visitCode() {
            mv.visitCode();

            final String PatchProxyClassName = packageName + "/PatchProxy";
            final String patchReDirectionSignature = "L" + packageName + "/PatchReDirection;";


            // android studio
//            mv.visitVarInsn(Opcodes.ALOAD, 0);
//            mv.visitFieldInsn(Opcodes.GETFIELD, className, addFieldName, "Lcom/com.hyphenate/PatchReDirection;");
            // ant
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, addFieldName, patchReDirectionSignature);

            Label l0 = new Label();
            mv.visitJumpInsn(Opcodes.IFNULL, l0);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            mv.visitVarInsn(Opcodes.ALOAD, 0);

            // android studio
//            mv.visitVarInsn(Opcodes.ALOAD, 0);
//            mv.visitFieldInsn(Opcodes.GETFIELD, className, addFieldName, "Lcom/com.hyphenate/PatchReDirection;");
            // ant
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, addFieldName, patchReDirectionSignature);

            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, PatchProxyClassName, "isSupport", "([Ljava/lang/Object;Ljava/lang/Object;" + patchReDirectionSignature + "Z)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, l0);

            mv.visitIntInsn(Opcodes.BIPUSH, mTypeList.size() - 1);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

            // mTypeList last pos is return type
            // local variable pos 0 is 'this', local variable exists of stack frame, a concept of JVM.
            // double & long occupy two local variable stub, others occupy one stub
            // still, they can not share same Opcodes.ILOAD
            int localVarPos = 1;
            for (int i = 0; i < mTypeList.size() - 1; i++) {
                Type type = mTypeList.get(i);

                mv.visitInsn(Opcodes.DUP);
                mv.visitIntInsn(Opcodes.BIPUSH, i);
                if (type == byte.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                } else if (type == char.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                } else if (type == short.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                } else if (type == int.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                } else if (type == long.class) {
                    mv.visitVarInsn(Opcodes.LLOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                    localVarPos++;
                } else if (type == float.class) {
                    mv.visitVarInsn(Opcodes.FLOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                } else if (type == double.class) {
                    mv.visitVarInsn(Opcodes.DLOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                    localVarPos++;
                } else if (type == void.class) {
                    // should not execute
                } else if (type == boolean.class) {
                    mv.visitVarInsn(Opcodes.ILOAD, localVarPos);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                } else if (type instanceof ParameterizedTypeImpl) {
                    mv.visitVarInsn(Opcodes.ALOAD, localVarPos);
                } else if (type instanceof com.hyphenate.asm.reflect.GenericArrayTypeImpl) {
                    mv.visitVarInsn(Opcodes.ALOAD, localVarPos);
                } else {
                    EMLog.d(TAG, "type:" + type);
                    throw new GenericSignatureFormatError("type:" + type);
                }
                mv.visitInsn(Opcodes.AASTORE);
                localVarPos++;
            }

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, addFieldName, patchReDirectionSignature);
            mv.visitInsn(Opcodes.ICONST_0);

            Type returnType = mTypeList.get(mTypeList.size() - 1);
            if (returnType == void.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, PatchProxyClassName, "accessDispatchVoid", "([Ljava/lang/Object;Ljava/lang/Object;" + patchReDirectionSignature + "Z)V", false);
            } else {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, PatchProxyClassName, "accessDispatch", "([Ljava/lang/Object;Ljava/lang/Object;" + patchReDirectionSignature + "Z)Ljava/lang/Object;", false);
            }

            // 修改返回类型
            if (mTypeList.size() == 0) {
                throw new GenericSignatureFormatError("mTypeList size can't be 0");
            }
            if (returnType == byte.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType == char.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType == short.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType == int.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType == long.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                mv.visitInsn(Opcodes.LRETURN);
            } else if (returnType == float.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                mv.visitInsn(Opcodes.FRETURN);
            } else if (returnType == double.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                mv.visitInsn(Opcodes.DRETURN);
            } else if (returnType == void.class) {
                mv.visitInsn(Opcodes.RETURN);
            } else if (returnType == boolean.class) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (returnType instanceof ParameterizedTypeImpl) {
                // TODO: class type cast to designate type, DONE
                mv.visitTypeInsn(Opcodes.CHECKCAST, ((ParameterizedTypeImpl)returnType).getRawTypeName());
                mv.visitInsn(Opcodes.ARETURN);
            } else if (returnType instanceof com.hyphenate.asm.reflect.GenericArrayTypeImpl) {
                com.hyphenate.asm.reflect.GenericArrayTypeImpl returnArray = (com.hyphenate.asm.reflect.GenericArrayTypeImpl)returnType;
                Type type = returnArray.getGenericComponentType();
                if (type == byte.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[B");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[B");
                } else if (type == char.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[C");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[C");
                } else if (type == short.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[S");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[S");
                } else if (type == int.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[I");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[I");
                } else if (type == long.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[J");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[J");
                } else if (type == float.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[F");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[F");
                } else if (type == double.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[D");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[D");
                } else if (type == boolean.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[Z");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[Z");
                } else if (type instanceof ParameterizedTypeImpl) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
                }
                mv.visitInsn(Opcodes.ARETURN);
            } else {
                EMLog.d(TAG, "type:" + returnType);
                throw new GenericSignatureFormatError("type:" + returnType);
            }

            mv.visitLabel(l0);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }
    }

    public static void main(String[] args) throws Exception {
        // TODO: accept args to set input file, and output file
        final String inputFile;
        final String outputFile;
        final String packageName;

        EMLog.d(TAG, "args length:" + args.length);
        if (args.length == 3) {
            inputFile = args[0];
            outputFile = args[1];
            packageName = args[2];
        } else {
            EMLog.d(TAG, "provide inputPath and outputPath");
            return;
        }

        ClassReader classReader = new ClassReader(new FileInputStream(inputFile));

        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor addField = new ASMModifyCode(packageName, classWriter);
        classReader.accept(addField, ClassReader.SKIP_DEBUG);
        byte[] newClass = classWriter.toByteArray();
        File newFile = new File(outputFile);
        new FileOutputStream(newFile).write(newClass);

    }
}

