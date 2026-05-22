package com.clank.app;

import android.app.Application;
import android.content.Context;

import dagger.hilt.android.testing.HiltTestApplication;
import io.qameta.allure.android.runners.AllureAndroidJUnitRunner;

public class HiltTestRunner extends AllureAndroidJUnitRunner {

    @Override
    public Application newApplication(
            ClassLoader cl,
            String className,
            Context context
    ) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(
                cl,
                HiltTestApplication.class.getName(),
                context
        );
    }
}