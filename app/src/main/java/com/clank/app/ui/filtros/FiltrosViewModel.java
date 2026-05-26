package com.clank.app.ui.filtros;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Categoria;
import com.clank.app.data.repository.CategoriaRepository;
import com.clank.app.util.TraductorCategorias;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FiltrosViewModel extends ViewModel {

    private final MutableLiveData<List<Categoria>> _categorias = new MutableLiveData<>();
    public LiveData<List<Categoria>> categorias = _categorias;

    private final MutableLiveData<Boolean> errorCargando = new MutableLiveData<>(false);

    private final CategoriaRepository categoriaRepository;
    private final TraductorCategorias traductorCategorias;

    @Inject
    public FiltrosViewModel(CategoriaRepository categoriaRepository,
                            TraductorCategorias traductorCategorias) {
        this.categoriaRepository = categoriaRepository;
        this.traductorCategorias = traductorCategorias;
        cargarCategorias();
    }

    public LiveData<Boolean> getErrorCargando() {
        return errorCargando;
    }

    private void cargarCategorias() {
        errorCargando.setValue(false);

        categoriaRepository.getTodas()
                .addOnSuccessListener(snapshot -> {
                    List<Categoria> lista = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Categoria categoria = doc.toObject(Categoria.class);

                        if (categoria != null) {
                            lista.add(categoria);
                        }
                    }

                    traducirCategorias(lista);
                })
                .addOnFailureListener(error -> {
                    android.util.Log.e(
                            "FILTROS_DEBUG",
                            "Error cargando categorías",
                            error
                    );

                    _categorias.setValue(new ArrayList<>());
                    errorCargando.setValue(true);
                });
    }

    private void traducirCategorias(List<Categoria> categoriasOriginales) {
        List<String[]> categoriasParaTraducir =
                convertirATuplas(categoriasOriginales);

        traductorCategorias.traducirSiProcede(categoriasParaTraducir)
                .addOnSuccessListener(categoriasTraducidas -> {
                    errorCargando.setValue(false);

                    _categorias.setValue(
                            convertirACategorias(categoriasTraducidas)
                    );
                })
                .addOnFailureListener(error -> {
                    android.util.Log.e(
                            "FILTROS_DEBUG",
                            "Error traduciendo categorías",
                            error
                    );

                    errorCargando.setValue(false);

                    _categorias.setValue(
                            categoriasOriginales != null
                                    ? categoriasOriginales
                                    : new ArrayList<>()
                    );
                });
    }

    private List<String[]> convertirATuplas(List<Categoria> categoriasOriginales) {
        List<String[]> lista = new ArrayList<>();

        if (categoriasOriginales == null) {
            return lista;
        }

        for (Categoria categoria : categoriasOriginales) {
            if (categoria == null) {
                continue;
            }

            String id =
                    categoria.getCatId() != null
                            ? categoria.getCatId()
                            : "";

            String nombre =
                    categoria.getCategoria() != null
                            ? categoria.getCategoria()
                            : "";

            lista.add(new String[]{id, nombre});
        }

        return lista;
    }

    private List<Categoria> convertirACategorias(List<String[]> categoriasTraducidas) {
        List<Categoria> lista = new ArrayList<>();

        if (categoriasTraducidas == null) {
            return lista;
        }

        for (String[] categoriaTraducida : categoriasTraducidas) {
            if (categoriaTraducida == null) {
                continue;
            }

            Categoria categoria = new Categoria();

            categoria.setCatId(
                    categoriaTraducida.length > 0
                            && categoriaTraducida[0] != null
                            ? categoriaTraducida[0]
                            : ""
            );

            categoria.setCategoria(
                    categoriaTraducida.length > 1
                            && categoriaTraducida[1] != null
                            ? categoriaTraducida[1]
                            : ""
            );

            lista.add(categoria);
        }

        return lista;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        traductorCategorias.cerrar();
    }
}