package com.panda912.agpbytecodemanipulationdemo;

import android.util.Log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class ActivityLifecycleTraceAspect {

  // 切面表达式，声明需要过滤的类和方法
  @Pointcut("execution(* android.app.Activity+.onCreate(..))")
  public void callMethod() {
  }

  // Before 表示在方法调用前织入
  @Before("callMethod()")
  public void beforeMethodCall(ProceedingJoinPoint joinPoint) {
    Log.i("AspectJ", "onCreate");
  }
}
