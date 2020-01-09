package com.tianshang.library;

import android.view.View;

public abstract class DebouncingOnClickListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
        //调用抽象方法
        doClick(v);
    }

    abstract void doClick(View view);
}
