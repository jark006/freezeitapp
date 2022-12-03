package com.jark006.freezeit.fragment;

import static android.content.Context.ACTIVITY_SERVICE;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.jark006.freezeit.AppTimeActivity;
import com.jark006.freezeit.BuildConfig;
import com.jark006.freezeit.R;
import com.jark006.freezeit.SettingsActivity;
import com.jark006.freezeit.Utils;
import com.jark006.freezeit.databinding.FragmentHomeBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

//                            _ooOoo_
//                           o8888888o
//                           88" . "88
//                           (| -_- |)
//                           O\  =  /O
//                        ____/`---'\____
//                      .'  \\|     |//  `.
//                     /  \\|||  :  |||//  \
//                    /  _||||| -:- |||||-  \
//                    |   | \\\  -  /// |   |
//                    | \_|  ''\---/''  |   |
//                    \  .-\__  `-`  ___/-. /
//                  ___`. .'  /--.--\  `. . __
//               ."" '<  `.___\_<|>_/___.'  >'"".
//              | | :  `- \`.;`\ _ /`;.`/ - ` : | |
//              \  \ `-.   \_ __\ /__ _/   .-` /  /
//         ======`-.____`-.___\_____/___.-`____.-'======
//                            `=---='
//        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                     佛祖保佑，代码永无BUG，阿弥陀佛

public class HomeFragment extends Fragment implements View.OnClickListener {
    private final static String TAG = "HomeFragment";
    private FragmentHomeBinding binding;

    ConstraintLayout constraintLayout;
    LinearLayout stateLayout, memLayout;
    TextView moduleStatus, memInfo, zramInfo,
            updateTips, changelogTips, battery, cpu, cpu0, cpu1, cpu2, cpu3, cpu4, cpu5, cpu6, cpu7,
            qqGroupLink, qqChannelLink, tgLink, tgChannelLink, tutorialLink;
    ImageView cpuImg, wechatPay, aliPay, qqpay, ecnyPay, ethereumPay, bitcoinPay;

    boolean moduleIsRunning = false;
    String moduleName;
    String moduleVersion;
    int moduleVersionCode = 0;

    String version = "";
    int versionCode = 0;
    String zipUrl = null;
    String changelogUrl = null;

    Timer timer;

    int viewWidth = 0;
    int viewHeight = 0;
    long availMem = 0;

    ActivityManager am;
    ActivityManager.MemoryInfo memoryInfo;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        am = (ActivityManager) requireActivity().getSystemService(ACTIVITY_SERVICE);
        memoryInfo = new ActivityManager.MemoryInfo();

        constraintLayout = binding.constraintLayoutHome;
        moduleStatus = binding.statusText;
        stateLayout = binding.stateLayout;
        memLayout = binding.memLayout;

        updateTips = binding.updateTips;
        changelogTips = binding.changelogTips;
        battery = binding.battery;
        cpu = binding.cpu;
        cpu0 = binding.cpu0;
        cpu1 = binding.cpu1;
        cpu2 = binding.cpu2;
        cpu3 = binding.cpu3;
        cpu4 = binding.cpu4;
        cpu5 = binding.cpu5;
        cpu6 = binding.cpu6;
        cpu7 = binding.cpu7;

        cpuImg = binding.cpuImg;
        memInfo = binding.memInfo;
        zramInfo = binding.zramInfo;


        wechatPay = binding.wechatpay;
        aliPay = binding.alipay;
        qqpay = binding.qqpay;
        ecnyPay = binding.ecnypay;
        ethereumPay = binding.ethereumpay;
        bitcoinPay = binding.bitcoinpay;

        qqGroupLink = binding.qqgroupLink;
        qqChannelLink = binding.qqchannelLink;
        tgLink = binding.telegramLink;
        tgChannelLink = binding.telegramChannelLink;
        tutorialLink = binding.tutorialLink;

        stateLayout.setOnClickListener(this);
        memLayout.setOnClickListener(this);

        updateTips.setOnClickListener(this);
        changelogTips.setOnClickListener(this);

