package com.clank.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.clank.app.databinding.ActivityMainBinding;
import com.clank.app.databinding.NavbarSuperiorBinding;
import com.clank.app.ui.comun.NavbarHost;
import com.clank.app.util.GestorIdioma;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements NavbarHost {

    private ActivityMainBinding binding;
    private NavbarSuperiorBinding navbarBinding;
    private View navbarView;
    private ImageButton btnNavbarVolver;
    private ImageButton btnNavbarAccion;
    private TextView tvNavbarTitulo;

    private FrameLayout frameBottomBar;
    private View btnNavFeed, btnNavCrear, btnNavPerfil;
    private View indicadorFeed, indicadorCrear, indicadorPerfil;

    // (RECORDAR: ocultar en Logo e Idioma, Portada, Bienvenida, InicioSesion,
    // Registro, EditarPerfil, CompletarPerfil, CambiarContrasenia, BorrarCuenta, CerrarSesion, OpcionesClankPerfil,
    // OpcionesCLankDetalle, OpcionesColeccion, Borrar y Publicar)
    private static final Set<Integer> FRAGMENTS_SIN_BOTTOMBAR = new HashSet<>(Arrays.asList(
            R.id.logoFragment,
            R.id.elegirIdiomaFragment,
            R.id.inspirarFragment,
            R.id.bienvenidaFragment,
            R.id.inicioSesionFragment,
            R.id.registroFragment,
            R.id.completarPerfilFragment,
            R.id.crearFragment,
            R.id.editarClankFragment
            // R.id.detalleFragment,
            // R.id.editarPerfilFragment
    ));

    @Override
    protected void attachBaseContext(Context newBase) {
        String idioma = newBase.getSharedPreferences("clank_prefs", Context.MODE_PRIVATE)
                .getString("idioma_seleccionado", "es");

        Locale locale = Locale.forLanguageTag(idioma);
        Locale.setDefault(locale);

        Configuration config = newBase.getResources().getConfiguration();
        config = new Configuration(config);
        config.setLocale(locale);

        Context contextConLocale = newBase.createConfigurationContext(config);
        super.attachBaseContext(contextConLocale);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.getInstance(getApplicationContext()).aplicarIdiomaGuardado();
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navbarBinding = NavbarSuperiorBinding.bind(binding.navbar.getRoot());
        configurarNavbar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        configurarBottombar();
    }

    private void configurarNavbar() {
        navbarBinding.btnNavbarVolver.setOnClickListener(v ->
                obtenerNavController().navigateUp());
    }

    private void configurarBottombar() {
        NavController nav = obtenerNavController();

        ///////////////////////// mostrar/ocultar y marcar pulsado /////////////////////////
        nav.addOnDestinationChangedListener((controller, destination, args) -> {
            if (FRAGMENTS_SIN_BOTTOMBAR.contains(destination.getId())) {
                binding.frameBottomBar.setVisibility(View.GONE);
            } else {
                binding.frameBottomBar.setVisibility(View.VISIBLE);
                actualizarIndicador(destination.getId());
            }
        });

        ///////////////////////// listeners /////////////////////////
        binding.bottomBar.btnNavFeed.setOnClickListener(v -> {
            if (fragmentActual() != R.id.feedFragment) {
                NavOptions opciones = new NavOptions.Builder()
                        .setPopUpTo(R.id.feedFragment, true)
                        .setLaunchSingleTop(true)
                        .build();

                nav.navigate(R.id.feedFragment, null, opciones);
            }
        });

        binding.bottomBar.btnNavCrear.setOnClickListener(v -> {
            if (fragmentActual() != R.id.crearFragment) {
                NavOptions opciones = new NavOptions.Builder()
                        .setPopUpTo(R.id.feedFragment, false)
                        .setLaunchSingleTop(true)
                        .build();

                nav.navigate(R.id.crearFragment, null, opciones);
            }
        });

        binding.bottomBar.btnNavPerfil.setOnClickListener(v -> {
            if (fragmentActual() != R.id.perfilFragment) {
                NavOptions opciones = new NavOptions.Builder()
                        .setPopUpTo(R.id.feedFragment, false)
                        .setLaunchSingleTop(true)
                        .build();

                nav.navigate(R.id.perfilFragment, null, opciones);
            }
        });
    }

    ///////////////////////// indicador al pulsar botones /////////////////////////
    private void actualizarIndicador(int fragmentId) {
        binding.bottomBar.indicadorFeed.setVisibility(View.INVISIBLE);
        binding.bottomBar.indicadorCrear.setVisibility(View.INVISIBLE);
        binding.bottomBar.indicadorPerfil.setVisibility(View.INVISIBLE);

        if (fragmentId == R.id.feedFragment) {
            binding.bottomBar.indicadorFeed.setVisibility(View.VISIBLE);
        } else if (fragmentId == R.id.crearFragment) {
            binding.bottomBar.indicadorCrear.setVisibility(View.VISIBLE);
        } else if (fragmentId == R.id.perfilFragment) {
            binding.bottomBar.indicadorPerfil.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void mostrarNavbar(String titulo) {
        navbarBinding.tvNavbarTitulo.setText(titulo);
        navbarBinding.btnNavbarAccion.setVisibility(View.GONE);
        binding.navbar.getRoot().setVisibility(View.VISIBLE);
    }

    @Override
    public void mostrarNavbar(String titulo, @DrawableRes int iconoAccion, View.OnClickListener onAccion) {
        navbarBinding.tvNavbarTitulo.setText(titulo);
        navbarBinding.btnNavbarAccion.setImageResource(iconoAccion);
        navbarBinding.btnNavbarAccion.setOnClickListener(onAccion);
        navbarBinding.btnNavbarAccion.setVisibility(View.VISIBLE);
        binding.navbar.getRoot().setVisibility(View.VISIBLE);
    }

    @Override
    public void ocultarNavbar() {
        binding.navbar.getRoot().setVisibility(View.GONE);
    }

    private NavController obtenerNavController() {
        return Navigation.findNavController(this, R.id.nav_host_fragment);
    }

    private int fragmentActual() {
        NavController nav = obtenerNavController();
        return nav.getCurrentDestination() != null
                ? nav.getCurrentDestination().getId()
                : -1;
    }
}