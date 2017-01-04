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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Created by linan on 16/12/5.
 */
public class ASMModifyCodeShell {
    public static boolean DEBUG = true;
    public static final String TAG = "ASMModifyCodeShell";

    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new BasicParser();
        Options options = new Options( );
        options.addOption("h", "help", false, "-classdir $class.dir -reverseFilter $regex -packageName $packageName");
        options.addOption("c", "classdir", true, "class source path");
        options.addOption("v", "reverseFilter", true, "regex expression, filter out the regex matched files" );
        options.addOption("p", "packageName", true, "packageName" );

        String reverseFilter = "";
        String classDir = "";
        String packageName = "com/com.hyphenate/patch";
        CommandLine commandLine = parser.parse( options, args );
        if( commandLine.hasOption('h') ) {
            System.out.println( "Help Message");
            System.exit(0);
        }
        if( commandLine.hasOption('c') ) {
            classDir= commandLine.getOptionValue('c');
            EMLog.d(TAG, "classDir:" + classDir);
        }
        if( commandLine.hasOption('v') ) {
            reverseFilter = commandLine.getOptionValue('v');
            EMLog.d(TAG, "reverseFilter:" + reverseFilter);
            if (reverseFilter.startsWith("\"") && reverseFilter.endsWith("\"")) {
                reverseFilter = reverseFilter.substring(1, reverseFilter.length() - 1);
            }
        }
        if( commandLine.hasOption('p') ) {
            packageName = commandLine.getOptionValue('p');
            EMLog.d(TAG, "packageName:" + packageName);
        }

        if (classDir.equals("")) {
            throw new Exception("classdir can not be empty");
        }

        final Set<String> classFiles = Collections.synchronizedSet(new HashSet<String>());
        iterateDir(new File(classDir), classFiles);

        if (!reverseFilter.equals("")) {
            filterOut(classFiles, reverseFilter);
        }

        final String _packageName = packageName;
        int totalJobsCount = classFiles.size();
        final AtomicInteger jobDone = new AtomicInteger();
        ExecutorService mainQueue = Executors.newFixedThreadPool(4);
        for (final String path : classFiles) {
            mainQueue.execute(new Runnable() {
                @Override
                public void run() {
                    boolean done = asmJob(_packageName, path, path);
                    if (done == false) {
                        EMLog.d(TAG, "job: " + path + " failed");
                        System.exit(-1);
                    }
                    jobDone.getAndAdd(1);
                }
            });
        }
        while (jobDone.get() != totalJobsCount) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        EMLog.d(TAG, "Finish " + totalJobsCount);
        mainQueue.shutdown();
    }

    static void iterateDir(File file, Set<String> fileList) {
        try {
            if (file == null || !file.exists() || !file.canWrite()) {
                return;
            }
            if (file.isFile() && file.getName().endsWith(".class")) {
                fileList.add(file.getAbsolutePath());
                return;
            }
            if (file.isDirectory()) {
                for (File item : file.listFiles()) {
                    iterateDir(item, fileList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void filterOut(Set<String> fileSet, String reverse) {
        Pattern pattern = Pattern.compile(reverse);

        Set<String> reverseMatchSet = new HashSet<>();
        for (String fileName : fileSet) {
            if (pattern.matcher(fileName).matches()) {
                reverseMatchSet.add(fileName);
            }
        }
        fileSet.removeAll(reverseMatchSet);

        if (DEBUG) {
            EMLog.d(TAG, "===== reverseMatchSet");
            debugFiles(reverseMatchSet);
        }
    }

    static boolean asmJob(String packageName, String inputFile, String outputFile) {
        try {
            {
                ClassReader classReader = new ClassReader(new FileInputStream(inputFile));
                ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                ASMPreModifyCode preModifyClassVisitor = new com.hyphenate.asm.ASMPreModifyCode(classWriter);
                classReader.accept(preModifyClassVisitor, ClassReader.SKIP_DEBUG);
                // can not support template class
                if (!preModifyClassVisitor.canModify()) {
                    EMLog.d(TAG, "============== can not modify: " + inputFile);
                    return true;
                }
            }

            ClassReader classReader = new ClassReader(new FileInputStream(inputFile));
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            ClassVisitor addField = new com.hyphenate.asm.ASMModifyCode(packageName, classWriter);
            classReader.accept(addField, ClassReader.SKIP_DEBUG);
            byte[] newClass = classWriter.toByteArray();
            File newFile = new File(outputFile);
            new FileOutputStream(newFile).write(newClass);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static void debugFiles(Set<String> fileSet) {
        for (String file : fileSet) {
            EMLog.d(TAG, "    " + file);
        }
    }

}
