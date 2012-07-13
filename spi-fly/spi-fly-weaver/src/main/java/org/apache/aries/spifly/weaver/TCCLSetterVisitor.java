/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.spifly.weaver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.aries.spifly.Util;
import org.apache.aries.spifly.WeavingData;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * This class implements an ASM ClassVisitor which puts the appropriate ThreadContextClassloader
 * calls around applicable method invocations. It does the actual bytecode weaving.
 */
public class TCCLSetterVisitor extends ClassVisitor implements Opcodes {
    private static final Type CLASSLOADER_TYPE = Type.getType(ClassLoader.class);

    private static final String GENERATED_METHOD_NAME = "$$FCCL$$";

    private static final Type UTIL_CLASS = Type.getType(Util.class);

    private static final Type CLASS_TYPE = Type.getType(Class.class);

    private static final Type String_TYPE = Type.getType(String.class);

    private final Type targetClass;
    private final Set<WeavingData> weavingData;

    // Set to true when the weaving code has changed the client such that an additional import
    // (to the Util.class.getPackage()) is needed.
    private boolean additionalImportRequired = false;

    // This field is true when the class was woven
    private boolean woven = false;

    public TCCLSetterVisitor(ClassVisitor cv, String className, Set<WeavingData> weavingData) {
        super(Opcodes.ASM4, cv);
        this.targetClass = Type.getType("L" + className.replace('.', '/') + ";");
        this.weavingData = weavingData;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new TCCLSetterMethodVisitor(mv, access, name, desc);
    }

    @Override
    public void visitEnd() {
        if (!woven) {
            // if this class wasn't woven, then don't add the synthesized method either.
            super.visitEnd();
            return;
        }

        // Add generated static method
        Set<String> methodNames = new HashSet<String>();

        for (WeavingData wd : weavingData) {
            /* Equivalent to:
             * private static void $$FCCL$$<className>$<methodName>(Class<?> cls) {
             *   Util.fixContextClassLoader("java.util.ServiceLoader", "load", cls, WovenClass.class.getClassLoader());
             * }
             */
             String methodName = getGeneratedMethodName(wd);
             if (methodNames.contains(methodName))
                 continue;

             methodNames.add(methodName);
             Method method = new Method(methodName, Type.VOID_TYPE, new Type[] {CLASS_TYPE});

             GeneratorAdapter mv = new GeneratorAdapter(cv.visitMethod(ACC_PRIVATE + ACC_STATIC, methodName,
                     method.getDescriptor(), null, null), ACC_PRIVATE + ACC_STATIC, methodName,
                     method.getDescriptor());

             //Load the strings, method parameter and target
             mv.visitLdcInsn(wd.getClassName());
             mv.visitLdcInsn(wd.getMethodName());
             mv.loadArg(0);
             mv.visitLdcInsn(targetClass);

             //Change the class on the stack into a classloader
             mv.invokeVirtual(CLASS_TYPE, new Method("getClassLoader",
                 CLASSLOADER_TYPE, new Type[0]));

             //Call our util method
             mv.invokeStatic(UTIL_CLASS, new Method("fixContextClassloader", Type.VOID_TYPE,
                 new Type[] {String_TYPE, String_TYPE, CLASS_TYPE, CLASSLOADER_TYPE}));

             mv.returnValue();
             mv.endMethod();
        }

        super.visitEnd();
    }

    private String getGeneratedMethodName(WeavingData wd) {
        StringBuilder name = new StringBuilder(GENERATED_METHOD_NAME);
        name.append(wd.getClassName().replace('.', '#'));
        name.append("$");
        name.append(wd.getMethodName());
        if (wd.getArgClasses() != null) {
            for (String cls : wd.getArgClasses()) {
                name.append("$");
                name.append(cls.replace('.', '#'));
            }
        }
        return name.toString();
    }

    private class TCCLSetterMethodVisitor extends GeneratorAdapter {
        Type lastLDCType;

