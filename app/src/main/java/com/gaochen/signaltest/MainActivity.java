package com.gaochen.signaltest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.gaochen.signal.ISignalLauncher;
import com.gaochen.signal.ISignalSender;
import com.gaochen.signal.Signal;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Signal<String> signal = Signal.CREATE(new ISignalLauncher<String>() {
            @Override
            public void launch(ISignalSender<String> sender) {
                Log.d("SignalTest", "JOB START in Thread:" + Thread.currentThread());
                sender.sendWatch("JOB START");
            }
        });

        getSupportFragmentManager().beginTransaction().add(R.id.container, TestFragment.newInstance(signal)).commit();
    }
}
