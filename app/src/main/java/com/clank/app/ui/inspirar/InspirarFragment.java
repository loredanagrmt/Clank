package com.clank.app.ui.inspirar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.databinding.FragmentInspirarBinding;

public class InspirarFragment extends Fragment {

    private FragmentInspirarBinding binding;
    private InspirarViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInspirarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(InspirarViewModel.class);

        binding.btnContinuar.getRoot().setText(getString(R.string.continuar));
        binding.btnContinuar.getRoot().setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_inspirar_a_bienvenida)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}