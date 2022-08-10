package com.jark006.freezeit;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Settings extends AppCompatActivity implements View.OnClickListener {
    final String TAG = "Settings";

    Chip chipForeground, chipPlay, chipCapture, chipBattery, chipDynamic, chipOOM;
    SeekBar seekbarCPU, seekbarTimeouts, seekbarRefreeze, seekbarMain, seekbarSub;
    TextView reboot, rebootRecovery, rebootBootloader, rebootEdl;
    TextView cpuText, timeoutsText, refreezeText, mainOomText, subOomText;

    byte[] settingsVar = new byte[256];
    long lastTimestamp = System.currentTimeMillis();
    Chip chipForHandle;
    SeekBar seekBarForHandle;
    TextView textViewForHandle;
    int varIndexForHandle = 0;
    int stringIndexForHandle = 0;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);

        chipForeground = findViewById(R.id.chip_foreground);
        chipPlay = findViewById(R.id.chip_play);
        chipCapture = findViewById(R.id.chip_capture);
        chipBattery = findViewById(R.id.chip_battery);
        chipDynamic = findViewById(R.id.chip_dynamic);
        chipOOM = findViewById(R.id.chip_oom);

        chipForeground.setOnClickListener(this);
        chipPlay.setOnClickListener(this);
        chipCapture.setOnClickListener(this);
        chipBattery.setOnClickListener(this);
        chipDynamic.setOnClickListener(this);
        chipOOM.setOnClickListener(this);


        reboot = findViewById(R.id.reboot);
        rebootRecovery = findViewById(R.id.reboot_recovery);
        rebootBootloader = findViewById(R.id.reboot_bootloader);
        rebootEdl = findViewById(R.id.reboot_edl);

        reboot.setOnClickListener(this);
        rebootRecovery.setOnClickListener(this);
        rebootBootloader.setOnClickListener(this);
        rebootEdl.setOnClickListener(this);


        cpuText = findViewById(R.id.cpu_text);
        timeoutsText = findViewById(R.id.timeout_text);
        refreezeText = findViewById(R.id.refreeze_text);
        mainOomText = findViewById(R.id.main_oom_text);
        subOomText = findViewById(R.id.sub_oom_text);

        seekbarCPU = findViewById(R.id.seekBarCPU);
        seekbarTimeouts = findViewById(R.id.seekBarTimeout);
        seekbarRefreeze = findViewById(R.id.seekBarRefreeze);
        seekbarMain = findViewById(R.id.seekBarMain);
        seekbarSub = findViewById(R.id.seekBarSub);


        seekbarCPU.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                cpuText.setText(String.format(getString(R.string.bind_core_text), value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarCPU;
                textViewForHandle = cpuText;
                stringIndexForHandle = R.string.bind_core_text;
                varIndexForHandle = 1;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress((int) settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle]));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
//                settingsVar[varIndexForHandle] = value;
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });

        seekbarTimeouts.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                timeoutsText.setText(String.format(getString(R.string.timeout_text), value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarTimeouts;
                textViewForHandle = timeoutsText;
                stringIndexForHandle = R.string.timeout_text;
                varIndexForHandle = 2;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress((int) settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle]));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
//                settingsVar[varIndexForHandle] = value;
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });

        seekbarRefreeze.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                refreezeText.setText(String.format(getString(R.string.refreeze_text), value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarRefreeze;
                textViewForHandle = refreezeText;
                stringIndexForHandle = R.string.refreeze_text;
                varIndexForHandle = 3;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress((int) settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle]));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
//                settingsVar[varIndexForHandle] = value;
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });

        seekbarMain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                mainOomText.setText(String.format(getString(R.string.main_oom_text), value * 100 - 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarMain;
                textViewForHandle = mainOomText;
                stringIndexForHandle = R.string.main_oom_text;
                varIndexForHandle = 10;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress((int) settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle] * 100 - 1000));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
//                settingsVar[varIndexForHandle] = value;
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });

        seekbarSub.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                subOomText.setText(String.format(getString(R.string.sub_oom_text), value * 100 - 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarSub;
                textViewForHandle = subOomText;
                stringIndexForHandle = R.string.sub_oom_text;
                varIndexForHandle = 11;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress((int) settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle] * 100 - 1000));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
