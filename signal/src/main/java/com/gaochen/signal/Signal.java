package com.gaochen.signal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Signal<T> implements Serializable {

    public enum ThreadToWatch{
        CURRENT_THREAD,
        MAIN_THREAD,
    }

    static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0,Integer.MAX_VALUE,2 * 60,TimeUnit.SECONDS,new SynchronousQueue<Runnable>()); // here use cached thread pool

    ISignalLauncher<T> mLauncher;
    ISignalWatcher<T> mWatcher;

    private Signal(){}

    public static<S> Signal<S> CREATE(ISignalLauncher<S> launcher){
        Signal<S> signal = new Signal<>();
        signal.mLauncher = launcher;
        return signal;
    }

    public Signal<T> setupWatcher(ISignalWatcher<T> watcher){
        mWatcher = watcher;
        return this;
    }

    public void watchOn(ThreadToWatch whichToWatch){
        SignalSender<T> sender = new SignalSender(mWatcher,whichToWatch == ThreadToWatch.MAIN_THREAD ? true : false);
        mLauncher.launch(sender);
    }

    /*
     * convert to a new signal to deal with the old signal,as same as shift.
     */
    public Signal<T> throwToSubThread(){
        Signal<T> subSignal = Signal.CREATE(new ISignalLauncher<T>() {
            @Override
            public void launch(final ISignalSender<T> sender) {
                final SignalSender<T> subSender = new SignalSender<>(new ISignalWatcher<T>() {
                    @Override
                    public void onWatch(T o) {
                        sender.sendWatch(o);
                    }
                });

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        mLauncher.launch(subSender);
                    }
                };
                threadPool.execute(runnable);
            }
        });
        return subSignal;
    }

    public <E> Signal<E> shift(final ISignalShifter<T,E> shifter){
        Signal<E> subSignal = Signal.CREATE(new ISignalLauncher<E>() {
            @Override
            public void launch(final ISignalSender<E> sender) {
                SignalSender<T> subSender = new SignalSender<>(new ISignalWatcher<T>() {
                    @Override
                    public void onWatch(T o) {
                        E shiftData = shifter.shift(o);
                        sender.sendWatch(shiftData);
                    }
                });
                mLauncher.launch(subSender);
            }
        });
        return subSignal;
    }

    static class SignalSender<S> implements ISignalSender<S>{

        ISignalWatcher<S> sWatcher;
        Handler sHandler;

        public SignalSender(ISignalWatcher<S> watcher){
            sWatcher = watcher;
        }

        public SignalSender(ISignalWatcher<S> watcher,boolean bSendToMain){
            sWatcher = watcher;
            if(bSendToMain){
                sHandler = new Handler(Looper.getMainLooper()){
                    public void handleMessage(Message msg){
//                        sWatcher.onWatch((S) msg.obj);
                        doWatch((S) msg.obj);
                    }
                };
            }
        }

        @Override
        public void sendWatch(S o) {
            if(sHandler == null){
//                sWatcher.onWatch(o);
                doWatch(o);
            }else {
                Message msg = Message.obtain();
                msg.obj = o;
                sHandler.sendMessage(msg);
            }
        }

        private void doWatch(S o){
            Method onWatchMethod = null;
            Method[] methods = sWatcher.getClass().getDeclaredMethods();
            for (int i = 0;i < methods.length;i++){
                Method method = methods[i];
                if(method.getAnnotation(WatchTag.class) != null){
                    Class[] paramClazz = method.getParameterTypes();
                    Type rtType = method.getGenericReturnType();
                    if(paramClazz != null && paramClazz.length == 1 && o.getClass() == paramClazz[0] && rtType.equals(Void.TYPE)){
                        onWatchMethod = method;
                        break;
                    }
                }
            }

            if(onWatchMethod == null){
                sWatcher.onWatch(o);
            }else {
                try {
                    onWatchMethod.invoke(sWatcher,o);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
