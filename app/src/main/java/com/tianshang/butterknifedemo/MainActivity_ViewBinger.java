package com.tianshang.butterknifedemo;

import android.view.View;

import com.tianshang.library.DebouncingOnClickListener;
import com.tianshang.library.ViewBinder;

public class MainActivity_ViewBinger implements ViewBinder<MainActivity> {
    @Override
    public void bind(final MainActivity target) {
        target.tv1 = target.findViewById(R.id.tv_1);
        target.tv2 = target.findViewById(R.id.tv_2);
        target.tv1.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            public void doClick(View view) {
                target.onClick(view);
            }
        });
    }
}
