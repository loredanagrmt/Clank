package com.clank.app.ui.filtros;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Categoria;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FiltrosViewModel extends ViewModel {

    private final MutableLiveData<List<Categoria>> _categorias = new MutableLiveData<>();
    public LiveData<List<Categoria>> categorias = _categorias;

    private final FirebaseFirestore db;

    @Inject
    public FiltrosViewModel(FirebaseFirestore db) {
        this.db = db;
        cargarCategorias();
    }

    private void cargarCategorias() {
        db.collection("categorias")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Categoria> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        Categoria cat = doc.toObject(Categoria.class);
                        if (cat != null) lista.add(cat);
                    }
                    _categorias.setValue(lista);
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("FILTROS_DEBUG", "Error cargando categorías", e)
                );
    }

}
