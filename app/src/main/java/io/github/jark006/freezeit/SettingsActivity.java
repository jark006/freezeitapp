package io.github.jark006.freezeit;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
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



public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    final String TAG = "Settings";

    Chip chipForeground, chipBatteryMonitor, chipBatteryFix, chipBreakNetwork, chipLmk, chipDoze;
    SeekBar seekbarCPU, seekbarTimeouts, seekbarWakeup, seekbarTerminate, seekbarMode;
    TextView cpuText, timeoutsText, wakeupText, terminateText, modeText;


    //    final int verIdx = 0;
    final int clusterBindIdx = 1;
    final int freezeTimeoutIdx = 2;
    final int wakeupTimeoutIdx = 3;
    final int terminateTimeoutIdx = 4;
    final int setModeIdx = 5;

    final int radicalIdx = 10;

    final int batteryMonitorIdx = 13;
    final int batteryFixIdx = 14;
    final int breakNetworkIdx = 15;
    final int lmkAdjustIdx = 16;
    final int dozeIdx = 17;

    final String[] freezerModeText = {"全局SIGSTOP", "FreezerV1(uid)", "FreezerV1(frozen)", "FreezerV2(uid)", "FreezerV2(frozen)", "自动选择",};

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

        setContentView(R.layout.activity_settings);

        findViewById(R.id.coolapk_link).setOnClickListener(this);
        findViewById(R.id.github_link).setOnClickListener(this);
        findViewById(R.id.lanzou_link).setOnClickListener(this);

        chipForeground = findViewById(R.id.chip_foreground);
        chipBatteryMonitor = findViewById(R.id.chip_battery);
        chipBatteryFix = findViewById(R.id.chip_current);
        chipBreakNetwork = findViewById(R.id.chip_break_network);
        chipLmk = findViewById(R.id.chip_lmk);
        chipDoze = findViewById(R.id.chip_doze);

        chipForeground.setOnClickListener(this);
        chipBatteryMonitor.setOnClickListener(this);
        chipBatteryFix.setOnClickListener(this);
        chipBreakNetwork.setOnClickListener(this);
        chipLmk.setOnClickListener(this);
        chipDoze.setOnClickListener(this);

        cpuText = findViewById(R.id.cpu_text);
        timeoutsText = findViewById(R.id.timeout_text);
        wakeupText = findViewById(R.id.wakeup_text);
        terminateText = findViewById(R.id.terminate_text);
        modeText = findViewById(R.id.mode_text);

        seekbarCPU = findViewById(R.id.seekBarCPU);
        seekbarTimeouts = findViewById(R.id.seekBarTimeout);
        seekbarWakeup = findViewById(R.id.seekBarWakeup);
        seekbarTerminate = findViewById(R.id.seekBarTerminate);
        seekbarMode = findViewById(R.id.seekBarMode);


        seekbarCPU.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                cpuText.setText(String.format(getString(R.string.bind_core_text), getClusterText(value)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarCPU;
                textViewForHandle = cpuText;
                stringIndexForHandle = R.string.bind_core_text;
                varIndexForHandle = clusterBindIdx;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress(settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), getClusterText(settingsVar[varIndexForHandle])));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
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
                varIndexForHandle = freezeTimeoutIdx;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress(settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle]));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });

        seekbarWakeup.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                wakeupText.setText(String.format(getString(R.string.refreeze_text), value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarWakeup;
                textViewForHandle = wakeupText;
                stringIndexForHandle = R.string.refreeze_text;
                varIndexForHandle = wakeupTimeoutIdx;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress(settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle]));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });

        seekbarTerminate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                terminateText.setText(String.format(getString(R.string.terminate_text), value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarTerminate;
                textViewForHandle = terminateText;
                stringIndexForHandle = R.string.terminate_text;
                varIndexForHandle = terminateTimeoutIdx;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress(settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), (int) settingsVar[varIndexForHandle]));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });

        seekbarMode.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = seekBar.getProgress();
                modeText.setText(String.format(getString(R.string.mode_text), freezerModeText[value]));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarForHandle = seekbarMode;
                textViewForHandle = modeText;
                stringIndexForHandle = R.string.mode_text;
                varIndexForHandle = setModeIdx;

                final byte value = (byte) seekBar.getProgress();
                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    seekBarForHandle.setProgress(settingsVar[varIndexForHandle]);
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), freezerModeText[settingsVar[varIndexForHandle]]));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();
                new Thread(() -> Utils.freezeitTask(Utils.setSettingsVar, new byte[]{(byte) varIndexForHandle, value}, seekbarHandler)).start();
            }
        });
    }

    String getClusterText(int idx) {
        switch (idx) {
            case 0:
            default:return "[0][1][2][3][_][_][_][_]"; // default -> 0
            case 1: return "[0][1][2][_][_][_][_][_]";
            case 2: return "[_][_][_][3][4][_][_][_]";
            case 3: return "[_][_][_][_][4][5][6][_]";
            case 4: return "[_][_][_][_][_][5][6][_]";
            case 5: return "[_][_][_][_][_][_][_][7]";
            case 6: return "[_][_][_][_][4][5][6][7]";
        }
    }

    private final Handler seekbarHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                final String errorTips = "handleMessage: seekbarHandler回应失败";
                Toast.makeText(getBaseContext(), errorTips, Toast.LENGTH_LONG).show();
                Log.e(TAG, errorTips);
                return;
            }
            String res = new String(response, StandardCharsets.UTF_8);
            if (res.equals("success")) {
                Toast.makeText(getBaseContext(), "设置成功", Toast.LENGTH_LONG).show();
                settingsVar[varIndexForHandle] = (byte) seekBarForHandle.getProgress();
            } else {
                Toast.makeText(getBaseContext(), "设置失败" + res, Toast.LENGTH_LONG).show();
                seekBarForHandle.setProgress(settingsVar[varIndexForHandle]); //进度条，文字 恢复原值
                if (stringIndexForHandle == R.string.bind_core_text)
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), getClusterText(settingsVar[varIndexForHandle])));
                else if (stringIndexForHandle == R.string.mode_text)
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), freezerModeText[settingsVar[varIndexForHandle]]));
                else
                    textViewForHandle.setText(String.format(getString(stringIndexForHandle), settingsVar[varIndexForHandle]));
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
                final String errorTips = "handleMessage: 设置数据获取失败";
                Toast.makeText(getBaseContext(), errorTips, Toast.LENGTH_LONG).show();
                Log.e(TAG, errorTips);
                return;
            }
            settingsVar = response;
            refreshView();
        }
    };

    void refreshView() {
        if (settingsVar[clusterBindIdx] > 6) settingsVar[clusterBindIdx] = 0;
        if (settingsVar[freezeTimeoutIdx] > 60) settingsVar[freezeTimeoutIdx] = 10;
        if (settingsVar[wakeupTimeoutIdx] > 120) settingsVar[wakeupTimeoutIdx] = 30;
        if (settingsVar[terminateTimeoutIdx] > 120) settingsVar[terminateTimeoutIdx] = 30;
        if (settingsVar[setModeIdx] > 5) settingsVar[setModeIdx] = 0;


        seekbarCPU.setProgress(settingsVar[clusterBindIdx]);
        cpuText.setText(String.format(getString(R.string.bind_core_text), getClusterText(settingsVar[clusterBindIdx])));

        seekbarTimeouts.setProgress(settingsVar[freezeTimeoutIdx]);
        timeoutsText.setText(String.format(getString(R.string.timeout_text), settingsVar[freezeTimeoutIdx]));

        seekbarWakeup.setProgress(settingsVar[wakeupTimeoutIdx]);
        wakeupText.setText(String.format(getString(R.string.refreeze_text), settingsVar[wakeupTimeoutIdx]));

        seekbarTerminate.setProgress(settingsVar[terminateTimeoutIdx]);
        terminateText.setText(String.format(getString(R.string.terminate_text), settingsVar[terminateTimeoutIdx]));

        seekbarMode.setProgress(settingsVar[setModeIdx]);
        modeText.setText(String.format(getString(R.string.mode_text), freezerModeText[settingsVar[setModeIdx]]));

        updateChip(chipForeground, settingsVar[radicalIdx] != 0);
        updateChip(chipBatteryMonitor, settingsVar[batteryMonitorIdx] != 0);
        updateChip(chipBatteryFix, settingsVar[batteryFixIdx] != 0);
        updateChip(chipBreakNetwork, settingsVar[breakNetworkIdx] != 0);
        updateChip(chipLmk, settingsVar[lmkAdjustIdx] != 0);
        updateChip(chipDoze, settingsVar[dozeIdx] != 0);
    }


    private final Handler chipHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                final String errorTips = "handleMessage: chipVarIndex回应失败" + varIndexForHandle;
                Toast.makeText(getBaseContext(), errorTips, Toast.LENGTH_LONG).show();
                Log.e(TAG, errorTips);
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
        if (id == R.id.chip_foreground) {
            chipForHandle = chipForeground;
            varIndexForHandle = radicalIdx;
        } else if (id == R.id.chip_battery) {
            chipForHandle = chipBatteryMonitor;
            varIndexForHandle = batteryMonitorIdx;
        } else if (id == R.id.chip_current) {
            chipForHandle = chipBatteryFix;
            varIndexForHandle = batteryFixIdx;
        } else if (id == R.id.chip_break_network) {
            chipForHandle = chipBreakNetwork;
            varIndexForHandle = breakNetworkIdx;
        } else if (id == R.id.chip_lmk) {
            chipForHandle = chipLmk;
            varIndexForHandle = lmkAdjustIdx;
        } else if (id == R.id.chip_doze) {
            chipForHandle = chipDoze;
            varIndexForHandle = dozeIdx;
        } else if (id == R.id.coolapk_link) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
                intent.setAction("android.intent.action.VIEW");
                intent.setData(Uri.parse("coolmarket://u/1212220"));
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.coolapk_link))));
            }
            return;
        } else if (id == R.id.github_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link))));
            return;
        } else if (id == R.id.lanzou_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.lanzou_link))));
            return;
        } else {
            return;
        }

        // 点击按钮进行切换，发送当前的 相反状态
        new Thread(() -> Utils.freezeitTask(
                Utils.setSettingsVar,
                new byte[]{(byte) varIndexForHandle, (byte) (settingsVar[varIndexForHandle] == 0 ? 1 : 0)},
                chipHandler
        )).start();

    }


    void updateChip(Chip chip, boolean bool) {
        chip.setText(bool ? "已开启" : "已关闭");
        chip.setChipBackgroundColor(ColorStateList.valueOf(bool ? 0x6600ff00 : 0x66ff0000));
    }
}