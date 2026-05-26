package com.clank.app.ui.filtros;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Categoria;
import com.clank.app.data.repository.CategoriaRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FiltrosViewModel extends ViewModel {

    private final MutableLiveData<List<Categoria>> _categorias = new MutableLiveData<>();
    public LiveData<List<Categoria>> categorias = _categorias;
  private final MutableLiveData<Boolean> errorCargando = new MutableLiveData<>();
  public LiveData<Boolean> getErrorCargando() { return errorCargando; }

    private final CategoriaRepository categoriaRepository;

    @Inject
    public FiltrosViewModel(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
        cargarCategorias();
    }

    private void cargarCategorias() {
        categoriaRepository.getTodas()
                .addOnSuccessListener(snapshot -> {
                    List<Categoria> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        Categoria cat = doc.toObject(Categoria.class);
                        if (cat != null) lista.add(cat);
                    }
                    _categorias.setValue(lista);
                }).addOnFailureListener(e -> {
            _categorias.setValue(new ArrayList<>());
            errorCargando.setValue(true);
          });
    }

}
