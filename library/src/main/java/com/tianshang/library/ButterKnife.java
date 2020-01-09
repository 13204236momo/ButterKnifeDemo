package com.tianshang.library;

import android.app.Activity;

/**
 * 核心类
 */
public class ButterKnife {
    public static void bind(Activity activity){
        // 拼接类名，如：MainActivity$ViewBinder
        String className = activity.getClass().getName() +"$ViewBinder";

        try {
            //加载上述拼接类
            Class<?> viewBinderClass = Class.forName(className);
            //接口 = 接口实现类
            ViewBinder viewBinder = (ViewBinder) viewBinderClass.newInstance();
            //调用接口方法
            viewBinder.bind(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
