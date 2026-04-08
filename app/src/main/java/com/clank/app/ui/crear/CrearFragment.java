package com.clank.app.ui.crear;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.clank.app.data.model.Clank;
import com.clank.app.databinding.FragmentCrearBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CrearFragment extends Fragment {

  private CrearViewModel viewModel;
  private FragmentCrearBinding binding;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    binding = FragmentCrearBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(CrearViewModel.class);

    viewModel.guardarEstado.observe(getViewLifecycleOwner(), resource -> {
      switch (resource.status) {
        case LOADING:
          binding.progressBar.setVisibility(View.VISIBLE);
          break;
        case SUCCESS:
          binding.progressBar.setVisibility(View.GONE);
          break;
        case ERROR:
          binding.progressBar.setVisibility(View.GONE);
          Toast.makeText(getContext(), resource.message, Toast.LENGTH_SHORT).show();
          break;
      }
    });

    binding.btnGuardar.setOnClickListener(v -> {
      Clank t = new Clank();
      t.setTitulo(binding.etTitulo.getText().toString());
      viewModel.guardarClank(t);
    });
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
