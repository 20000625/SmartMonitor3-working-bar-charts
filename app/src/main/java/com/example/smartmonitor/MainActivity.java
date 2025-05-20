package com.example.smartmonitor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.lifecycle.ViewModelProvider;

import com.example.smartmonitor.databinding.ActivityMainBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private EnvironmentalDataViewModel environmentalDataViewModel;

    private static final int UDP_PORT = 8888;
    private static final int TCP_PORT = 12345;
    private static final String UDP_MESSAGE = "SensorBoard";
    private static final String UDP_MESSAGEE = "ACK";
    private static final String UDP_RESPONSE = "SensorBoard";

    private String pairedDeviceIP = null;

    private TextView textViewReceivedData;
    private BarChart barChartTemperature;
    private BarChart barChartHumidity;
    private BarChart barChartCarbonMonoxide;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        EnvironmentalDataViewModel viewModel = new ViewModelProvider(this).get(EnvironmentalDataViewModel.class);

        binding.appBarMain.fab.setOnClickListener(view -> {
            Snackbar.make(view, "Starting communication", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(R.id.fab).show();
            discoverPairedDevice();
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Initialize views
        textViewReceivedData = findViewById(R.id.textViewReceivedData);
        barChartTemperature = findViewById(R.id.barChartTemperature);
        barChartHumidity = findViewById(R.id.barChartHumidity);
        barChartCarbonMonoxide = findViewById(R.id.barChartCarbonMonoxide);

        // Initialize the Auto buttons
        Button fanAutoButton = findViewById(R.id.buttonFanAuto);
        Button humidifierAutoButton = findViewById(R.id.buttonHumidifierAuto);
        Button radiatorAutoButton = findViewById(R.id.buttonRadiatorAuto);

        discoverPairedDevice();  // Automatically start the device discovery process when the activity starts

    }

    private void discoverPairedDevice() {
        executorService.execute(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket()) {
                udpSocket.setBroadcast(true);
                byte[] message = UDP_MESSAGE.getBytes();
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

                boolean responseReceived = false;

                while (!responseReceived) {
                    // Send UDP broadcast
                    try {
                        DatagramPacket packet = new DatagramPacket(message, message.length, broadcastAddress, UDP_PORT);
                        udpSocket.send(packet);
                        Log.d("UDP", "UDP message sent: " + UDP_MESSAGE);
                    } catch (Exception e) {
                        Log.e("UDP", "Error sending UDP broadcast", e);
                    }

                    // Listen for UDP response
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

                        udpSocket.setSoTimeout(2000); // Timeout for listening (2 seconds)
                        udpSocket.receive(responsePacket);

                        String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                        String responderIP = responsePacket.getAddress().getHostAddress();

                        if (UDP_RESPONSE.equals(response)) {
                            pairedDeviceIP = responderIP;  // Set the IP once a response is received
                            responseReceived = true;
                            Log.d("UDP", "Paired with device: " + pairedDeviceIP);

                            // Update UI with the paired device (optional)
                            handler.post(() -> Snackbar.make(findViewById(R.id.nav_host_fragment_content_main),
                                    "Paired with device: " + pairedDeviceIP, Snackbar.LENGTH_LONG).show());

                            // Automatically request environmental data once paired
                            requestEnvironmentalData();  // Call requestEnvironmentalData automatically

                            break;
                        } else {
                            Log.d("UDP", "Unknown response from " + responderIP + ": " + response);
                        }
                    } catch (Exception e) {
                        Log.d("UDP", "No response received, retrying...", e);
                    }

                    // Wait before sending the next broadcast
                    try {
                        Thread.sleep(3000); // Send a broadcast every 3 seconds
                    } catch (InterruptedException e) {
                        Log.e("UDP", "Broadcast loop interrupted", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Send acknowledgment to the paired device
                byte[] message2 = UDP_MESSAGEE.getBytes();
                DatagramPacket packet = new DatagramPacket(message2, message2.length, broadcastAddress, UDP_PORT);
                udpSocket.send(packet);

            } catch (Exception e) {
                Log.e("UDP", "Error during UDP communication", e);
                handler.post(() -> Snackbar.make(findViewById(R.id.nav_host_fragment_content_main),
                        "Failed to discover devices.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    public void requestEnvironmentalData() {
        if (pairedDeviceIP == null) {
            Log.e("TCP", "Paired device IP is null. Cannot request data.");
            handler.post(() -> Snackbar.make(findViewById(R.id.nav_host_fragment_content_main),
                    "Device not paired. Cannot request data.", Snackbar.LENGTH_LONG).show());
            return;
        }

        executorService.execute(() -> {
            try (Socket socket = new Socket(pairedDeviceIP, TCP_PORT)) {
                Log.d("TCP", "Connected to server at " + pairedDeviceIP + ":" + TCP_PORT);

                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Send the "ALL" command
                    out.println("ALL");
                    Log.d("TCP", "Sent command: ALL");

                    // Read the server response
                    String response = in.readLine();
                    Log.d("TCP", "Raw Response: " + response);

                    // Validate and parse the response
                    if (response != null && response.matches("\\d{2}\\.\\d:\\d{2}\\.\\d:\\d{2}\\.\\d")) {
                        String[] parts = response.split(":");
                        Log.d("TCP", "Parsed Response: Temperature=" + parts[0] + ", Humidity=" + parts[1] + ", CO=" + parts[2]);

                        // Update the ViewModel with parsed data
                        environmentalDataViewModel.setTemperature(Double.parseDouble(parts[0]));
                        environmentalDataViewModel.setHumidity(Double.parseDouble(parts[1]));
                        environmentalDataViewModel.setCarbonMonoxide(Double.parseDouble(parts[2]));
                        Log.d("TCP", "Environmental data updated successfully.");
                    } else {
                        Log.e("TCP", "Invalid response format. Expected 'XX.X:XX.X:XX.X', but got: " + response);
                    }
                }
            } catch (Exception e) {
                Log.e("TCP", "Error during communication with the server", e);
                handler.post(() -> Snackbar.make(findViewById(R.id.nav_host_fragment_content_main),
                        "Failed to request environmental data.", Snackbar.LENGTH_LONG).show());
            }
        });
    }


    public void sendDehumidifierControlCommand(String command) {
        executorService.execute(() -> {
            try (Socket socket = new Socket(pairedDeviceIP, TCP_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Send the dehumidifier control command
                out.println(command);

                // Optionally read acknowledgment from the server
                String response = in.readLine();
                Log.d("TCP", "Dehumidifier Control Response: " + response);

                runOnUiThread(() -> {
                    String message;
                    switch (command) {
                        case "HUMIDIFIER:ON":
                            message = "Dehumidifier turned on";
                            break;
                        case "HUMIDIFIER:OFF":
                            message = "Dehumidifier turned off";
                            break;
                        case "HUMIDIFIER:AUTO":
                            message = "Humidifier Auto Mode";
                            break;
                        default:
                            message = "Humidifier Auto Mode";
                    }
                    Snackbar.make(findViewById(R.id.nav_host_fragment_content_main), message, Snackbar.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e("TCP", "Error sending dehumidifier control command", e);
                runOnUiThread(() -> Snackbar.make(findViewById(R.id.nav_host_fragment_content_main), "Failed to control dehumidifier.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    public void sendRadiatorControlCommand(String command) {
        executorService.execute(() -> {
            try (Socket socket = new Socket(pairedDeviceIP, TCP_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Send the radiator control command
                out.println(command);

                // Optionally read acknowledgment from the server
                String response = in.readLine();
                Log.d("TCP", "Radiator Control Response: " + response);

                runOnUiThread(() -> {
                    String message;
                    switch (command) {
                        case "RADIATOR:ON":
                            message = "Radiator turned on";
                            break;
                        case "RADIATOR:OFF":
                            message = "Radiator turned off";
                            break;
                        case "RADIATOR:AUTO":
                            message = "Radiator Auto Mode";
                            break;
                        default:
                            message = "Radiator Auto Mode";
                    }
                    Snackbar.make(findViewById(R.id.nav_host_fragment_content_main), message, Snackbar.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e("TCP", "Error sending radiator control command", e);
                runOnUiThread(() -> Snackbar.make(findViewById(R.id.nav_host_fragment_content_main), "Failed to control radiator.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    public void sendExtractorFanControlCommand(String command) {
        executorService.execute(() -> {
            try (Socket socket = new Socket(pairedDeviceIP, TCP_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Send the extractor fan control command
                out.println(command);

                // Optionally read acknowledgment from the server
                String response = in.readLine();
                Log.d("TCP", "Extractor Fan Control Response: " + response);

                runOnUiThread(() -> {
                    String message;
                    switch (command) {
                        case "FAN:ON":
                            message = "Fan turned on";
                            break;
                        case "FAN:OFF":
                            message = "Fan turned off";
                            break;
                        case "FAN:AUTO":
                            message = "Fan Auto Mode";
                            break;
                        default:
                            message = "Fan Auto Mode";
                    }
                    Snackbar.make(findViewById(R.id.nav_host_fragment_content_main), message, Snackbar.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e("TCP", "Error sending extractor fan control command", e);
                runOnUiThread(() -> Snackbar.make(findViewById(R.id.nav_host_fragment_content_main), "Failed to control extractor fan.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}