        public TCCLSetterMethodVisitor(MethodVisitor mv, int access, String name, String descriptor) {
            super(Opcodes.ASM4, mv, access, name, descriptor);
        }

        /**
         * Store the last LDC call. When ServiceLoader.load(Class cls) is called
         * the last LDC call before the ServiceLoader.load() visitMethodInsn call
         * contains the class being passed in. We need to pass this class to $$FCCL$$ as well
         * so we can copy the value found in here.
         */
        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Type) {
                lastLDCType = ((Type) cst);
            }
            super.visitLdcInsn(cst);
        }

        /**
         * Wrap selected method calls with
         *  Util.storeContextClassloader();
         *  $$FCCL$$(<class>)
         *  Util.restoreContextClassloader();
         */
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            WeavingData wd = findWeavingData(owner, name, desc);
            if (opcode == INVOKESTATIC && wd != null) {
                additionalImportRequired = true;
                woven = true;

                Label startTry = newLabel();
                Label endTry = newLabel();

                //start try block
                visitTryCatchBlock(startTry, endTry, endTry, null);
                mark(startTry);

                // Add: Util.storeContextClassloader();
                invokeStatic(UTIL_CLASS, new Method("storeContextClassloader", Type.VOID_TYPE, new Type[0]));


                // Add: MyClass.$$FCCL$$<classname>$<methodname>(<class>);
                if (ServiceLoader.class.getName().equals(wd.getClassName()) &&
                    "load".equals(wd.getMethodName()) &&
                    (wd.getArgClasses() == null || Arrays.equals(new String [] {Class.class.getName()}, wd.getArgClasses()))) {
                    // ServiceLoader.load() is a special case because it's a general-purpose service loader,
                    // therefore, the target class it the class being passed in to the ServiceLoader.load()
                    // call itself.

                    mv.visitLdcInsn(lastLDCType);
                } else {
                    // In any other case, we're not dealing with a general-purpose service loader, but rather
                    // with a specific one, such as DocumentBuilderFactory.newInstance(). In that case the
                    // target class is the class that is being invoked on (i.e. DocumentBuilderFactory).
                    Type type = Type.getObjectType(owner);
                    mv.visitLdcInsn(type);
                }
                invokeStatic(targetClass, new Method(getGeneratedMethodName(wd),
                    Type.VOID_TYPE, new Type[] {CLASS_TYPE}));

                //Call the original instruction
                super.visitMethodInsn(opcode, owner, name, desc);

                //If no exception then go to the finally (finally blocks are a catch block with a jump)
                Label afterCatch = newLabel();
                goTo(afterCatch);


                //start the catch
                mark(endTry);
                //Run the restore method then throw on the exception
                invokeStatic(UTIL_CLASS, new Method("restoreContextClassloader", Type.VOID_TYPE, new Type[0]));
                throwException();

                //start the finally
                mark(afterCatch);
                //Run the restore and continue
                invokeStatic(UTIL_CLASS, new Method("restoreContextClassloader", Type.VOID_TYPE, new Type[0]));
            } else {
                super.visitMethodInsn(opcode, owner, name, desc);
            }
        }

        private WeavingData findWeavingData(String owner, String methodName, String methodDesc) {
            owner = owner.replace('/', '.');

            Type[] argTypes = Type.getArgumentTypes(methodDesc);
            String [] argClassNames = new String[argTypes.length];
            for (int i = 0; i < argTypes.length; i++) {
                argClassNames[i] = argTypes[i].getClassName();
            }

            for (WeavingData wd : weavingData) {
                if (wd.getClassName().equals(owner) &&
                    wd.getMethodName().equals(methodName) &&
                    (wd.getArgClasses() != null ? Arrays.equals(argClassNames, wd.getArgClasses()) : true)) {
                    return wd;
                }
            }
            return null;
        }
    }

    public boolean additionalImportRequired() {
        return additionalImportRequired ;
    }
}