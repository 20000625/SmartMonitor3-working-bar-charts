package com.example.smartmonitor.ui.gallery;

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
import com.example.smartmonitor.databinding.FragmentGalleryBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import android.R;

import java.util.ArrayList;


public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private BarChart barChartHumidity;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize BarCharts
        barChartHumidity = binding.barChartHumidity;

        // Obtain ViewModel instance
        EnvironmentalDataViewModel viewModel = new ViewModelProvider(requireActivity()).get(EnvironmentalDataViewModel.class);

        // Observe humidity data and update the respective chart
        viewModel.getHumidity().observe(getViewLifecycleOwner(), humidity -> {
            if (humidity != null) {
                Log.d("GalleryFragment", "Observed Humidity: " + humidity);
                updateBarChart(barChartHumidity, humidity);
            } else {
                Log.d("GalleryFragment", "No humidity data available.");
            }
        });

        // Handle Turn On button click
        binding.buttonTurnOnDehumidifier.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendDehumidifierControlCommand("HUMIDIFIER:ON");
        });

        // Handle Turn Off button click
        binding.buttonTurnOffDehumidifier.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendDehumidifierControlCommand("HUMIDIFIER:OFF");
        });

        // Handle Auto button click
        binding.buttonHumidifierAuto.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendRadiatorControlCommand("HUMIDIFIER:AUTO");
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
            entries.add(new BarEntry(0, (float) value));

            BarDataSet dataSet = new BarDataSet(entries, "Humidity");
            dataSet.setColor(requireContext().getColor(android.R.color.holo_red_dark));

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.9f);

            barChart.setData(barData);
            barChart.invalidate();
        } catch (Exception e) {
            Log.e("BarChart", "Error updating chart", e);
        }
    }

}
