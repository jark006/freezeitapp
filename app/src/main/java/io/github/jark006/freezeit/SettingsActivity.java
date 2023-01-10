package io.github.jark006.freezeit;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
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

import java.nio.charset.StandardCharsets;


public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    final String TAG = "Settings";

    Spinner clusterBindSpinner, freezeModeSpinner;
    SeekBar freezeTimeoutSeekbar, wakeupTimeoutSeekbar, terminateTimeoutSeekbar;
    TextView freezeTimeoutText, wakeupTimeoutText, terminateTimeoutText;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch radicalFgSwitch, batterySwitch, currentSwitch, breakNetworkSwitch,
            lmkSwitch, dozeSwitch, extendFgSwitch;


    final int clusterBindIdx = 1;
    final int freezeTimeoutIdx = 2;
    final int wakeupTimeoutIdx = 3;
    final int terminateTimeoutIdx = 4;
    final int freezeModeIdx = 5;

    final int radicalFgIdx = 10;

    final int batteryIdx = 13;
    final int currentIdx = 14;
    final int breakNetworkIdx = 15;
    final int lmkIdx = 16;
    final int dozeIdx = 17;
    final int extendFgIdx = 18;

    byte[] settingsVar = new byte[256];
    long lastTimestamp = System.currentTimeMillis();

    int varIndexForHandle = 0;
    int newValueForHandle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.cluster_title).setOnClickListener(this);
        findViewById(R.id.freeze_mode_title).setOnClickListener(this);
        findViewById(R.id.freeze_timeout_title).setOnClickListener(this);
        findViewById(R.id.terminate_timeout_title).setOnClickListener(this);
        findViewById(R.id.wakeup_timeout_title).setOnClickListener(this);
        findViewById(R.id.radical_fg_title).setOnClickListener(this);
        findViewById(R.id.battery_title).setOnClickListener(this);
        findViewById(R.id.current_title).setOnClickListener(this);
        findViewById(R.id.break_network_title).setOnClickListener(this);
        findViewById(R.id.lmk_title).setOnClickListener(this);
        findViewById(R.id.doze_title).setOnClickListener(this);
        findViewById(R.id.extend_fg_title).setOnClickListener(this);


        findViewById(R.id.coolapk_link).setOnClickListener(this);
        findViewById(R.id.github_link).setOnClickListener(this);
        findViewById(R.id.lanzou_link).setOnClickListener(this);

        findViewById(R.id.qq_group_link).setOnClickListener(this);
        findViewById(R.id.qq_channel_link).setOnClickListener(this);
        findViewById(R.id.telegram_link).setOnClickListener(this);
        findViewById(R.id.tutorial_link).setOnClickListener(this);
        findViewById(R.id.changelog_text).setOnClickListener(this);

        findViewById(R.id.wechat_pay).setOnClickListener(this);
        findViewById(R.id.alipay).setOnClickListener(this);


        clusterBindSpinner = findViewById(R.id.cluster_spinner);
        freezeModeSpinner = findViewById(R.id.freeze_mode_spinner);

        freezeTimeoutText = findViewById(R.id.freeze_timeout_text);
        wakeupTimeoutText = findViewById(R.id.wakeup_timeout_text);
        terminateTimeoutText = findViewById(R.id.terminate_timeout_text);
        freezeTimeoutSeekbar = findViewById(R.id.seekBarTimeout);
        wakeupTimeoutSeekbar = findViewById(R.id.seekBarWakeup);
        terminateTimeoutSeekbar = findViewById(R.id.seekBarTerminate);

        radicalFgSwitch = findViewById(R.id.switch_radical_fg);
        batterySwitch = findViewById(R.id.switch_battery);
        currentSwitch = findViewById(R.id.switch_current);
        breakNetworkSwitch = findViewById(R.id.switch_break_network);
        lmkSwitch = findViewById(R.id.switch_lmk);
        dozeSwitch = findViewById(R.id.switch_doze);
        extendFgSwitch = findViewById(R.id.switch_extend_fg);
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

                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    spinner.setSelection(settingsVar[idx]);
                    return;
                }
                lastTimestamp = System.currentTimeMillis();

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

                if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                    Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_SHORT).show();
                    seekBar.setProgress(settingsVar[idx]);//进度条，文字 恢复原值
                    textView.setText(String.valueOf(settingsVar[idx]));
                    return;
                }
                lastTimestamp = System.currentTimeMillis();

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

            if ((System.currentTimeMillis() - lastTimestamp) < 1000) {
                Toast.makeText(getBaseContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                sw.setChecked(settingsVar[idx] != 0);
                return;
            }
            lastTimestamp = System.currentTimeMillis();

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
            String res = new String(response, StandardCharsets.UTF_8);
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
            if (settingsVar[freezeTimeoutIdx] > 60) settingsVar[freezeTimeoutIdx] = 10;
            if (settingsVar[terminateTimeoutIdx] > 120) settingsVar[terminateTimeoutIdx] = 30;
            if (settingsVar[wakeupTimeoutIdx] > 120) settingsVar[wakeupTimeoutIdx] = 30;

            InitSpinner(clusterBindSpinner, clusterBindIdx);
            InitSpinner(freezeModeSpinner, freezeModeIdx);

            InitSeekbar(freezeTimeoutSeekbar, freezeTimeoutText, freezeTimeoutIdx);
            InitSeekbar(wakeupTimeoutSeekbar, wakeupTimeoutText, wakeupTimeoutIdx);
            InitSeekbar(terminateTimeoutSeekbar, terminateTimeoutText, terminateTimeoutIdx);

            InitSwitch(radicalFgSwitch, radicalFgIdx);
            InitSwitch(batterySwitch, batteryIdx);
            InitSwitch(currentSwitch, currentIdx);
            InitSwitch(breakNetworkSwitch, breakNetworkIdx);
            InitSwitch(lmkSwitch, lmkIdx);
            InitSwitch(dozeSwitch, dozeIdx);
            InitSwitch(extendFgSwitch, extendFgIdx);
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
        } else if (id == R.id.terminate_timeout_title) {
            Utils.textDialog(this, R.string.terminate_timeout_title, R.string.terminate_timeout_tips);
        } else if (id == R.id.wakeup_timeout_title) {
            Utils.textDialog(this, R.string.wakeup_timeout_title, R.string.wakeup_timeout_tips);
        } else if (id == R.id.radical_fg_title) {
            Utils.textDialog(this, R.string.radical_fg_title, R.string.radical_fg_tips);
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
        } else if (id == R.id.coolapk_link) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
                intent.setAction("android.intent.action.VIEW");
                intent.setData(Uri.parse("coolmarket://u/1212220"));
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.coolapk_link))));
            }
        } else if (id == R.id.github_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link))));
        } else if (id == R.id.lanzou_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.lanzou_link))));
        } else if (id == R.id.qq_group_link) {
            try {
                //【冻它模块 freezeit】(781222669) 的 key 为： ntLAwm7WxB0hVcetV7DsxfNTVN16cGUD
                String key = "ntLAwm7WxB0hVcetV7DsxfNTVN16cGUD";
                Intent intent = new Intent();
                intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.qq_group_link))));
            }
        } else if (id == R.id.qq_channel_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.qq_channel_link))));
        } else if (id == R.id.telegram_link) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tg_link))));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tg_https_link))));
            }
        } else if (id == R.id.tutorial_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tutorial_link))));
        } else if (id == R.id.changelog_text) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.online_changelog_link))));
        } else if (id == R.id.wechat_pay) {
            Utils.imgDialog(this, R.drawable.wechatpay);
        } else if (id == R.id.alipay) {
            Utils.imgDialog(this, R.drawable.alipay);
        }
    }
}