package com.zhymabekroman.keyboardhelper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

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

        Intent intent = new Intent(this, KeyboardService.class);
        startService(intent);

        setContentView(R.layout.activity_main);

        String currentInputMethod = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> methodList = imm.getEnabledInputMethodList();
        String keyboardList = currentInputMethod;
        keyboardList += "\n";

        for(InputMethodInfo method : methodList){
            // String id = method.getId(); // The unique ID for this input method
            // String packageName = method.getPackageName(); // The package that contains this input method
            String serviceClassName = method.getServiceName(); // The fully qualified class name of the service that implements this input method
            keyboardList += serviceClassName;
            keyboardList += "\n";
        }
        TextView myTextView = (TextView) findViewById(R.id.keyboardsList1);
        myTextView.setText(keyboardList);

    }
}