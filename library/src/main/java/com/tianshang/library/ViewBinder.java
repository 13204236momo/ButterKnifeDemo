package com.tianshang.library;

/**
 * 接口绑定类（所有注解处理器生成的类，都要实现该接口）
 */
public interface ViewBinder<T> {
    void bind(T target);
}
