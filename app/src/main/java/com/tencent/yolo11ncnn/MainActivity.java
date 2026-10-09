// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.tencent.yolo11ncnn;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class MainActivity extends Activity implements SurfaceHolder.Callback
{
    public static final int REQUEST_CAMERA = 100;

    private YOLO11Ncnn yolo11ncnn = new YOLO11Ncnn();
    private int facing = 0;

    private Spinner spinnerTask;
    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_task = 0;
    private int current_model = 0;
    private int current_cpugpu = 0;

    private SurfaceView cameraView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cameraView = (SurfaceView) findViewById(R.id.cameraview);

        cameraView.setZOrderMediaOverlay(true);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        TextView buttonSwitchCamera = (TextView) findViewById(R.id.buttonSwitchCamera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                int new_facing = 1 - facing;

                yolo11ncnn.closeCamera();

                yolo11ncnn.openCamera(new_facing);

                facing = new_facing;
            }
        });

        spinnerTask = (Spinner) findViewById(R.id.spinnerTask);
        spinnerTask.setAdapter(createSpinnerAdapter(R.array.task_array));
        spinnerTask.setBackgroundResource(0);
        spinnerTask.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_task)
                {
                    current_task = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        spinnerModel = (Spinner) findViewById(R.id.spinnerModel);
        spinnerModel.setAdapter(createSpinnerAdapter(R.array.model_array));
        spinnerModel.setBackgroundResource(0);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_model)
                {
                    current_model = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        spinnerCPUGPU = (Spinner) findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setAdapter(createSpinnerAdapter(R.array.cpugpu_array));
        spinnerCPUGPU.setBackgroundResource(0);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_cpugpu)
                {
                    current_cpugpu = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        reload();
    }

    private ArrayAdapter<String> createSpinnerAdapter(int arrayResource)
    {
        String[] values = getResources().getStringArray(arrayResource);
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                TextView textView = convertView instanceof TextView
                        ? (TextView) convertView
                        : new TextView(MainActivity.this);
                textView.setText(getItem(position));
                textView.setTextColor(getColor(R.color.text_primary));
                textView.setTextSize(13);
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setPadding(0, 0, dp(2), 0);

                Drawable arrow = getDrawable(R.drawable.ic_expand_more);
                if (arrow != null)
                {
                    arrow.setTint(getColor(R.color.text_secondary));
                    textView.setCompoundDrawablesWithIntrinsicBounds(null, null, arrow, null);
                    textView.setCompoundDrawablePadding(dp(5));
                }
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent)
            {
                TextView textView = convertView instanceof TextView
                        ? (TextView) convertView
                        : new TextView(MainActivity.this);
                textView.setText(getItem(position));
                textView.setTextColor(getColor(R.color.text_primary));
                textView.setTextSize(13);
                textView.setSingleLine(true);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setPadding(dp(14), dp(12), dp(14), dp(12));
                textView.setBackgroundColor(getColor(R.color.panel_background));
                return textView;
            }
        };
    }

    private int dp(float value)
    {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void reload()
    {
        boolean ret_init = yolo11ncnn.loadModel(getAssets(), current_task, current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolo11ncnn loadModel failed");
        }
        TextView engineStatus = (TextView) findViewById(R.id.textEngineStatus);
        engineStatus.setText(ret_init ? "识别引擎已就绪" : "模型加载失败，请更换配置");
        View statusDot = findViewById(R.id.statusDot);
        statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(ret_init ? R.color.accent : R.color.error)));
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        yolo11ncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        yolo11ncnn.openCamera(facing);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        yolo11ncnn.closeCamera();
    }
}
