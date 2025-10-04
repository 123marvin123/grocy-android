package xyz.zedler.patrick.grocy.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatBehaviorBinding;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatGeminiBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatGeminiFragment extends BaseFragment {

    private final static String TAG = SettingsCatGeminiFragment.class.getSimpleName();

    private FragmentSettingsCatGeminiBinding binding;
    private MainActivity activity;
    private SettingsViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        binding = FragmentSettingsCatGeminiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        binding.setActivity(activity);
        binding.setFragment(this);
        binding.setViewModel(viewModel);
        binding.setSharedPrefs(PreferenceManager.getDefaultSharedPreferences(activity));
        binding.setClickUtil(new ClickUtil());
        binding.setLifecycleOwner(getViewLifecycleOwner());

        SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
        systemBarBehavior.setAppBar(binding.appBar);
        systemBarBehavior.setScroll(binding.scroll, binding.constraint);
        systemBarBehavior.setUp();
        activity.setSystemBarBehavior(systemBarBehavior);

        binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

        viewModel.getEventHandler().observe(getViewLifecycleOwner(), event -> {
            if (event.getType() == Event.SNACKBAR_MESSAGE) {
                activity.showSnackbar(
                        ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
                );
            } else if (event.getType() == Event.BOTTOM_SHEET) {
                BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
                activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
            } else if (event.getType() == Event.UPDATE_BOTTOM_APP_BAR) {
                activity.updateBottomNavigationMenuButton();
                activity.getScrollBehavior().setBottomBarVisibility(
                        activity.hasBottomNavigationIcon(), !activity.hasBottomNavigationIcon()
                );
            }
        });

        viewModel.getEventHandler().observe(getViewLifecycleOwner(), event -> {
            if (event.getType() == Event.SNACKBAR_MESSAGE) {
                activity.showSnackbar(
                        ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
                );
            } else if (event.getType() == Event.BOTTOM_SHEET) {
                BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
                activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
            }
        });

        activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
        activity.getScrollBehavior().setUpScroll(
                binding.appBar, false, binding.scroll, false
        );
        activity.getScrollBehavior().setBottomBarVisibility(
                activity.hasBottomNavigationIcon(), !activity.hasBottomNavigationIcon()
        );
        activity.updateBottomAppBar(false, R.menu.menu_empty);

        setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);

    }

    @Override
    public void saveInput(String text, Bundle argsBundle) {
        String type = argsBundle.getString(Constants.ARGUMENT.TYPE);
        if (type == null) return;

        if (type.equals(Constants.SETTINGS.GEMINI.API_KEY)) {
            viewModel.setGeminiApiKey(text);
            binding.textGeminiApiKey.setText(viewModel.getGeminiApiKey());
        }
    }

}
