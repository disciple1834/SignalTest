package com.gaochen.signaltest;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gaochen.signal.ISignalShifter;
import com.gaochen.signal.ISignalWatcher;
import com.gaochen.signal.Signal;
import com.gaochen.signal.WatchTag;

public class TestFragment extends Fragment implements ISignalWatcher<String> {

    public Signal<String> mSignal;
    TextView mTv;
    Button mBtn;

    public static TestFragment newInstance(Signal<String> signal){
        TestFragment fragment = new TestFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("signal",signal);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        mSignal = (Signal<String>) getArguments().getSerializable("signal");
    }

    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState){
        View fragmentView = inflater.inflate(R.layout.fragment_layout,container,false);
        mTv = fragmentView.findViewById(R.id.tv);
        mBtn = fragmentView.findViewById(R.id.btn);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignal.shift(new ISignalShifter<String, String>() {
                    @Override
                    public String shift(String o) {
                        Log.d("SignalTest","Shifter receive message:" + o + " in Thread:" + Thread.currentThread());
                        return "JOB DONE";
                    }
                }).throwToSubThread()
                        .setupWatcher(TestFragment.this)
                        .watchOn(Signal.ThreadToWatch.MAIN_THREAD);
            }
        });
        return fragmentView;
    }

    @Override
    public void onWatch(String o) {
//        mTv.setText(o);
    }

    /*
     * Use WatchTag to mark the function you want to name
     * which can show better your business flow.When a function
     * is marked,the onWatch will never invoke again.Make sure
     * the args and return type be matched.
     */
    @WatchTag
    public void jobDone(String o){
        mTv.setText(o);
    }
}
