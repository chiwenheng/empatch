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

package com.hyphenate.asm.reflect;

import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class SignatureParser {

    public final static String TAG = "SignatureParser";

    char symbol;
    String identifier;
    ClassLoader loader;

    private boolean eof;

    char[] buffer;
    int pos;

    // Holds a mapping from Java type names to native type codes.
    // TODO: append two types that for Void & Object & A(array) type, we only care about these types
    private static final Map<Class<?>, String> PRIMITIVE_TO_SIGNATURE;
    static {
        PRIMITIVE_TO_SIGNATURE = new HashMap<Class<?>, String>(9);
        PRIMITIVE_TO_SIGNATURE.put(byte.class, "B");
        PRIMITIVE_TO_SIGNATURE.put(char.class, "C");
        PRIMITIVE_TO_SIGNATURE.put(short.class, "S");
        PRIMITIVE_TO_SIGNATURE.put(int.class, "I");
        PRIMITIVE_TO_SIGNATURE.put(long.class, "J");
        PRIMITIVE_TO_SIGNATURE.put(float.class, "F");
        PRIMITIVE_TO_SIGNATURE.put(double.class, "D");
        PRIMITIVE_TO_SIGNATURE.put(void.class, "V");
        PRIMITIVE_TO_SIGNATURE.put(boolean.class, "Z");
        // TODO: append two types
        PRIMITIVE_TO_SIGNATURE.put(ParameterizedTypeImpl.class, "O");
        PRIMITIVE_TO_SIGNATURE.put(GenericArrayTypeImpl.class, "A");
    }

    void scanSymbol() {
        if (!eof) {
            if (pos < buffer.length) {
                symbol = buffer[pos];
                pos++;
            } else {
                symbol = 0;
                eof = true;
            }
        } else {
            throw new GenericSignatureFormatError();
        }
    }

    void expect(char c) {
        if (symbol == c) {
            scanSymbol();
        } else {
            throw new GenericSignatureFormatError();
        }
    }

    void setInput(String input) {
        if (input != null) {
            this.buffer = input.toCharArray();
            this.eof = false;
            scanSymbol();
        }
        else {
            this.eof = true;
        }
    }

    Type parseTypeSignature() {
        switch (symbol) {
            case 'B': scanSymbol(); return byte.class;
            case 'C': scanSymbol(); return char.class;
            case 'D': scanSymbol(); return double.class;
            case 'F': scanSymbol(); return float.class;
            case 'I': scanSymbol(); return int.class;
            case 'J': scanSymbol(); return long.class;
            case 'S': scanSymbol(); return short.class;
            case 'Z': scanSymbol(); return boolean.class;
            case 'L':
                return parseClassTypeSignature();
            case '[':
                scanSymbol();
                // ArrayTypeSignature ::= "[" TypSignature.
                // TODO: array type could be recursive
                return new GenericArrayTypeImpl(parseTypeSignature());
            case 'T':
                // TODO: don't support Type Variant signature, too complicated
                //return parseTypeVariableSignature();
            default:
                throw new GenericSignatureFormatError();
        }
    }

    static boolean isStopSymbol(char ch) {
        switch (ch) {
            case ':':
            case '/':
            case ';':
            case '<':
            case '.':
                return true;
        }
        return false;
    }

    static boolean packageStopSymbol(char ch) {
        switch (ch) {
            case ';':
                return true;
        }
        return false;
    }


    // TODO: previous used by parseClasTypeSignature, now it is obsolete
    // PRE: symbol is the first char of the identifier.
    // POST: symbol = the next symbol AFTER the identifier.
    void scanIdentifier() {
        if (!eof) {
            StringBuilder identBuf = new StringBuilder(32);
            if (!isStopSymbol(symbol)) {
                identBuf.append(symbol);
                do {
                    char ch = buffer[pos];
                    // TODO: how about '_', '$', '\d'
                    if ((ch >= 'a') && (ch <= 'z') || (ch >= 'A') && (ch <= 'Z')
                            || !isStopSymbol(ch)) {
                        identBuf.append(ch);
                        pos++;
                    } else {
                        identifier = identBuf.toString();
                        scanSymbol();
                        return;
                    }
                } while (pos != buffer.length);
                identifier = identBuf.toString();
                eof = true;
            } else {
                // Ident starts with incorrect char.
                eof = true;
                throw new GenericSignatureFormatError();
            }
        } else {
            throw new GenericSignatureFormatError();
        }
    }

    Type parseClassTypeSignature() {

        // ClassTypeSignature ::= "L" {Ident "/"} Ident
        //         OptTypeArguments {"." Ident OptTypeArguments} ";".


        // TODO: this is 1st version
        // =================== start ==============
//        expect('L');
//        StringBuilder identBuf = new StringBuilder(32);
//        do {
//            char ch = buffer[pos];
//            if (!packageStopSymbol(ch)) {
//                identBuf.append(ch);
//                pos++;
//            } else {
//                identifier = identBuf.toString();
//                scanSymbol();
//                break;
//            }
//        } while (pos < buffer.length);
//        expect(';');
//        return Object.class;
        // =================== end

        // TODO: this is 2nd version
        expect('L');
        StringBuilder qualIdent = new StringBuilder();
        scanIdentifier();
        while (symbol == '/') {
            scanSymbol();
            qualIdent.append(identifier).append("/");
            scanIdentifier();
        }

        qualIdent.append(this.identifier);
        ListOfTypes typeArgs = new ListOfTypes(8);
        ParameterizedTypeImpl parentType =
                new ParameterizedTypeImpl(null, qualIdent.toString(), typeArgs, loader);
        ParameterizedTypeImpl type = parentType;

        /*
        while (symbol == '.') {
            // Deal with Member Classes:
            scanSymbol();
            scanIdentifier();
            qualIdent.append("$").append(identifier); // FIXME: is "$" correct?
            typeArgs = parseOptTypeArguments();
            type = new ParameterizedTypeImpl(parentType, qualIdent.toString(), typeArgs,
                    loader);
        }
        */

        expect(';');

        return type;


        // TODO: original implements
        /*
        expect('L');

        StringBuilder qualIdent = new StringBuilder();
        scanIdentifier();
        while (symbol == '/') {
            scanSymbol();
            qualIdent.append(identifier).append(".");
            scanIdentifier();
        }

        qualIdent.append(this.identifier);

        // TODO: don't support <> Type variant
        // OptTypeArguments ::= "<" TypeArgument {TypeArgument} ">".
        //  ListOfTypes typeArgs = parseOptTypeArguments();
        ParameterizedTypeImpl parentType =
                new ParameterizedTypeImpl(null, qualIdent.toString(), typeArgs, loader);
        ParameterizedTypeImpl type = parentType;

        while (symbol == '.') {
            // Deal with Member Classes:
            scanSymbol();
            scanIdentifier();
            qualIdent.append("$").append(identifier); // FIXME: is "$" correct?
            typeArgs = parseOptTypeArguments();
            type = new ParameterizedTypeImpl(parentType, qualIdent.toString(), typeArgs,
                    loader);
        }

        expect(';');

        return type;
        */
    }

    /*
    ListOfTypes parseOptTypeArguments() {
        // OptTypeArguments ::= "<" TypeArgument {TypeArgument} ">".

        ListOfTypes typeArgs = new ListOfTypes(8);
        if (symbol == '<') {
            scanSymbol();

            // TODO: bypass TypeVariableImpl
//            typeArgs.add(parseTypeArgument());
//            while ((symbol != '>') && (symbol > 0)) {
//                typeArgs.add(parseTypeArgument());
//            }
            while (symbol != '>' && symbol > 0) {
                scanSymbol();
            }
            expect('>');
        }
        return typeArgs;
    }
    */

    Type parseReturnType() {
        // ReturnType ::= TypeSignature | "V".
        if (symbol != 'V') { return parseTypeSignature(); }
        else { scanSymbol(); return void.class; }
    }

    /**
     * this last element of result is signature's return type, could be 'Void'
     * @param signature
     * @return List<Type>
     */
    // this come from GenericSignatureParser.parseMethodTypeSignature(Class<?>[] rawExceptionTypes)
    public List<Type> parseDesc(String signature) {
        setInput(signature);
        List<Type> parameterTypes = new ArrayList<>();
        expect('(');
        while (symbol != ')' && (symbol > 0)) {
            parameterTypes.add(parseTypeSignature());
        }
        expect(')');

        // return type
        parameterTypes.add(parseReturnType());
        return parameterTypes;
    }
}
