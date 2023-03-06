package io.github.jark006.freezeit;

import android.annotation.SuppressLint;
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

import androidx.appcompat.app.AppCompatActivity;


public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    Spinner clusterBindSpinner, freezeModeSpinner, reFreezeTimeoutSpinner;
    SeekBar freezeTimeoutSeekbar, wakeupTimeoutSeekbar, terminateTimeoutSeekbar;
    TextView freezeTimeoutText, wakeupTimeoutText, terminateTimeoutText;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch batterySwitch, currentSwitch, breakNetworkSwitch,
            lmkSwitch, dozeSwitch, extendFgSwitch, dozeDebugSwitch;

    final int clusterBindIdx = 1;
    final int freezeTimeoutIdx = 2;
    final int wakeupTimeoutIdx = 3;
    final int terminateTimeoutIdx = 4;
    final int freezeModeIdx = 5;
    final int reFreezeTimeoutIdx = 6;

    final int batteryIdx = 13;
    final int currentIdx = 14;
    final int breakNetworkIdx = 15;
    final int lmkIdx = 16;
    final int dozeIdx = 17;
    final int extendFgIdx = 18;

    final int dozeDebugIdx = 30;

    byte[] settingsVar = new byte[256];
    long lastTimestamp = 0;

    int varIndexForHandle = 0;
    int newValueForHandle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.cluster_title).setOnClickListener(this);
        findViewById(R.id.freeze_mode_title).setOnClickListener(this);
        findViewById(R.id.freeze_timeout_title).setOnClickListener(this);
        findViewById(R.id.refreeze_timeout_title).setOnClickListener(this);
        findViewById(R.id.terminate_timeout_title).setOnClickListener(this);
        findViewById(R.id.wakeup_timeout_title).setOnClickListener(this);
        findViewById(R.id.battery_title).setOnClickListener(this);
        findViewById(R.id.current_title).setOnClickListener(this);
        findViewById(R.id.break_network_title).setOnClickListener(this);
        findViewById(R.id.lmk_title).setOnClickListener(this);
        findViewById(R.id.doze_title).setOnClickListener(this);
        findViewById(R.id.extend_fg_title).setOnClickListener(this);
        findViewById(R.id.doze_debug_title).setOnClickListener(this);


        clusterBindSpinner = findViewById(R.id.cluster_spinner);
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
        breakNetworkSwitch = findViewById(R.id.switch_break_network);
        lmkSwitch = findViewById(R.id.switch_lmk);
        dozeSwitch = findViewById(R.id.switch_doze);
        extendFgSwitch = findViewById(R.id.switch_extend_fg);
        dozeDebugSwitch = findViewById(R.id.switch_doze_debug);
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(() -> Utils.freezeitTask(Utils.getSettings, null, InitUI_Handler)).start();
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
                new Thread(() -> Utils.freezeitTask(
                        Utils.setSettingsVar,
                        new byte[]{(byte) varIndexForHandle, (byte) newValueForHandle},
                        setVarHandler)
                ).start();
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
                new Thread(() -> Utils.freezeitTask(
                        Utils.setSettingsVar,
                        new byte[]{(byte) varIndexForHandle, (byte) newValueForHandle},
                        setVarHandler)
                ).start();
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
            new Thread(() -> Utils.freezeitTask(
                    Utils.setSettingsVar,
                    new byte[]{(byte) varIndexForHandle, (byte) newValueForHandle},
                    setVarHandler)
            ).start();
        });
    }


    private final Handler setVarHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                Toast.makeText(getBaseContext(), getString(R.string.no_response), Toast.LENGTH_LONG).show();
                return;
            }
            String res = new String(response);
            if (res.equals("success")) {
                Toast.makeText(getBaseContext(), getString(R.string.setup_successful), Toast.LENGTH_SHORT).show();
                settingsVar[varIndexForHandle] = (byte) newValueForHandle;
            } else {
                Toast.makeText(getBaseContext(), getString(R.string.setup_failed) + ": " + res, Toast.LENGTH_LONG).show();
            }
        }
    };

    private final Handler InitUI_Handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length != 256) {
                Toast.makeText(getBaseContext(), getString(R.string.get_settings_fail), Toast.LENGTH_LONG).show();
                return;
            }
            settingsVar = response;

            if (settingsVar[clusterBindIdx] > 6) settingsVar[clusterBindIdx] = 0;
            if (settingsVar[freezeModeIdx] > 5) settingsVar[freezeModeIdx] = 0;
            if (settingsVar[reFreezeTimeoutIdx] > 4) settingsVar[reFreezeTimeoutIdx] = 2;
            if (settingsVar[freezeTimeoutIdx] > 60) settingsVar[freezeTimeoutIdx] = 10;
            if (settingsVar[wakeupTimeoutIdx] > 120) settingsVar[wakeupTimeoutIdx] = 30;
            if (settingsVar[terminateTimeoutIdx] > 120) settingsVar[terminateTimeoutIdx] = 30;

            InitSpinner(clusterBindSpinner, clusterBindIdx);
            InitSpinner(freezeModeSpinner, freezeModeIdx);
            InitSpinner(reFreezeTimeoutSpinner, reFreezeTimeoutIdx);

            InitSeekbar(freezeTimeoutSeekbar, freezeTimeoutText, freezeTimeoutIdx);
            InitSeekbar(wakeupTimeoutSeekbar, wakeupTimeoutText, wakeupTimeoutIdx);
            InitSeekbar(terminateTimeoutSeekbar, terminateTimeoutText, terminateTimeoutIdx);

            InitSwitch(batterySwitch, batteryIdx);
            InitSwitch(currentSwitch, currentIdx);
            InitSwitch(breakNetworkSwitch, breakNetworkIdx);
            InitSwitch(lmkSwitch, lmkIdx);
            InitSwitch(dozeSwitch, dozeIdx);
            InitSwitch(extendFgSwitch, extendFgIdx);

            InitSwitch(dozeDebugSwitch, dozeDebugIdx);
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cluster_title) {
            Utils.textDialog(this, R.string.cluster_title, R.string.cluster_tips);
        } else if (id == R.id.freeze_mode_title) {
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
        } else if (id == R.id.break_network_title) {
            Utils.textDialog(this, R.string.break_network_title, R.string.break_network_tips);
        } else if (id == R.id.lmk_title) {
            Utils.textDialog(this, R.string.lmk_title, R.string.lmk_tips);
        } else if (id == R.id.doze_title) {
            Utils.textDialog(this, R.string.doze_title, R.string.doze_tips);
        } else if (id == R.id.extend_fg_title) {
            Utils.textDialog(this, R.string.extend_fg_title, R.string.extend_fg_tips);
        } else if (id == R.id.doze_debug_title) {
            Utils.textDialog(this, R.string.doze_debug_title, R.string.doze_debug_tips);
        }
    }
}