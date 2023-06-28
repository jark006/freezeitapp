package io.github.jark006.freezeit.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;

import io.github.jark006.freezeit.R;
import io.github.jark006.freezeit.StaticData;
import io.github.jark006.freezeit.Utils;


public class Settings extends AppCompatActivity implements View.OnClickListener {
    final int INIT_UI = 1, SET_VAR_SUCCESS = 2, SET_VAR_FAIL = 3;

    Spinner freezeModeSpinner, reFreezeTimeoutSpinner;
    SeekBar freezeTimeoutSeekbar, wakeupTimeoutSeekbar, terminateTimeoutSeekbar;
    TextView freezeTimeoutText, wakeupTimeoutText, terminateTimeoutText;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch batterySwitch, currentSwitch,
            lmkSwitch, dozeSwitch, extendFgSwitch, dozeDebugSwitch;

    final int freezeTimeoutIdx = 2;
    final int wakeupTimeoutIdx = 3;
    final int terminateTimeoutIdx = 4;
    final int freezeModeIdx = 5;
    final int reFreezeTimeoutIdx = 6;

    final int batteryIdx = 13;
    final int currentIdx = 14;
    final int lmkIdx = 16;
    final int dozeIdx = 17;
    final int extendFgIdx = 18;

    final int dozeDebugIdx = 30;

    byte[] settingsVar = new byte[256];
    long lastTimestamp = 0;

    int varIndexForHandle = 0;
    int newValueForHandle = 0;

