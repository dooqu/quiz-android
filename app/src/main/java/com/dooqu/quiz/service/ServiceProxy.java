package com.dooqu.quiz.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.lang.ref.WeakReference;

/*
封装了用来使用aidl方式bind小冰服务的调用
 */
public abstract class ServiceProxy {
    static class BinderServiceConnection implements ServiceConnection {
        WeakReference<ServiceProxy> binderWeakReference;

        public BinderServiceConnection(ServiceProxy proxy) {
            binderWeakReference = new WeakReference<>(proxy);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            ServiceProxy proxy = binderWeakReference.get();
            if (proxy != null) {
                proxy.connected = true;
                if (proxy.contextWeakReference.get() != null) {
                    proxy.onConnected(true,  serviceBinder);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ServiceProxy binder = binderWeakReference.get();
            if (binder != null) {
                binder.connected = false;
                if (binder.contextWeakReference.get() != null) {
                    binder.onConnected(false, null);
                }
            }
        }
    }

    Class<? extends IBinder> binderType;
    private ServiceConnection connection;
    private boolean connected;
    private WeakReference<Context> contextWeakReference;
    private Intent intent;

    public ServiceProxy(Context context, String serviceAction, String packageName) {
        this.contextWeakReference = new WeakReference<>(context);
        intent = new Intent();
        intent.setAction(serviceAction);
        intent.setPackage(packageName);
        this.connection = new BinderServiceConnection(this);
    }


    public synchronized boolean bind() {
        Context context = null;
        if (connected == true || (context = contextWeakReference.get()) == null) {
            return false;
        }
        return context.bindService(intent, this.connection, context.BIND_AUTO_CREATE);
    }

    public synchronized void unbind() {
        Context context = contextWeakReference.get();
        if (connected && context != null) {
            context.unbindService(connection);
        }
        contextWeakReference.clear();
        this.connection = null;
        this.connected = false;
    }

    protected abstract void onConnected(boolean connected, IBinder serviceBinder);

    public boolean isConnected() {
        return connected;
    }
}