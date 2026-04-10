package com.aiimagegen.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText etPrompt, etApiKey;
    private Spinner spMode, spModel, spSize;
    private Button btnGenerate;
    private TextView tvStatus;
    private ImageView ivPreview;
    private ProgressBar progressBar;

    // Mode: 0 = Pollinations (free), 1 = ModelScope (API key)
    private int currentMode = 0;

    private String[] polModels = {"flux", "turbo"};
    private String[] polModelNames = {"flux (Recommended)", "turbo"};
    private String[] mscopeModels = {
            "Tongyi-MAI/Z-Image-Turbo",
            "Tongyi-MAI/Z-Image",
            "Qwen/Qwen-Image-2512",
            "Qwen/Qwen-Image",
            "atonyxu/Illustrious-XL",
            "MusePublic/majicMIX_realistic_maijuxieshi_SD_1_5"
    };
    private String[] mscopeNames = {
            "Z-Image-Turbo (Recommended)", "Z-Image", "Qwen-Image-2512",
            "Qwen-Image", "Illustrious-XL", "majicMIX realistic"
    };
    private String[] sizes = {"1024x1024", "1024x2048", "2048x1024", "768x1344", "1344x768"};
    private String[] sizeLabels = {"1024x1024", "1024x2048 (V)", "2048x1024 (H)", "768x1344 (V)", "1344x768 (H)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPrompt = findViewById(R.id.etPrompt);
        etApiKey = findViewById(R.id.etApiKey);
        spMode = findViewById(R.id.spMode);
        spModel = findViewById(R.id.spModel);
        spSize = findViewById(R.id.spSize);
        btnGenerate = findViewById(R.id.btnGenerate);
        tvStatus = findViewById(R.id.tvStatus);
        ivPreview = findViewById(R.id.ivPreview);
        progressBar = findViewById(R.id.progressBar);

        setupModeSpinner();
        setupSizeSpinner();
        spMode.setSelection(0);
        updateModelSpinner(0);
        updateApiKeyVisibility(0);

        spMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                currentMode = pos;
                updateModelSpinner(pos);
                updateApiKeyVisibility(pos);
                if (pos == 0) {
                    tvStatus.setText("Ready - [Pollinations] Free mode, no API key needed");
                } else {
                    tvStatus.setText("Ready - [ModelScope] Enter your Access Token first");
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startGeneration(); }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void setupModeSpinner() {
        String[] modes = {"[Free] Pollinations AI  (No API Key)", "[API] ModelScope  (Access Token)"};
        spMode.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, modes));
    }

    private void setupSizeSpinner() {
        spSize.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, sizeLabels));
    }

    private void updateModelSpinner(int mode) {
        String[] names = (mode == 0) ? polModelNames : mscopeNames;
        spModel.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, names));
    }

    private void updateApiKeyVisibility(int mode) {
        if (mode == 0) {
            etApiKey.setVisibility(View.GONE);
            findViewById(R.id.tvApiKeyLabel).setVisibility(View.GONE);
            findViewById(R.id.tvApiKeyHint).setVisibility(View.GONE);
        } else {
            etApiKey.setVisibility(View.VISIBLE);
            findViewById(R.id.tvApiKeyLabel).setVisibility(View.VISIBLE);
            findViewById(R.id.tvApiKeyHint).setVisibility(View.VISIBLE);
        }
    }

    private void startGeneration() {
        String prompt = etPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            toast("Please enter a prompt");
            return;
        }
        if (currentMode == 1) {
            String key = etApiKey.getText().toString().trim();
            if (key.isEmpty()) {
                toast("ModelScope mode requires Access Token");
                return;
            }
        }
        new GenerateTask(prompt).execute();
    }

    private class GenerateTask extends AsyncTask<Void, String, Bitmap> {
        private String prompt;

        GenerateTask(String prompt) { this.prompt = prompt; }

        @Override
        protected void onPreExecute() {
            btnGenerate.setEnabled(false);
            btnGenerate.setText("Generating...");
            progressBar.setVisibility(View.VISIBLE);
            ivPreview.setImageBitmap(null);
            tvStatus.setText(currentMode == 0
                    ? "Generating with Pollinations AI..."
                    : "Submitting to ModelScope...");
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            if (currentMode == 0) return generatePollinations(prompt);
            else return generateModelScope(prompt);
        }

        private Bitmap generatePollinations(String p) {
            try {
                int modelIdx = spModel.getSelectedItemPosition();
                String model = polModels[modelIdx >= 0 ? modelIdx : 0];
                int sizeIdx = spSize.getSelectedItemPosition();
                String sizeStr = sizes[sizeIdx >= 0 ? sizeIdx : 0];
                String[] wh = sizeStr.split("x");
                int w = Integer.parseInt(wh[0]);
                int h = Integer.parseInt(wh[1]);

                String encoded = URLEncoder.encode(p, "UTF-8");
                String urlStr = "https://image.pollinations.ai/prompt/" + encoded
                        + "?width=" + w + "&height=" + h + "&model=" + model + "&nologo=1";

                publishProgress("Downloading from Pollinations...");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(60000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();

                int code = conn.getResponseCode();
                if (code != 200) {
                    publishProgress("HTTP " + code);
                    return null;
                }

                InputStream is = conn.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                conn.disconnect();
                return bmp;
            } catch (Exception e) {
                publishProgress("Error: " + e.getMessage());
                return null;
            }
        }

        private Bitmap generateModelScope(String p) {
            try {
                String apiKey = etApiKey.getText().toString().trim();
                int modelIdx = spModel.getSelectedItemPosition();
                String model = mscopeModels[modelIdx >= 0 ? modelIdx : 0];

                publishProgress("Submitting task to ModelScope...");
                URL url = new URL("https://api-inference.modelscope.cn/v1/images/generations");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                String json = "{\"model\":\"" + model + "\",\"prompt\":\"" + p + "\"}";
                conn.getOutputStream().write(json.getBytes("UTF-8"));

                int code = conn.getResponseCode();
                if (code != 200) {
                    publishProgress("Submit failed: HTTP " + code);
                    return null;
                }

                InputStream is = conn.getInputStream();
                byte[] body = new byte[4096];
                int len = is.read(body);
                is.close();
                conn.disconnect();
                String resp = new String(body, 0, len, "UTF-8");

                int q1 = resp.indexOf("\"task_id\"");
                if (q1 < 0) { publishProgress("No task_id in response"); return null; }
                int a = resp.indexOf('"', q1 + 9);
                int b = resp.indexOf('"', a + 1);
                String taskId = resp.substring(a + 1, b);

                publishProgress("Task: " + taskId.substring(0, Math.min(8, taskId.length())) + " - polling...");
                return pollModelScope(taskId, apiKey);
            } catch (Exception e) {
                publishProgress("Error: " + e.getMessage());
                return null;
            }
        }

        private Bitmap pollModelScope(String taskId, String apiKey) {
            for (int i = 0; i < 60; i++) {
                try {
                    Thread.sleep(5000);
                    publishProgress("Polling... (" + (i * 5) + "s)");

                    URL url = new URL("https://api-inference.modelscope.cn/v1/tasks/" + taskId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(30000);

                    InputStream is = conn.getInputStream();
                    byte[] buf = new byte[8192];
                    int len = is.read(buf);
                    is.close();
                    conn.disconnect();
                    String body = new String(buf, 0, len, "UTF-8");

                    if (body.contains("\"SUCCEED\"")) {
                        int q1 = body.indexOf("\"output_images\"");
                        if (q1 >= 0) {
                            int a = body.indexOf('"', q1 + 17);
                            int b = body.indexOf('"', a + 1);
                            String imgUrl = body.substring(a + 1, b);

                            publishProgress("Downloading image...");
                            URL imgU = new URL(imgUrl);
                            HttpURLConnection imgC = (HttpURLConnection) imgU.openConnection();
                            imgC.setRequestProperty("User-Agent", "Mozilla/5.0");
                            imgC.setConnectTimeout(30000);
                            imgC.setReadTimeout(30000);
                            InputStream iis = imgC.getInputStream();
                            Bitmap bmp = BitmapFactory.decodeStream(iis);
                            iis.close();
                            imgC.disconnect();
                            return bmp;
                        }
                    }
                    if (body.contains("\"FAILED\"")) {
                        publishProgress("Task failed on ModelScope");
                        return null;
                    }
                } catch (Exception e) {
                    // continue polling
                }
            }
            publishProgress("Timeout waiting for result");
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            tvStatus.setText(values[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            btnGenerate.setEnabled(true);
            btnGenerate.setText("GENERATE");
            progressBar.setVisibility(View.GONE);

            if (bitmap == null) {
                tvStatus.setText("Generation failed. Check your network or API key.");
                toast("Generation failed");
                return;
            }

            ivPreview.setImageBitmap(bitmap);
            saveToGallery(bitmap);
        }
    }

    private void saveToGallery(Bitmap bmp) {
        try {
            String prefix = currentMode == 0 ? "PL" : "MS";
            String name = "AI_" + prefix + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, name);
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            toast("Saved: " + name);
            tvStatus.setText("SUCCESS: " + name + " saved to Pictures");
        } catch (Exception e) {
            toast("Save failed: " + e.getMessage());
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