    ActivityResultLauncher<Intent> pickPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.freeze_mode_title).setOnClickListener(this);
        findViewById(R.id.freeze_timeout_title).setOnClickListener(this);
        findViewById(R.id.refreeze_timeout_title).setOnClickListener(this);
        findViewById(R.id.terminate_timeout_title).setOnClickListener(this);
        findViewById(R.id.wakeup_timeout_title).setOnClickListener(this);
        findViewById(R.id.battery_title).setOnClickListener(this);
        findViewById(R.id.current_title).setOnClickListener(this);
        findViewById(R.id.lmk_title).setOnClickListener(this);
        findViewById(R.id.doze_title).setOnClickListener(this);
        findViewById(R.id.extend_fg_title).setOnClickListener(this);
        findViewById(R.id.doze_debug_title).setOnClickListener(this);

        findViewById(R.id.set_bg).setOnClickListener(this);

        freezeModeSpinner = findViewById(R.id.freeze_mode_spinner);
        reFreezeTimeoutSpinner = findViewById(R.id.refreeze_timeout_spinner);

        freezeTimeoutText = findViewById(R.id.freeze_timeout_text);
        wakeupTimeoutText = findViewById(R.id.wakeup_timeout_text);
        terminateTimeoutText = findViewById(R.id.terminate_timeout_text);
        freezeTimeoutSeekbar = findViewById(R.id.seekBarTimeout);
        wakeupTimeoutSeekbar = findViewById(R.id.seekBarWakeup);
        terminateTimeoutSeekbar = findViewById(R.id.seekBarTerminate);

        batterySwitch = findViewById(R.id.switch_battery);
        currentSwitch = findViewById(R.id.switch_current);
        lmkSwitch = findViewById(R.id.switch_lmk);
        dozeSwitch = findViewById(R.id.switch_doze);
        extendFgSwitch = findViewById(R.id.switch_extend_fg);
        dozeDebugSwitch = findViewById(R.id.switch_doze_debug);

        if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Settings.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        pickPicture = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null)
                return;

            try {
                String imagePath = Utils.getFileAbsolutePath(this, result.getData().getData());
                var bg = BitmapFactory.decodeFile(imagePath);
                if (bg == null || bg.getHeight() == 0 || bg.getWidth() == 0) return;

                // 居中截取 宽:高 = 1:2
                if (bg.getHeight() > 2 * bg.getWidth())
                    bg = Bitmap.createBitmap(bg, 0, bg.getHeight() / 2 - bg.getWidth(),
                            bg.getWidth(), bg.getWidth() * 2);
                else if (bg.getHeight() < 2 * bg.getWidth())
                    bg = Bitmap.createBitmap(bg, bg.getWidth() / 2 - bg.getHeight() / 4, 0,
                            bg.getHeight() / 2, bg.getHeight());

                // 限制分辨率
                if (bg.getWidth() > 1080)
                    bg = Utils.resize(bg, 1080f / bg.getWidth());

                bg.compress(Bitmap.CompressFormat.JPEG, 90,
                        openFileOutput(StaticData.bgFileName, Context.MODE_PRIVATE));

                StaticData.bg = new BitmapDrawable(getResources(), bg);
                StaticData.bg.setAlpha(56);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        findViewById(R.id.container).setBackground(StaticData.getBackgroundDrawable(this));
        new Thread(() -> {
            var recvLen = Utils.freezeitTask(Utils.getSettings, null);
            if (recvLen != 256) {
                Toast.makeText(getBaseContext(), getString(R.string.get_settings_fail), Toast.LENGTH_LONG).show();
                return;
            }
            settingsVar = Arrays.copyOfRange(StaticData.response, 0, 256);
            if (settingsVar[freezeModeIdx] > 5) settingsVar[freezeModeIdx] = 0;
            if (settingsVar[reFreezeTimeoutIdx] > 4) settingsVar[reFreezeTimeoutIdx] = 2;
            if (settingsVar[freezeTimeoutIdx] > 60) settingsVar[freezeTimeoutIdx] = 10;
            if (settingsVar[wakeupTimeoutIdx] > 120) settingsVar[wakeupTimeoutIdx] = 30;
            if (settingsVar[terminateTimeoutIdx] > 120) settingsVar[terminateTimeoutIdx] = 30;
            handler.sendEmptyMessage(INIT_UI);
        }
        ).start();
    }


    void InitSpinner(Spinner spinner, int idx) {
        spinner.setSelection(settingsVar[idx]);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                if (settingsVar[idx] == spinnerPosition)
                    return;

                var now = System.currentTimeMillis();
                if ((now - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    spinner.setSelection(settingsVar[idx]);
                    return;
                }
                lastTimestamp = now;

                varIndexForHandle = idx;
                newValueForHandle = spinnerPosition;
                setVarTask();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    void InitSeekbar(SeekBar seekBar, TextView textView, int idx) {
        seekBar.setProgress(settingsVar[idx]);
        textView.setText(String.valueOf(settingsVar[idx]));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (settingsVar[idx] == seekBar.getProgress())
                    return;

                var now = System.currentTimeMillis();
                if ((now - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_SHORT).show();
                    seekBar.setProgress(settingsVar[idx]);//进度条，文字 恢复原值
                    textView.setText(String.valueOf(settingsVar[idx]));
                    return;
                }
                lastTimestamp = now;

                varIndexForHandle = idx;
                newValueForHandle = seekBar.getProgress();
                setVarTask();
            }
        });
    }

    void InitSwitch(@SuppressLint("UseSwitchCompatOrMaterialCode") Switch sw, int idx) {
        sw.setChecked(settingsVar[idx] != 0);

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (settingsVar[idx] == (isChecked ? 1 : 0))
                return;

            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 1000) {
                Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                sw.setChecked(settingsVar[idx] != 0);
                return;
            }
            lastTimestamp = now;

            varIndexForHandle = idx;
            newValueForHandle = isChecked ? 1 : 0;
            setVarTask();
        });
    }

    void setVarTask() {
        new Thread(() -> {
            byte[] request = {(byte) varIndexForHandle, (byte) newValueForHandle};
            var recvLen = Utils.freezeitTask(Utils.setSettingsVar, request);

            Message msg = Message.obtain();
            if (recvLen == 0) {
                msg.what = SET_VAR_FAIL;
                msg.obj = "UnknownError";
            } else {
                String res = new String(StaticData.response, 0, recvLen);
                if (res.equals("success")) {
                    msg.what = SET_VAR_SUCCESS;
                } else {
                    msg.what = SET_VAR_FAIL;
                    msg.obj = res;
                }
            }
            handler.sendMessage(msg);
        }
        ).start();
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INIT_UI:
                    InitSpinner(freezeModeSpinner, freezeModeIdx);
                    InitSpinner(reFreezeTimeoutSpinner, reFreezeTimeoutIdx);

                    InitSeekbar(freezeTimeoutSeekbar, freezeTimeoutText, freezeTimeoutIdx);
                    InitSeekbar(wakeupTimeoutSeekbar, wakeupTimeoutText, wakeupTimeoutIdx);
                    InitSeekbar(terminateTimeoutSeekbar, terminateTimeoutText, terminateTimeoutIdx);

                    InitSwitch(batterySwitch, batteryIdx);
                    InitSwitch(currentSwitch, currentIdx);
                    InitSwitch(lmkSwitch, lmkIdx);
                    InitSwitch(dozeSwitch, dozeIdx);
                    InitSwitch(extendFgSwitch, extendFgIdx);
                    InitSwitch(dozeDebugSwitch, dozeDebugIdx);
                    break;

                case SET_VAR_SUCCESS:
                    Toast.makeText(getBaseContext(), getString(R.string.setup_successful), Toast.LENGTH_SHORT).show();
                    settingsVar[varIndexForHandle] = (byte) newValueForHandle;
                    break;

                case SET_VAR_FAIL:
                    Toast.makeText(getBaseContext(), getString(R.string.setup_failed) + ": " + msg.obj, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.freeze_mode_title) {
            Utils.textDialog(this, R.string.freeze_mode_title, R.string.freeze_mode_tips);
        } else if (id == R.id.freeze_timeout_title) {
            Utils.textDialog(this, R.string.freeze_timeout_title, R.string.freeze_timeout_tips);
        } else if (id == R.id.refreeze_timeout_title) {
            Utils.textDialog(this, R.string.refreeze_timeout_title, R.string.refreeze_timeout_tips);
        } else if (id == R.id.terminate_timeout_title) {
            Utils.textDialog(this, R.string.terminate_timeout_title, R.string.terminate_timeout_tips);
        } else if (id == R.id.wakeup_timeout_title) {
            Utils.textDialog(this, R.string.wakeup_timeout_title, R.string.wakeup_timeout_tips);
        } else if (id == R.id.battery_title) {
            Utils.textDialog(this, R.string.battery_title, R.string.battery_tips);
        } else if (id == R.id.current_title) {
            Utils.textDialog(this, R.string.current_title, R.string.current_tips);
        } else if (id == R.id.lmk_title) {
            Utils.textDialog(this, R.string.lmk_title, R.string.lmk_tips);
        } else if (id == R.id.doze_title) {
            Utils.textDialog(this, R.string.doze_title, R.string.doze_tips);
        } else if (id == R.id.extend_fg_title) {
            Utils.textDialog(this, R.string.extend_fg_title, R.string.extend_fg_tips);
        } else if (id == R.id.doze_debug_title) {
            Utils.textDialog(this, R.string.doze_debug_title, R.string.doze_debug_tips);
        } else if (id == R.id.set_bg) {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("image/*");
            pickPicture.launch(intent);
        }
    }
}