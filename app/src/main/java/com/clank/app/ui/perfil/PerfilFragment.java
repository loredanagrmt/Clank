package com.clank.app.ui.perfil;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.adapters.ClanksAdapter;
import com.clank.app.data.model.Clank;
import com.clank.app.databinding.FragmentPerfilBinding;
import com.clank.app.ui.comun.NavbarHost;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PerfilFragment extends Fragment {

  private static final String ID_USER    = "idUser";
  private static final String TAB_INICIAL = "tabInicial";

  private FragmentPerfilBinding binding;
  private PerfilViewModel viewModel;
  private ClanksAdapter adapter;
  private String idUser;
  private boolean mostrandoClanks = true;

  public static PerfilFragment newInstance(String idUser) {
    PerfilFragment f = new PerfilFragment();
    Bundle args = new Bundle();
    args.putString(ID_USER, idUser);
    f.setArguments(args);
    return f;
  }

  /////////////////////////ciclo de vida/////////////////////////
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentPerfilBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(PerfilViewModel.class);

    if (getArguments() != null)
      idUser = getArguments().getString(ID_USER, "");
    if (idUser == null || idUser.isEmpty())
      idUser = viewModel.getCurrentUserId();

    //si venimos de editar/crear abre el tab indicado
    String tabInicial = getArguments() != null
      ? getArguments().getString(TAB_INICIAL, "clanks") : "clanks";
    mostrandoClanks = !"bocetos".equals(tabInicial);

    configurarRecyclerView();
    configurarTabs();
    configurarBotones();
    observarViewModel();

    viewModel.cargarDatos(idUser);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((NavbarHost) requireActivity()).ocultarNavbar();
  }

  @Override
  public void onStart() {
    super.onStart();
    if (adapter != null) adapter.startListening();
  }

  @Override
  public void onStop() {
    super.onStop();
    if (adapter != null) adapter.stopListening();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  /////////////////////////recyclerView/////////////////////////
  private void configurarRecyclerView() {
    binding.rvClanks.setLayoutManager(
      new GridLayoutManager(requireContext(), 2));
    binding.rvClanks.setHasFixedSize(false);
  }

  /////////////////////////tabs clanks y borradores/////////////////////////
  private void configurarTabs() {
    binding.tabClanks.setOnClickListener(v -> {
      if (!mostrandoClanks) {
        mostrandoClanks = true;
        animarIndicador(true);
        cargarAdapter(true);
      }
    });
    binding.tabBocetos.setOnClickListener(v -> {
      if (mostrandoClanks) {
        mostrandoClanks = false;
        animarIndicador(false);
        cargarAdapter(false);
      }
    });

    binding.indicadorTab.getViewTreeObserver().addOnGlobalLayoutListener(
      new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          binding.indicadorTab.getViewTreeObserver()
            .removeOnGlobalLayoutListener(this);
          //indicador en el tab correcto
          View tabActivo = mostrandoClanks ? binding.tabClanks : binding.tabBocetos;
          ajustarAnchoIndicador(tabActivo);
          binding.indicadorTab.setTranslationX(tabActivo.getLeft());
        }
      });
  }

  private void animarIndicador(boolean haciaClanks) {
    View tabDestino = haciaClanks ? binding.tabClanks : binding.tabBocetos;
    float destino = tabDestino.getLeft();

    ValueAnimator anim = ValueAnimator.ofFloat(
      binding.indicadorTab.getTranslationX(), destino);
    anim.setDuration(200);
    anim.addUpdateListener(a ->
      binding.indicadorTab.setTranslationX((float) a.getAnimatedValue()));
    anim.start();

    ajustarAnchoIndicador(tabDestino);
  }

  private void ajustarAnchoIndicador(View tab) {
    ViewGroup.LayoutParams lp = binding.indicadorTab.getLayoutParams();
    lp.width = tab.getWidth();
    binding.indicadorTab.setLayoutParams(lp);
  }
  //RECORDAR: comprobar si difrencia clank de boceto
  /////////////////////////adapter/////////////////////////

  private void cargarAdapter(boolean soloAcabados) {
    if (adapter != null) adapter.stopListening();

    FirestoreRecyclerOptions<Clank> options = soloAcabados
      ? viewModel.buildClankOptionsAcabados(idUser)
      : viewModel.buildClankOptionsBocetos(idUser);

    //dentro de cargarAdapter(), sustituir el listener:
    adapter = new ClanksAdapter(
      options,
      requireContext(),
      viewModel.getUsuarioRepository(),
      true,
      clankId -> {
        Bundle args = new Bundle();
        args.putString("clankId", clankId);
        Navigation.findNavController(requireView())
          .navigate(R.id.action_perfil_a_detalle_clank, args);
      }
    );
    binding.rvClanks.setAdapter(adapter);
    adapter.startListening();
  }

  /////////////////////////botones/////////////////////////
  private void configurarBotones() {
    binding.btnAjustes.setOnClickListener(v -> {
      //RECORDAR: cuando lore acabe ajustes, añadir la navegación
    });

    if (viewModel.esPerfilPropio(idUser)) {
      binding.tvEditarPerfil.setVisibility(View.VISIBLE);
      binding.tvEditarPerfil.setOnClickListener(v -> {
        //RECORDAR: para cuando acabemos editar perfil
      });
    } else {
      binding.tvEditarPerfil.setVisibility(View.GONE);
    }
  }

  /////////////////////////observadores/////////////////////////
  private void observarViewModel() {
    viewModel.getPerfil().observe(getViewLifecycleOwner(), perfil -> {
      if (perfil == null) return;

      binding.tvNombrePerfil.setText(perfil.nombre);
      binding.tvUidPerfil.setText(
        !perfil.correo.isEmpty()
          ? "@" + perfil.correo.split("@")[0]
          : "");

      if (!perfil.fotoPerfil.isEmpty()) {
        Glide.with(this)
          .load(perfil.fotoPerfil)
          .circleCrop()
          .placeholder(R.drawable.ic_usuario_inactivo)
          .into(binding.civFotoPerfil);
      }

      //carga el adapter solo la primera vez
      if (adapter == null) cargarAdapter(mostrandoClanks);
    });

    viewModel.getNumClanks().observe(getViewLifecycleOwner(), num ->
      binding.tvNumClanks.setText(String.valueOf(num != null ? num : 0)));

    viewModel.getNumBocetos().observe(getViewLifecycleOwner(), num ->
      binding.tvNumBocetos.setText(String.valueOf(num != null ? num : 0)));
  }
}