        wechatPay.setOnClickListener(this);
        aliPay.setOnClickListener(this);
        qqpay.setOnClickListener(this);
        ecnyPay.setOnClickListener(this);
        ethereumPay.setOnClickListener(this);
        bitcoinPay.setOnClickListener(this);

        qqGroupLink.setOnClickListener(this);
        qqChannelLink.setOnClickListener(this);
        tgLink.setOnClickListener(this);
        tgChannelLink.setOnClickListener(this);
        tutorialLink.setOnClickListener(this);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.home_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.help_home) {
                    Utils.imgDialog(requireContext(), R.drawable.help_home);
                } else if (id == R.id.settings) {
                    if (moduleIsRunning)
                        startActivity(new Intent(requireContext(), SettingsActivity.class));
                    else
                        Snackbar.make(constraintLayout, getString(R.string.freezeit_offline), Snackbar.LENGTH_SHORT).show();
                }
                return false;
            }
        }, this.getViewLifecycleOwner());

        cpuImg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                viewWidth = cpuImg.getWidth();
                viewHeight = cpuImg.getHeight();
                cpuImg.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                new Thread(realTimeTask).start();
            }
        });

        new Thread(() -> Utils.freezeitTask(Utils.getInfo, null, statusHandler)).start();

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        timer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (viewHeight == 0 || viewWidth == 0)
                    return;
                new Thread(realTimeTask).start();
            }
        }, 2000, 3000);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private final Handler realTimeHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                memLayout.setVisibility(View.GONE);
                return;
            }

            if (viewHeight == 0 || viewWidth == 0)
                return;

            Bitmap bitmap = Bitmap.createBitmap(viewWidth / 3, viewHeight / 3, Bitmap.Config.ARGB_8888);
            ByteBuffer buffer = ByteBuffer.wrap(response);
            bitmap.copyPixelsFromBuffer(buffer);

            cpuImg.setImageBitmap(bitmap);

            int offset = (viewWidth / 3) * (viewHeight / 3) * 4;

            if (response.length - offset <= 0) {
                String errorTips = "handleMessage: viewWidth" + viewWidth + " viewHeight" +
                        viewHeight + " response.length" + response.length + " offset" + offset;
                Log.e(TAG, errorTips);
                memInfo.setText(errorTips);
                return;
            }

            byte[] tmpBytes = new byte[response.length - offset];
            System.arraycopy(response, offset, tmpBytes, 0, response.length - offset);

            String tmpStr = new String(tmpBytes, StandardCharsets.UTF_8);
            String[] realTimeInfo = tmpStr.split(" ");

            // [0/1/2/3]内存情况 [4/5/6]小中大核频率 [7]CPU总使用率 [8-15]八个核心使用率cpu0-cpu7
            // [16]CPU温度(需除以1000) [17]电流(mA)
            if (realTimeInfo.length < 18) {
                StringBuilder tmp = new StringBuilder("handleMessage: memSplit.length" + realTimeInfo.length);
                for (int i = 0; i < realTimeInfo.length; i++)
                    tmp.append(" [").append(i).append("]").append(realTimeInfo[i]);

                Log.e(TAG, tmp.toString());
                memInfo.setText(tmp);
                return;
            }

            long[] memList = new long[4];
            try {
                for (int i = 0; i < 4; i++)
                    memList[i] = Long.parseLong(realTimeInfo[i]);
            } catch (Exception e) {
                Log.e(TAG, "handleMessage: memList long:" + tmpStr + "\n" + e);
                cpu.setText("tmpStr" + tmpStr);
                return;
            }

            @SuppressLint("DefaultLocale")
            String tmp = String.format("[物理内存] 全部: %.2f GiB\n已用:%.1f%% 剩余: %.2f %s",
                    (memList[0] / Math.pow(1024, 3)), 100.0 * (memList[0] - availMem) / memList[0],
                    availMem > Math.pow(1024, 3) ? (availMem / Math.pow(1024, 3)) : (availMem / Math.pow(1024, 2)),
                    availMem > Math.pow(1024, 3) ? "GiB" : "MiB");
            memInfo.setText(tmp);

            if (memList[2] > 0) { //可能没有 虚拟内存
                tmp = String.format("[虚拟内存] 全部: %.2f GiB\n已用:%.1f%% 剩余: %.2f %s",
                        (memList[2] / Math.pow(1024, 3)), 100.0 * (memList[2] - memList[3]) / memList[2],
                        memList[3] > Math.pow(1024, 3) ? (memList[3] / Math.pow(1024, 3)) : (memList[3] / Math.pow(1024, 2)),
                        memList[3] > Math.pow(1024, 3) ? "GiB" : "MiB");
                zramInfo.setText(tmp);
            }

            // [4/5/6]小中大核频率 [7]CPU总使用率 [8-15]八个核心使用率cpu0-cpu7
            // [16]CPU温度(需除以1000) [17]电流(uA)
            int percent = 0, temperature = 0, mA = 0;

            try {
                percent = Integer.parseInt(realTimeInfo[7]);
                temperature = Integer.parseInt(realTimeInfo[16]);

                mA = Integer.parseInt(realTimeInfo[17].trim());
                mA = (mA == 0) ? 0 : (mA / -1000);

            } catch (Exception e) {
                Log.e(TAG, "fail temperature_mA:[" + realTimeInfo[7] + "] [" + realTimeInfo[16] + "] [" + realTimeInfo[17] + "]");
            }
            cpu.setText(String.format(getString(R.string.realtime_text), percent, temperature / 1000.0));
            battery.setText(Math.abs(mA) > 1000 ? String.format("%.1f A", mA / 1e3) : (mA + " mA"));

            cpu0.setText("cpu0\n" + realTimeInfo[4] + "MHz\n" + realTimeInfo[8] + "%");
            cpu1.setText("cpu1\n" + realTimeInfo[4] + "MHz\n" + realTimeInfo[9] + "%");
            cpu2.setText("cpu2\n" + realTimeInfo[4] + "MHz\n" + realTimeInfo[10] + "%");
            cpu3.setText("cpu3\n" + realTimeInfo[4] + "MHz\n" + realTimeInfo[11] + "%");
            cpu4.setText("cpu4\n" + realTimeInfo[5] + "MHz\n" + realTimeInfo[12] + "%");
            cpu5.setText("cpu5\n" + realTimeInfo[5] + "MHz\n" + realTimeInfo[13] + "%");
            cpu6.setText("cpu6\n" + realTimeInfo[5] + "MHz\n" + realTimeInfo[14] + "%");
            cpu7.setText("cpu7\n" + realTimeInfo[6] + "MHz\n" + realTimeInfo[15] + "%");

        }
    };

    private final Handler statusHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                stateLayout.setBackgroundResource(R.color.warn_red);
                moduleStatus.setText(R.string.freezeit_offline);
                Log.e(TAG, getString(R.string.freezeit_offline));
                memLayout.setVisibility(View.GONE);
                return;
            }

            // info [0]:moduleID [1]:moduleName [2]:moduleVersion [3]:moduleVersionCode [4]:moduleAuthor
            //      [5]:xxx xxx xxx xxx (全部内存 可用内存 全部虚拟内存 可用虚拟内存: bytes)
            String[] info = new String(response, StandardCharsets.UTF_8).split("\n");

            if (info.length < 5 || !info[0].equals("freezeit")) {
                stateLayout.setBackgroundResource(R.color.warn_red);
                moduleStatus.setText(R.string.freezeit_offline);
                Log.e(TAG, getString(R.string.freezeit_offline));
                memLayout.setVisibility(View.GONE);
                return;
            }

            boolean xposedState = isXposedActive();

            moduleIsRunning = true;
            moduleName = info[1];
            moduleVersion = info[2];
            try {
                moduleVersionCode = Integer.parseInt(info[3]);
            } catch (Exception ignored) {
            }

            if (xposedState) stateLayout.setBackgroundResource(R.color.normal_green);
            else stateLayout.setBackgroundResource(R.color.warn_orange);


            StringBuilder statusStr = new StringBuilder();
            statusStr.append(getString(R.string.magisk_online)).append(' ').append(moduleVersion).append('\n');
            if (xposedState)
                statusStr.append(getString(R.string.xposed_online)).append(" v").append(BuildConfig.VERSION_NAME);
            else
                statusStr.append(getString(R.string.xposed_offline));

            moduleStatus.setText(statusStr);

            new Thread(() -> Utils.getData("https://raw.fastgit.org/jark006/freezeitRelease/master/update.json", checkUpdateHandler)).start();
        }
    };


    private final Handler checkUpdateHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0)
                return;

            try {
                JSONObject json = new JSONObject(new String(response, StandardCharsets.UTF_8));
                version = json.getString("version");
                versionCode = json.getInt("versionCode");

                if (versionCode > moduleVersionCode) {
                    updateTips.setText("\uD83D\uDCCC可更新 " + version);
                    changelogTips.setText("更新日志");
                    zipUrl = json.getString("zipUrl");
                    changelogUrl = json.getString("changelog");
                } else if (versionCode == moduleVersionCode) {
                    updateTips.setText("已是最新版");
                } else {
                    updateTips.setText("测试版");
                }
            } catch (JSONException e) {
                updateTips.setText("");
                changelogTips.setText("");
                e.printStackTrace();
            }
        }
    };

    Runnable realTimeTask = () -> {
        am.getMemoryInfo(memoryInfo);
        availMem = memoryInfo.availMem;

        Utils.freezeitTask(Utils.getRealTimeInfo, ("" + (viewHeight / 3) + " " + (viewWidth / 3) + " " + availMem).getBytes(), realTimeHandler);
    };


    private final Handler changelogHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                Snackbar.make(constraintLayout, getString(R.string.freezeit_offline), Snackbar.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(moduleName + " " + moduleVersion)
                    .setIcon(R.drawable.ic_changelog_24dp)
                    .setCancelable(true)
                    .setMessage(new String(response, StandardCharsets.UTF_8));

            AlertDialog dlg = builder.create();
            dlg.show();
        }
    };


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.stateLayout) {
            if (moduleIsRunning)
                new Thread(() -> Utils.freezeitTask(Utils.getChangelog, null, changelogHandler)).start();
            else
                Snackbar.make(constraintLayout, getString(R.string.freezeit_offline), Snackbar.LENGTH_SHORT).show();
        } else if (id == R.id.memLayout) {
            startActivity(new Intent(requireContext(), AppTimeActivity.class));
        } else if (id == R.id.wechatpay) {
            Utils.imgDialog(requireContext(), R.drawable.wechatpay);
        } else if (id == R.id.qqpay) {
            Utils.imgDialog(requireContext(), R.drawable.qqpay);
        } else if (id == R.id.alipay) {
            Utils.imgDialog(requireContext(), R.drawable.alipay);
        } else if (id == R.id.ecnypay) {
            Utils.imgDialog(requireContext(), R.drawable.ecnypay);
        } else if (id == R.id.ethereumpay) {
            Utils.imgDialog(requireContext(), R.drawable.ethereumpay);
        } else if (id == R.id.bitcoinpay) {
            Utils.imgDialog(requireContext(), R.drawable.bitcoinpay);
        } else if (id == R.id.updateTips) {
            if (zipUrl != null && zipUrl.length() > 0)
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(zipUrl)));
        } else if (id == R.id.changelogTips) {
            if (changelogUrl != null && changelogUrl.length() > 0)
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(changelogUrl)));
        } else if (id == R.id.qqgroup_link) {
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
        } else if (id == R.id.qqchannel_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.qq_channel_link))));
        } else if (id == R.id.telegram_link) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tg_link))));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tg_https_link))));
            }
        } else if (id == R.id.telegram_channel_link) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tg_channel_link))));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tg_channel_https_link))));
            }
        } else if (id == R.id.tutorial_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tutorial_link))));
        }
    }

    public boolean isXposedActive() {
        Log.e(TAG, "isXposedActive: Hook Fail");
        return false;
    }

}