package com.gaochen.signal;

public interface ISignalLauncher<T> {

    void launch(ISignalSender<T> sender);

}
