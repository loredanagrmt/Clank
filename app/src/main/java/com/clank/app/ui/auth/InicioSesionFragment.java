package com.clank.app.ui.auth;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clank.app.R;
import com.clank.app.databinding.FragmentInicioSesionBinding;

public class InicioSesionFragment extends Fragment {

    private FragmentInicioSesionBinding binding;
    private boolean contrasenaVisible = false;

    public InicioSesionFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInicioSesionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configurarVista();
        configurarListeners();
    }

    private void configurarVista() {
        binding.inputCorreo.tvInputTitulo.setText(getString(R.string.correo_electronico));
        binding.inputCorreo.customEditText.setHint(getString(R.string.ejemplo_correo));
        binding.inputCorreo.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        binding.inputCorreo.inputTrailingIcon.setVisibility(View.GONE);

        binding.inputContrasena.tvInputTitulo.setText(getString(R.string.contrasena));
        binding.inputContrasena.customEditText.setHint("••••••••");
        binding.inputContrasena.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        binding.inputContrasena.inputTrailingIcon.setVisibility(View.VISIBLE);
        binding.inputContrasena.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_oculta);

        binding.btnIniciarSesion.btnSecundario.setText(getString(R.string.iniciar_sesion));
        binding.btnRegistrarse.btnSecundario.setText(getString(R.string.registrarse));
    }

    private void configurarListeners() {
        binding.inputContrasena.inputTrailingIcon.setOnClickListener(v -> alternarVisibilidadContrasenya());
    }

    private void alternarVisibilidadContrasenya() {
        if (contrasenaVisible) {
            binding.inputContrasena.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            binding.inputContrasena.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_oculta);
            contrasenaVisible = false;
        } else {
            binding.inputContrasena.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );
            binding.inputContrasena.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_visible);
            contrasenaVisible = true;
        }

        binding.inputContrasena.customEditText.setSelection(
                binding.inputContrasena.customEditText.getText().length()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}