package com.clank.app.test.util;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.rules.ExternalResource;

public class FirebaseEmulatorRule extends ExternalResource {

    private static boolean configurado = false;

    @Override
    protected void before() {
        if (configurado) {
            return;
        }

        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);

        configurado = true;
    }
}