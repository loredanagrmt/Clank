package com.clank.app.test.util;

import androidx.test.espresso.idling.CountingIdlingResource;

public class EspressoIdlingResource {

    private static final String RESOURCE = "CLANK_ASYNC";
    private static final CountingIdlingResource instance =
            new CountingIdlingResource(RESOURCE);

    public static CountingIdlingResource getIdlingResource() {
        return instance;
    }

    public static void incrementar() {
        instance.increment();
    }

    public static void decrementar() {
        if (!instance.isIdleNow()) {
            instance.decrement();
        }
    }
}
