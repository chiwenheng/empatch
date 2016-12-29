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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Created by linan on 16/11/18.
 */
public class ASMPreModifyCode extends AbstractClassVisitor {

    boolean has_access_bridge = false;
    boolean has_access_synthetic = false;

    public ASMPreModifyCode(ClassVisitor delegate) {
        super(delegate);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        if ((Opcodes.ACC_BRIDGE & access) != 0) {
            has_access_bridge = true;
        }

        if ((Opcodes.ACC_SYNTHETIC & access) != 0) {
            has_access_synthetic = true;
        }
        return cv.visitMethod(access, name, desc, signature, exceptions);
    }

    public boolean canModify() {
        return has_access_bridge == false && has_access_synthetic == false;
    }
}

