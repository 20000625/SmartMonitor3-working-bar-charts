package com.example.smartmonitor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EnvironmentalDataViewModel extends ViewModel {
    private final MutableLiveData<Double> temperature = new MutableLiveData<>();
    private final MutableLiveData<Double> humidity = new MutableLiveData<>();
    private final MutableLiveData<Double> carbonMonoxide = new MutableLiveData<>();

    public LiveData<Double> getTemperature() {
        return temperature;
    }

    public LiveData<Double> getHumidity() {
        return humidity;
    }

    public LiveData<Double> getCarbonMonoxide() {
        return carbonMonoxide;
    }

    public void setTemperature(double value) {
        temperature.setValue(value);
    }

    public void setHumidity(double value) {
        humidity.setValue(value);
    }

    public void setCarbonMonoxide(double value) {
        carbonMonoxide.setValue(value);
    }
}
