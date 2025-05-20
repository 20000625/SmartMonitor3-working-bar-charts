package com.example.smartmonitor.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.smartmonitor.EnvironmentalDataViewModel;

import com.example.smartmonitor.MainActivity;
import com.example.smartmonitor.databinding.FragmentSlideshowBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import android.R;


import java.util.ArrayList;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private BarChart barChartCarbonMonoxide;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize BarCharts
        barChartCarbonMonoxide = binding.barChartCarbonMonoxide;

        // Obtain ViewModel instance
        EnvironmentalDataViewModel viewModel = new ViewModelProvider(requireActivity()).get(EnvironmentalDataViewModel.class);

        // Observe carbon monoxide data and update the respective chart
        viewModel.getCarbonMonoxide().observe(getViewLifecycleOwner(), carbonMonoxide -> {
            if (carbonMonoxide != null) {
                Log.d("SlideshowFragment", "Observed Carbon Monoxide: " + carbonMonoxide);
                updateBarChart(barChartCarbonMonoxide, carbonMonoxide);
            } else {
                Log.d("SlideshowFragment", "No carbon monoxide data available.");
            }
        });

        // Handle Turn On button click
        binding.buttonTurnOnExtractorFan.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendExtractorFanControlCommand("FAN:ON");
        });

        // Handle Turn Off button click
        binding.buttonTurnOffExtractorFan.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendExtractorFanControlCommand("FAN:OFF");
        });

        // Handle Auto button click
        binding.buttonFanAuto.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendRadiatorControlCommand("FAN:AUTO");
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop periodic updates to prevent memory leaks
        binding = null;
    }

    private void updateBarChart(BarChart barChart, double value) {
        try {
            Log.d("BarChart", "Updating chart with value: " + value);

            // Create a new dataset for the bar chart
            ArrayList<BarEntry> entries = new ArrayList<>();
            entries.add(new BarEntry(0, (float) value)); // Add a single entry with the value

            BarDataSet dataSet = new BarDataSet(entries, "Carbon Monoxide");
            dataSet.setColor(requireContext().getColor(android.R.color.holo_orange_dark)); // Set a color for the bars

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.9f); // Set bar width

            barChart.setData(barData);
            barChart.invalidate(); // Refresh the chart
        } catch (Exception e) {
            Log.e("BarChart", "Error updating chart", e);
        }
    }
}
