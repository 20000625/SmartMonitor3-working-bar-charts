package com.example.smartmonitor.ui.home;

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
import com.example.smartmonitor.databinding.FragmentHomeBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import android.R;

import java.util.ArrayList;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private BarChart barChartTemperature;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize the BarChart
        barChartTemperature = binding.barChartTemperature;

        // Obtain ViewModel instance
        EnvironmentalDataViewModel viewModel = new ViewModelProvider(requireActivity()).get(EnvironmentalDataViewModel.class);

        // Observe temperature data and update the respective chart
        viewModel.getTemperature().observe(getViewLifecycleOwner(), temperature -> {
            if (temperature != null) {
                Log.d("HomeFragment", "Observed Temperature: " + temperature);
                updateBarChart(barChartTemperature, temperature);
            } else {
                Log.d("HomeFragment", "No temperature data available.");
            }
        });


        // Handle Turn On button click
        binding.buttonTurnOnRadiator.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendRadiatorControlCommand("RADIATOR:ON");
        });

        // Handle Turn Off button click
        binding.buttonTurnOffRadiator.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendRadiatorControlCommand("RADIATOR:OFF");
        });

        // Handle Auto button click
        binding.buttonRadiatorAuto.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendRadiatorControlCommand("RADIATOR:AUTO");
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateBarChart(BarChart barChart, double value) {
        try {
            Log.d("BarChart", "Updating chart with value: " + value);

            ArrayList<BarEntry> entries = new ArrayList<>();
            entries.add(new BarEntry(0, (float) value)); // Add a single entry with the value

            BarDataSet dataSet = new BarDataSet(entries, "Temperature");
            dataSet.setColor(requireContext().getColor(android.R.color.holo_blue_light)); // Use a system color

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.9f); // Set bar width

            barChart.setData(barData);
            barChart.invalidate(); // Refresh the chart
        } catch (Exception e) {
            Log.e("BarChart", "Error updating chart", e);
        }
    }

}
