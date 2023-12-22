package com.zhymabekroman.keyboardhelper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    String packageName = getPackageName();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
      }
    }

    setContentView(R.layout.activity_main);

    Spinner portraitSpinner = findViewById(R.id.portraitKeyboardSpinner);
    Spinner landscapeSpinner = findViewById(R.id.landscapeKeyboardSpinner);

    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    List<InputMethodInfo> methodList = imm.getEnabledInputMethodList();
    HashMap<String, String> keyboardMap = new HashMap<>();

    int i = 0;
    for (InputMethodInfo method : methodList) {
      String label = method.loadLabel(getPackageManager()).toString();
      String inputMethodClass = method.getId();
      keyboardMap.put(label, inputMethodClass);
    }

    String[] keyboardList = keyboardMap.keySet().toArray(new String[0]);

    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, keyboardList);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    portraitSpinner.setAdapter(adapter);
    landscapeSpinner.setAdapter(adapter);

    portraitSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selectedKeyboardLabel = parent.getItemAtPosition(position).toString();
            String selectedKeyboardPackage = keyboardMap.get(selectedKeyboardLabel);
            SharedPreferences sharedPref =
                getSharedPreferences("appSettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("portraitKeyboard", selectedKeyboardPackage);
            editor.apply();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    landscapeSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selectedKeyboardLabel = parent.getItemAtPosition(position).toString();
            String selectedKeyboardPackage = keyboardMap.get(selectedKeyboardLabel);
            SharedPreferences sharedPref =
                getSharedPreferences("appSettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("landscapeKeyboard", selectedKeyboardPackage);
            editor.apply();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    Button startServiceButton = findViewById(R.id.startServiceButton);
    startServiceButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (MainActivity.this.checkCallingOrSelfPermission(
                    android.Manifest.permission.WRITE_SECURE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
              Toast.makeText(
                      MainActivity.this,
                      "WRITE_SECURE_SETTINGS permission not granted",
                      Toast.LENGTH_SHORT)
                  .show();
            } else {
              Intent intent = new Intent(MainActivity.this, KeyboardService.class);
              stopService(intent);
              startService(intent);
            }
          }
        });

    Button stopServiceButton = findViewById(R.id.stopServiceButton);
    stopServiceButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, KeyboardService.class);
            stopService(intent);
          }
        });
  }
}