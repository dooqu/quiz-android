package com.dooqu.quiz.utils;

public class ThreadUtil {
    public static boolean safeSleep(long mill) {
        try {
            Thread.sleep(mill);
            return true;
        }
        catch (InterruptedException ex) {
        }
        return false;
    }
}
