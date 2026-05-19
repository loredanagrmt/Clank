package com.clank.app.ui.logo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.lifecycle.ViewModelProvider;
import dagger.hilt.android.AndroidEntryPoint;

import com.clank.app.R;
import com.clank.app.util.GestorIdioma;

@AndroidEntryPoint
public class LogoFragment extends Fragment {

    private LogoViewModel vistaModelo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable navegarRunnable = new Runnable() {
        @Override
        public void run() {
            View vista = getView();

            if (vista == null) {
                return;
            }

            if (vistaModelo.haySesionIniciada()) {
                Navigation.findNavController(vista)
                        .navigate(R.id.action_logo_a_feed);
            } else {
                Navigation.findNavController(vista)
                        .navigate(R.id.action_logo_a_idioma);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vistaModelo = new ViewModelProvider(this).get(LogoViewModel.class);

        handler.postDelayed(navegarRunnable, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(navegarRunnable);
    }
}