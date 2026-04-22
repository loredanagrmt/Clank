package com.clank.app.ui.bienvenida;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.clank.app.R;

public class BienvenidaFragment extends Fragment {

    public BienvenidaFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bienvenida, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnSoyNuevo = view.findViewById(R.id.btnSoyNuevo);
        Button btnYaHeEstado = view.findViewById(R.id.btnYaHeEstado);

        btnSoyNuevo.setText(getString(R.string.soy_nuevo));
        btnYaHeEstado.setText(getString(R.string.ya_he_estado));

        NavController navController = Navigation.findNavController(view);

        btnSoyNuevo.setOnClickListener(v ->
                navController.navigate(R.id.action_bienvenida_a_registro)
        );

        btnYaHeEstado.setOnClickListener(v ->
                navController.navigate(R.id.action_bienvenida_a_inicio_sesion)
        );
    }
}