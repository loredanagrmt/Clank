package com.clank.app.data.repository;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImagenRepository {

    private final FirebaseStorage storage;

    @Inject
    public ImagenRepository(FirebaseStorage storage) {
        this.storage = storage;
    }

    public Task<Uri> guardarFotoPerfil(Context context, Uri uri, String uid) {
        StorageReference ref = storage.getReference()
                .child(uid)
                .child("fotos_perfil")
                .child(System.currentTimeMillis() + ".jpg");
        return subir(uri, ref);
    }

    public Task<Uri> guardarFotoPortada(Context context, Uri uri, String uid) {
        StorageReference ref = storage.getReference()
                .child(uid)
                .child("fotos_portada")
                .child(System.currentTimeMillis() + ".jpg");
        return subir(uri, ref);
    }

    public Task<Uri> guardarPortadaClank(Uri uri, String uid, String clankId) {
        StorageReference ref = storage.getReference()
                .child(uid)
                .child(clankId)
                .child("portada")
                .child(System.currentTimeMillis() + ".jpg");
        return subir(uri, ref);
    }

    public Task<Uri> guardarImagenInstruccion(Uri uri, String uid, String clankId, int orden) {
        StorageReference ref = storage.getReference()
                .child(uid)
                .child(clankId)
                .child("instrucciones")
                .child("inst_" + orden + ".jpg");
        return subir(uri, ref);
    }

    private Task<Uri> subir(Uri uri, StorageReference ref) {
        return ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        });
    }
}