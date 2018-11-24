package com.gaochen.signal;

public interface ISignalShifter<T,E> {

    E shift(T o);

}
