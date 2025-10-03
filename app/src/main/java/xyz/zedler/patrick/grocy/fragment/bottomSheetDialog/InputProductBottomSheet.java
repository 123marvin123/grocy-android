/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetInputProductBinding;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.OpenBeautyFactsProduct;
import xyz.zedler.patrick.grocy.model.OpenFoodFactsProduct;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class InputProductBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = InputProductBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetInputProductBinding binding;

  private MutableLiveData<Integer> selectionLive;
  private String productNameFromOnlineSource;
  private String productImageUrlFromOnlineSource;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetInputProductBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    setSkipCollapsedInPortrait();
    setCancelable(false);
    super.onViewCreated(view, savedInstanceState);
    activity = (MainActivity) requireActivity();
    binding.setBottomsheet(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    String input = requireArguments().getString(Constants.ARGUMENT.PRODUCT_INPUT);
    assert input != null;
    binding.input.setText(input);

    boolean stringOnlyContainsNumbers = true;
    for (char c : input.trim().toCharArray()) {
      try {
        Integer.parseInt(String.valueOf(c));
      } catch (NumberFormatException e) {
        stringOnlyContainsNumbers = false;
        break;
      }
    }
    selectionLive = new MutableLiveData<>(stringOnlyContainsNumbers ? 3 : 1);

    // Query OpenFoodFacts/OpenBeautyFacts if input is a barcode
    if (stringOnlyContainsNumbers) {
      queryProductNameFromOnlineSources(input.trim());
    }
  }

  private void queryProductNameFromOnlineSources(String barcode) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
        requireContext()
    );
    boolean isOpenFoodFactsEnabled = sharedPrefs.getBoolean(
        SETTINGS.BEHAVIOR.FOOD_FACTS,
        SETTINGS_DEFAULT.BEHAVIOR.FOOD_FACTS
    );

    if (!isOpenFoodFactsEnabled) {
      return;
    }

    DownloadHelper dlHelper = new DownloadHelper(activity, TAG);
    OpenFoodFactsProduct.getOpenFoodFactsProduct(
        dlHelper,
        barcode,
        product -> {
          String productName = product.getLocalizedProductName(activity.getApplication());
          if (productName != null && !productName.isEmpty()) {
            productNameFromOnlineSource = productName;
          }
          String imageUrl = product.getImageUrl();
          if (imageUrl != null && !imageUrl.isEmpty()) {
            productImageUrlFromOnlineSource = imageUrl;
          }
        },
        error -> OpenBeautyFactsProduct.getOpenBeautyFactsProduct(
            dlHelper,
            barcode,
            product -> {
              String productName = product.getLocalizedProductName(activity.getApplication());
              if (productName != null && !productName.isEmpty()) {
                productNameFromOnlineSource = productName;
              }
              String imageUrl = product.getImageUrl();
              if (imageUrl != null && !imageUrl.isEmpty()) {
                productImageUrlFromOnlineSource = imageUrl;
              }
            },
            error1 -> {
              // No product name found from online sources, continue without it
            }
        )
    );
  }

  public void proceed() {
    assert selectionLive.getValue() != null;
    String input = binding.input.getText().toString();

    // Add image URL if available and setting is enabled
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
    boolean isImageAutofillEnabled = sharedPrefs.getBoolean(
            SETTINGS.BEHAVIOR.FOOD_FACTS_AUTOFILL_IMAGES,
            SETTINGS_DEFAULT.BEHAVIOR.FOOD_FACTS_AUTOFILL_IMAGES
    );

    MasterProductFragmentArgs.Builder argsBuilder =
            new MasterProductFragmentArgs.Builder(Constants.ACTION.CREATE)
            .setProductName(input);

    if (isImageAutofillEnabled && productImageUrlFromOnlineSource != null) {
      argsBuilder.setPictureUrl(productImageUrlFromOnlineSource);
    }

    if (selectionLive.getValue() == 1) {
      activity.navUtil.navigateDeepLink(R.string.deep_link_masterProductFragment,
          argsBuilder.build().toBundle());
    } else if (selectionLive.getValue() == 2) {
      // Use product name from online source if available, otherwise use empty
      String productName = productNameFromOnlineSource != null ? productNameFromOnlineSource : "";

        argsBuilder.setProductName(productName);
      
      activity.navUtil.navigateDeepLink(
              R.string.deep_link_masterProductFragment,
              argsBuilder.build().toBundle());
    } else {
      activity.getCurrentFragment().addBarcodeToExistingProduct(input.trim());
    }
    dismiss();
  }

  public MutableLiveData<Integer> getSelectionLive() {
    return selectionLive;
  }

  public void setSelectionLive(int selection) {
    selectionLive.setValue(selection);
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.linearContainerScroll.setPadding(
        binding.linearContainerScroll.getPaddingLeft(),
        binding.linearContainerScroll.getPaddingTop(),
        binding.linearContainerScroll.getPaddingRight(),
        UiUtil.dpToPx(activity, 12) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
