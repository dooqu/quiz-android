package com.dooqu.quiz.service;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

public class SkillOutputStream extends OutputStream {
    FileOutputStream fileOutputStream;
    WeakReference<Context> contextWeakReference;
    String skillFilePath;
    int frameCount = 0;

    public SkillOutputStream(Context context, String skillId) throws FileNotFoundException {
        contextWeakReference = new WeakReference<>(context);
        skillFilePath = context.getCacheDir().getPath() + "/" + skillId + ".mp3";
        File logFile = new File(skillFilePath);
        if (logFile.exists()) {
            logFile.delete();
        }
        try {
            fileOutputStream = new FileOutputStream(skillFilePath, false);
        }
        catch (FileNotFoundException ex) {
            throw ex;
        }
    }

    public String getSkillFilePath() {
        return skillFilePath;
    }

    @Override
    public void write(int i) throws IOException {
        fileOutputStream.write(i);
    }

    @Override
    public void write(byte[] b) throws IOException {
        fileOutputStream.write(b);
        frameCount ++;
    }


    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        fileOutputStream.write(b, off, len);
        frameCount ++;
    }

    @Override
    public void close() throws IOException {
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
        fileOutputStream = null;
        frameCount = 0;
    }

    public int getFrameCount() {
        return frameCount;
    }
}