//                settingsVar[varIndexForHandle] = value;
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });

    }

    private final Handler seekbarHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                final String tips = "handleMessage: seekbarHandler回应失败";
                Toast.makeText(getBaseContext(), tips, Toast.LENGTH_LONG).show();
                Log.e(TAG, tips);
                return;
            }
            String res = new String(response, StandardCharsets.UTF_8);
            if (res.equals("success")) {
                Toast.makeText(getBaseContext(), "设置成功", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getBaseContext(), "设置失败" + res, Toast.LENGTH_LONG).show();
                seekBarForHandle.setProgress(settingsVar[varIndexForHandle]);
                if (varIndexForHandle == 10 || varIndexForHandle == 11) {
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle] * 100 - 1000));
                } else {
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle]));
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        new Thread(() -> Utils.freezeitTask(Utils.getSettings, null, handler)).start();
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length != 256) {
                final String tips = "handleMessage: 设置数据获取失败";
                Toast.makeText(getBaseContext(), tips, Toast.LENGTH_LONG).show();
                Log.e(TAG, tips);
                return;
            }
            settingsVar = Arrays.copyOf(response, response.length);

            byte[] tmp = Arrays.copyOf(response, 20);
            Log.i(TAG, "handleMessage: 设置 " + Arrays.toString(tmp));

            refreshView();
        }
    };

    void refreshView() {

        seekbarCPU.setProgress(settingsVar[1]);
        cpuText.setText(String.format(getString(R.string.bind_core_text), settingsVar[1]));

        seekbarTimeouts.setProgress(settingsVar[2]);
        timeoutsText.setText(String.format(getString(R.string.timeout_text), settingsVar[2]));

        seekbarRefreeze.setProgress(settingsVar[3]);
        refreezeText.setText(String.format(getString(R.string.refreeze_text), settingsVar[3]));

        updateChip(chipForeground, settingsVar[4] != 0);
        updateChip(chipPlay, settingsVar[5] != 0);
        updateChip(chipCapture, settingsVar[6] != 0);
        updateChip(chipBattery, settingsVar[7] != 0);
        updateChip(chipDynamic, settingsVar[8] != 0);

        updateChip(chipOOM, settingsVar[9] != 0);

        seekbarMain.setProgress(settingsVar[10]);
        mainOomText.setText(String.format(getString(R.string.main_oom_text), settingsVar[10] * 100 - 1000));

        seekbarSub.setProgress(settingsVar[11]);
        subOomText.setText(String.format(getString(R.string.sub_oom_text), settingsVar[11] * 100 - 1000));

    }


    private final Handler chipHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                final String tips = "handleMessage: chipVarIndex回应失败" + varIndexForHandle;
                Toast.makeText(getBaseContext(), tips, Toast.LENGTH_LONG).show();
                Log.e(TAG, tips);
                return;
            }
            String res = new String(response, StandardCharsets.UTF_8);
            if (res.equals("success")) {
                settingsVar[varIndexForHandle] = (settingsVar[varIndexForHandle] != 0) ? (byte) 0 : (byte) 1;
                updateChip(chipForHandle, settingsVar[varIndexForHandle] != 0);
            } else {
                Toast.makeText(getBaseContext(), "设置失败" + res, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onClick(View v) {
        if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
            Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
            return;
        }
        lastTimestamp = System.currentTimeMillis();

        int id = v.getId();
        if (id == R.id.chip_foreground) { // 点击按钮进行切换，发送当前状态的 反命令
            chipForHandle = chipForeground;
            varIndexForHandle = 4;
            new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, (byte) (settingsVar[varIndexForHandle] == 0 ? 1 : 0)}, chipHandler)).start();
        } else if (id == R.id.chip_play) {
            chipForHandle = chipPlay;
            varIndexForHandle = 5;
            new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, (byte) (settingsVar[varIndexForHandle] == 0 ? 1 : 0)}, chipHandler)).start();
        } else if (id == R.id.chip_capture) {
            chipForHandle = chipCapture;
            varIndexForHandle = 6;
            new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, (byte) (settingsVar[varIndexForHandle] == 0 ? 1 : 0)}, chipHandler)).start();
        } else if (id == R.id.chip_battery) {
            chipForHandle = chipBattery;
            varIndexForHandle = 7;
            new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, (byte) (settingsVar[varIndexForHandle] == 0 ? 1 : 0)}, chipHandler)).start();
        } else if (id == R.id.chip_dynamic) {
            chipForHandle = chipDynamic;
            varIndexForHandle = 8;
            new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, (byte) (settingsVar[varIndexForHandle] == 0 ? 1 : 0)}, chipHandler)).start();
        } else if (id == R.id.chip_oom) {
            chipForHandle = chipOOM;
            varIndexForHandle = 9;
            new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, (byte) (settingsVar[varIndexForHandle] == 0 ? 1 : 0)}, chipHandler)).start();
        } else if (id == R.id.reboot) {
            new Thread(() -> Utils.freezeitTask(Utils.reboot, null, null)).start();
        } else if (id == R.id.reboot_recovery) {
            new Thread(() -> Utils.freezeitTask(Utils.rebootRecovery, null, null)).start();
        } else if (id == R.id.reboot_bootloader) {
            new Thread(() -> Utils.freezeitTask(Utils.rebootBootloader, null, null)).start();
        } else if (id == R.id.reboot_edl) {
            new Thread(() -> Utils.freezeitTask(Utils.rebootEdl, null, null)).start();
        }
    }


    void updateChip(Chip chip, boolean bool) {
        chip.setText(bool ? "已开启" : "已关闭");
        chip.setChipBackgroundColor(ColorStateList.valueOf(bool ? 0x6600ff00 : 0x66ff0000));
    }
}