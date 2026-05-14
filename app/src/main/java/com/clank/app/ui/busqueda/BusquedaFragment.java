package com.clank.app.ui.busqueda;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.clank.app.R;
import com.clank.app.adapters.BusquedaAdapter;
import com.clank.app.data.repository.UsuarioRepository;
import com.clank.app.databinding.FragmentBusquedaBinding;
import com.clank.app.ui.comun.NavbarHost;
import android.graphics.Rect;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BusquedaFragment extends Fragment {

  private FragmentBusquedaBinding binding;
  private BusquedaViewModel viewModel;
  private BusquedaAdapter adapter;

  @Inject
  UsuarioRepository usuarioRepository;

  /////////////////////////ciclo de vida/////////////////////////
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentBusquedaBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(BusquedaViewModel.class);

    configurarRecyclerView();
    configurarCampoBusqueda();
    observarViewModel();
    abrirTeclado();
  }

  @Override
  public void onResume() {
    super.onResume();
    ((NavbarHost) requireActivity()).ocultarNavbar();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  /////////////////////////recyclerView/////////////////////////
  private void configurarRecyclerView() {
    adapter = new BusquedaAdapter(
      requireContext(),
      usuarioRepository,
      uid -> viewModel.getUsuarioCacheado(uid),
      clankId -> {
        Bundle args = new Bundle();
        args.putString("clankId", clankId);
        Navigation.findNavController(requireView()).navigate(R.id.action_busqueda_a_detalle_clank, args);
      }
    );

    binding.rvBusqueda.setLayoutManager(
      new LinearLayoutManager(requireContext()));
    binding.rvBusqueda.setHasFixedSize(false);

    int spacing = (int) getResources().getDimension(R.dimen.feed_item_spacing);
    binding.rvBusqueda.addItemDecoration(new RecyclerView.ItemDecoration() {
      @Override
      public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos > 0) outRect.top = spacing;
      }
    });

    binding.rvBusqueda.setAdapter(adapter);
  }

  /////////////////////////campo de búsqueda/////////////////////////
  private void configurarCampoBusqueda() {
    // volver atrás
    binding.btnBusquedaVolver.setOnClickListener(v ->
      Navigation.findNavController(requireView()).navigateUp());

    //busqueda en tiempo real mientras escribes
    binding.etBusqueda.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void afterTextChanged(Editable s) {
        String query = s != null ? s.toString() : "";
        if (query.trim().isEmpty()) {
          viewModel.limpiar();
        } else {
          viewModel.buscar(query);
        }
      }
    });

    binding.etBusqueda.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        ocultarTeclado();
        return true;
      }
      return false;
    });
  }

  /////////////////////////observadores/////////////////////////
  private void observarViewModel() {
    viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
      binding.progressBusqueda.setVisibility(Boolean.TRUE.equals(cargando) ? View.VISIBLE : View.GONE);
    });

    viewModel.getResultados().observe(getViewLifecycleOwner(), clanks -> {
      adapter.actualizar(clanks);

      String query = binding.etBusqueda.getText() != null
        ? binding.etBusqueda.getText().toString().trim() : "";

      if (!query.isEmpty() && (clanks == null || clanks.isEmpty())) {
        binding.tvBusquedaVacio.setText(getString(R.string.busqueda_sin_resultados, query));
        binding.tvBusquedaVacio.setVisibility(View.VISIBLE);
        binding.rvBusqueda.setVisibility(View.GONE);
      } else {
        binding.tvBusquedaVacio.setVisibility(View.GONE);
        binding.rvBusqueda.setVisibility(View.VISIBLE);
      }
    });

    viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
      if (msg != null && !msg.isEmpty()) {
        binding.tvBusquedaVacio.setText(getString(R.string.error_generico));
        binding.tvBusquedaVacio.setVisibility(View.VISIBLE);
      }
    });
  }

  /////////////////////////teclado/////////////////////////
  private void abrirTeclado() {
    binding.etBusqueda.requestFocus();
    binding.etBusqueda.post(() -> {
      InputMethodManager imm = (InputMethodManager)
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm != null) {
        imm.showSoftInput(binding.etBusqueda,
          InputMethodManager.SHOW_IMPLICIT);
      }
    });
  }

  private void ocultarTeclado() {
    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null && binding != null) {
      imm.hideSoftInputFromWindow(binding.etBusqueda.getWindowToken(), 0);
    }
  }
}
