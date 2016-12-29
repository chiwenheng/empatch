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

/**
 * Created by linan on 16/8/22.
 */
public class EMLog {

    public static final boolean DEBUG = false;

    enum LEVEL {
        INFO,
        DEBUG,
        ERROR,
    }
    static LEVEL level = LEVEL.DEBUG;


    static void setLevel(LEVEL l) {
        level = l;
    }

    public static void i(String TAG, String log) {
        if (level.ordinal() > LEVEL.INFO.ordinal()) { return; }
        System.out.println("["+TAG+"] " + log);
    }
    public static void d(String TAG, String log) {
        if (level.ordinal() > LEVEL.DEBUG.ordinal()) { return; }
        System.out.println("["+TAG+"] " + log);
    }
    public static void e(String TAG, String log) {
        System.out.println("["+TAG+"] " + log);
    }

    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_BLACK = "\u001B[30m";
    static final String ANSI_RED = "\u001B[31m";
    static final String ANSI_GREEN = "\u001B[32m";
    static final String ANSI_YELLOW = "\u001B[33m";
    static final String ANSI_BLUE = "\u001B[34m";
    static final String ANSI_PURPLE = "\u001B[35m";
    static final String ANSI_CYAN = "\u001B[36m";
    static final String ANSI_WHITE = "\u001B[37m";

    public enum COLOR {
        BLACK,
        RED,
        GREEN,
        YELLOW,
        BLUE,
        PURPLE,
        CYAN,
        WHITE,
    }

    public static void e(String TAG, String log, COLOR color) {
        d(TAG, log, color);
    }
    public static void d(String TAG, String log, COLOR color) {
        String colorLog = log;
        switch (color) {
            case BLACK:
                colorLog = ANSI_BLACK + log + ANSI_RESET;
                break;
            case RED:
                colorLog = ANSI_RED + log + ANSI_RESET;
                break;
            case GREEN:
                colorLog = ANSI_GREEN + log + ANSI_RESET;
                break;
            case YELLOW:
                colorLog = ANSI_YELLOW + log + ANSI_RESET;
                break;
            case BLUE:
                colorLog = ANSI_BLUE + log + ANSI_RESET;
                break;
            case PURPLE:
                colorLog = ANSI_PURPLE + log + ANSI_RESET;
                break;
            case CYAN:
                colorLog = ANSI_CYAN + log + ANSI_RESET;
                break;
            case WHITE:
                colorLog = ANSI_WHITE + log + ANSI_RESET;
                break;
            default:
                break;
        }
        System.out.println("["+TAG+"] " + colorLog);
    }
}
