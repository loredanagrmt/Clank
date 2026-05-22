package com.clank.app.ui.perfil;

import android.animation.ValueAnimator;
import android.graphics.Rect;
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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.adapters.ClanksAdapter;
import com.clank.app.data.model.Clank;
import com.clank.app.databinding.FragmentPerfilBinding;
import com.clank.app.ui.comun.HojaOpciones;
import com.clank.app.ui.comun.ItemOpcion;
import com.clank.app.ui.comun.NavbarHost;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PerfilFragment extends Fragment {

    private static final String ID_USER = "usuarioId";
    private static final String TAB_INICIAL = "tabInicial";

    private final java.util.Set<String> observadosLikes = new java.util.HashSet<>();

    private FragmentPerfilBinding binding;
    private PerfilViewModel viewModel;
    private ClanksAdapter adapter;
    private String idUser;
    private boolean mostrandoClanks = true;

    public static PerfilFragment newInstance(String idUser) {
        PerfilFragment f = new PerfilFragment();
        Bundle args = new Bundle();
        args.putString("usuarioId", idUser);
        f.setArguments(args);
        return f;
    }

    ///////////////////////// ciclo de vida /////////////////////////

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PerfilViewModel.class);

        if (getArguments() != null) {
            idUser = getArguments().getString(ID_USER, "");
        }

        if (idUser == null || idUser.isEmpty()) {
            idUser = viewModel.getCurrentUserId();
        }

        // Si venimos de editar/crear abre el tab indicado
        String tabInicial = getArguments() != null
                ? getArguments().getString(TAB_INICIAL, "clanks")
                : "clanks";

        mostrandoClanks = !"bocetos".equals(tabInicial);

        configurarRecyclerView();
        configurarTabs();
        configurarBotones();
        observarViewModel();

        viewModel.cargarDatos(idUser);
        cargarAdapter(mostrandoClanks);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (viewModel.esPerfilPropio(idUser)) {
            ((NavbarHost) requireActivity()).ocultarNavbar();
        } else {
            ((NavbarHost) requireActivity()).mostrarNavbarConVolver("");
        }

        viewModel.invalidarDatos();
        viewModel.cargarDatos(idUser);
        cargarAdapter(mostrandoClanks);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (adapter != null) {
            adapter.cerrar();
        }

        binding = null;
    }

    ///////////////////////// recyclerView /////////////////////////

    private void configurarRecyclerView() {
        binding.rvClanks.setLayoutManager(
                new GridLayoutManager(requireContext(), 2)
        );

        binding.rvClanks.setHasFixedSize(false);

        int spacing = (int) getResources().getDimension(
                R.dimen.perfil_grid_spacing
        );

        binding.rvClanks.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect,
                                       @NonNull View view,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int pos = parent.getChildAdapterPosition(view);
                int col = pos % 2;

                outRect.left = col == 0 ? 0 : spacing / 2;
                outRect.right = col == 0 ? spacing / 2 : 0;
                outRect.bottom = spacing;
            }
        });
    }

    ///////////////////////// tabs clanks y borradores /////////////////////////

    private void configurarTabs() {
        if (!viewModel.esPerfilPropio(idUser)) {
            binding.tabBocetos.setVisibility(View.GONE);
            binding.tvNumBocetos.setVisibility(View.GONE);
        }

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

                        // Indicador en el tab correcto
                        View tabActivo = mostrandoClanks
                                ? binding.tabClanks
                                : binding.tabBocetos;

                        ajustarAnchoIndicador(tabActivo);
                        binding.indicadorTab.setTranslationX(tabActivo.getLeft());
                    }
                }
        );
    }

    private void animarIndicador(boolean haciaClanks) {
        View tabDestino = haciaClanks
                ? binding.tabClanks
                : binding.tabBocetos;

        float destino = tabDestino.getLeft();

        ValueAnimator anim = ValueAnimator.ofFloat(
                binding.indicadorTab.getTranslationX(),
                destino
        );

        anim.setDuration(200);

        anim.addUpdateListener(a ->
                binding.indicadorTab.setTranslationX(
                        (float) a.getAnimatedValue()
                )
        );

        anim.start();

        ajustarAnchoIndicador(tabDestino);
    }

    private void ajustarAnchoIndicador(View tab) {
        ViewGroup.LayoutParams lp = binding.indicadorTab.getLayoutParams();
        lp.width = tab.getWidth();
        binding.indicadorTab.setLayoutParams(lp);
    }

    ///////////////////////// adapter /////////////////////////

    private void cargarAdapter(boolean soloAcabados) {
        mostrarCargandoPerfil();

        if (adapter != null) {
            adapter.stopListening();
        }

        FirestoreRecyclerOptions<Clank> options = soloAcabados
                ? viewModel.buildClankOptionsAcabados(idUser)
                : viewModel.buildClankOptionsBocetos(idUser);

        adapter = new ClanksAdapter(
                options,
                requireContext(),
                viewModel.getClankRepository(),
                viewModel.getLikeRepository(),
                viewModel.getCurrentUserId(),
                viewModel.esPerfilPropio(idUser),
                clankId -> {
                    Bundle args = new Bundle();
                    args.putString("clankId", clankId);

                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_perfil_a_detalle_clank, args);
                },
                this::mostrarOpcionesClank,
                new ClanksAdapter.OnPreparacionTarjetasListener() {
                    @Override
                    public void alIniciarPreparacion() {
                        mostrarCargandoPerfil();
                    }

                    @Override
                    public void alFinalizarPreparacion() {
                        ocultarCargandoPerfil();
                    }
                }
        );

        if (!viewModel.esPerfilPropio(idUser)) {
            adapter.setLikeListener(clankId ->
                    viewModel.toggleLike(clankId)
            );

            adapter.setItemsListosListener((clankIds, numLikesIniciales) ->
                    arrancarObservadoresLikes(clankIds, numLikesIniciales)
            );
        }

        binding.rvClanks.setAdapter(adapter);
        adapter.startListening();
    }

    ///////////////////////// botones /////////////////////////

    private void configurarBotones() {
        if (viewModel.esPerfilPropio(idUser)) {
            binding.btnAjustes.setOnClickListener(v ->
                    Navigation.findNavController(v)
                            .navigate(R.id.action_perfil_a_ajustes)
            );

            binding.tvEditarPerfil.setVisibility(View.VISIBLE);

            binding.tvEditarPerfil.setOnClickListener(v ->
                    Navigation.findNavController(v)
                            .navigate(R.id.action_perfil_a_editar_perfil)
            );
        } else {
            binding.btnAjustes.setVisibility(View.GONE);
            binding.tvEditarPerfil.setVisibility(View.GONE);
        }
    }

    ///////////////////////// observadores /////////////////////////

    private void observarViewModel() {
        viewModel.getPerfil().observe(getViewLifecycleOwner(), perfil -> {
            if (perfil == null) {
                return;
            }

            binding.tvNombrePerfil.setText(
                    perfil.nombre != null && !perfil.nombre.isEmpty()
                            ? perfil.nombre
                            : ""
            );

            String handle = "";

            if (perfil.usuarioClank != null &&
                    !perfil.usuarioClank.trim().isEmpty()) {
                handle = perfil.usuarioClank.replace("@", "").trim();
            } else if (perfil.correo != null &&
                    !perfil.correo.trim().isEmpty()) {
                handle = perfil.correo.split("@")[0].trim();
            }

            if (!viewModel.esPerfilPropio(idUser)) {
                ((NavbarHost) requireActivity()).mostrarNavbarConVolver(
                        !handle.isEmpty()
                                ? "@" + handle
                                : ""
                );
            }

            binding.tvUidPerfil.setText(
                    !handle.isEmpty()
                            ? "@" + handle
                            : ""
            );

            if (perfil.fotoPerfil != null && !perfil.fotoPerfil.isEmpty()) {
                Glide.with(this)
                        .load(perfil.fotoPerfil)
                        .circleCrop()
                        .placeholder(R.drawable.img_usuario_defecto)
                        .into(binding.civFotoPerfil);
            }
        });

        viewModel.getNumClanks().observe(getViewLifecycleOwner(), num ->
                binding.tvNumClanks.setText(
                        String.valueOf(num != null ? num : 0)
                )
        );

        viewModel.getNumBocetos().observe(getViewLifecycleOwner(), num ->
                binding.tvNumBocetos.setText(
                        String.valueOf(num != null ? num : 0)
                )
        );
    }

    ///////////////////////// hoja de opciones de clank /////////////////////////

    private void mostrarOpcionesClank(String clankId, String tituloClank) {
        HojaOpciones hoja = HojaOpciones.nuevaLista(
                tituloClank,
                Arrays.asList(
                        new ItemOpcion(
                                "editar",
                                getString(R.string.perfil_opcion_editar_clank)
                        ),
                        new ItemOpcion(
                                "eliminar",
                                getString(R.string.perfil_opcion_eliminar_clank)
                        )
                ),
                opcionSeleccionada -> {
                    if ("editar".equals(opcionSeleccionada)) {
                        navegarAEditarClank(clankId);
                    } else if ("eliminar".equals(opcionSeleccionada)) {
                        requireView().post(() ->
                                mostrarConfirmarEliminarClank(
                                        clankId,
                                        tituloClank
                                )
                        );
                    }
                }
        );

        hoja.show(
                getParentFragmentManager(),
                "hoja_opciones_clank"
        );
    }

    private void navegarAEditarClank(String clankId) {
        Bundle args = new Bundle();
        args.putString("clankId", clankId);

        Navigation.findNavController(requireView())
                .navigate(R.id.action_perfil_a_editar_clank, args);
    }

    private void mostrarConfirmarEliminarClank(String clankId,
                                               String tituloClank) {
        HojaOpciones hoja = HojaOpciones.nuevaConfirmacion(
                getString(R.string.perfil_eliminar_clank_titulo),
                getString(R.string.perfil_eliminar_clank_mensaje, tituloClank),
                getString(R.string.cancelar),
                getString(R.string.perfil_eliminar_clank_confirmar),
                null,
                () -> eliminarClankDesdePerfil(clankId)
        );

        hoja.show(
                getParentFragmentManager(),
                "hoja_confirmar_eliminar_clank"
        );
    }

    private void eliminarClankDesdePerfil(String clankId) {
        viewModel.eliminarClank(clankId)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) {
                        return;
                    }
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) {
                        return;
                    }
                });
    }

    private void mostrarCargandoPerfil() {
        if (binding == null) {
            return;
        }

        binding.overlayCargandoPerfil.setVisibility(View.VISIBLE);
    }

    private void ocultarCargandoPerfil() {
        if (binding == null) {
            return;
        }

        binding.overlayCargandoPerfil.setVisibility(View.GONE);
    }

    private void arrancarObservadoresLikes(List<String> clankIds,
                                           List<Integer> numLikesIniciales) {
        for (int i = 0; i < clankIds.size(); i++) {
            String clankId = clankIds.get(i);
            int likesInicial = numLikesIniciales.get(i);

            viewModel.iniciarListenerLike(clankId, likesInicial);

            if (observadosLikes.contains(clankId)) {
                continue;
            }

            observadosLikes.add(clankId);

            observarEstadoLike(clankId);
            observarContadorLike(clankId);
        }
    }

    private void observarEstadoLike(String clankId) {
        viewModel.getEstadoLike(clankId)
                .observe(getViewLifecycleOwner(), isLiked -> {
                    if (isLiked == null || adapter == null) {
                        return;
                    }

                    Integer contador =
                            viewModel.getContadorLikes(clankId).getValue();

                    adapter.actualizarLike(
                            clankId,
                            isLiked,
                            contador != null ? contador : 0
                    );

                    int pos = encontrarPosicion(clankId);

                    if (pos >= 0) {
                        adapter.notifyItemChanged(
                                pos,
                                ClanksAdapter.PAYLOAD_LIKE
                        );
                    }
                });
    }

    private void observarContadorLike(String clankId) {
        viewModel.getContadorLikes(clankId)
                .observe(getViewLifecycleOwner(), contador -> {
                    if (contador == null || adapter == null) {
                        return;
                    }

                    Boolean isLiked =
                            viewModel.getEstadoLike(clankId).getValue();

                    adapter.actualizarLike(
                            clankId,
                            Boolean.TRUE.equals(isLiked),
                            contador
                    );

                    int pos = encontrarPosicion(clankId);

                    if (pos >= 0) {
                        adapter.notifyItemChanged(
                                pos,
                                ClanksAdapter.PAYLOAD_LIKE
                        );
                    }
                });
    }

    private int encontrarPosicion(String clankId) {
        if (adapter == null) {
            return -1;
        }

        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (clankId.equals(adapter.getSnapshots().getSnapshot(i).getId())) {
                return i;
            }
        }

        return -1;
    }
}