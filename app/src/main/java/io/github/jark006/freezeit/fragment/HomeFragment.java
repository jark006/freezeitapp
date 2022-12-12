package io.github.jark006.freezeit.fragment;

import static android.content.Context.ACTIVITY_SERVICE;
import static androidx.core.content.ContextCompat.getColor;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import io.github.jark006.freezeit.AppTimeActivity;
import io.github.jark006.freezeit.BuildConfig;
import io.github.jark006.freezeit.R;
import io.github.jark006.freezeit.SettingsActivity;
import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment implements View.OnClickListener {
    private final static String TAG = "HomeFragment";
    private FragmentHomeBinding binding;

    TextView updateTips;
    TextView changelogTips;

    boolean moduleIsRunning = false;
    String moduleName;
    String moduleVersion;
    int moduleVersionCode = 0;

    String zipUrl = null;
    String changelogUrl = null;

    Timer timer;

    final int imgScale = 2;
    int imgWidth = 0; //图像宽高 缩小为控件的 1/imgScale, 减少绘图花销，显示到imageView控件时再放大 imgScale 倍
    int imgHeight = 0;
    long availMem = 0;

    ActivityManager am;
    ActivityManager.MemoryInfo memoryInfo;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        am = (ActivityManager) requireActivity().getSystemService(ACTIVITY_SERVICE);
        memoryInfo = new ActivityManager.MemoryInfo();

        updateTips = binding.updateTips;
        changelogTips = binding.changelogTips;

        updateTips.setOnClickListener(this);
        changelogTips.setOnClickListener(this);

        binding.stateLayout.setOnClickListener(this);
        binding.realtimeLayout.setOnClickListener(this);

        binding.wechatpay.setOnClickListener(this);
        binding.alipay.setOnClickListener(this);
        binding.qqpay.setOnClickListener(this);
        binding.ecnypay.setOnClickListener(this);
        binding.ethereumpay.setOnClickListener(this);
        binding.bitcoinpay.setOnClickListener(this);

        binding.qqgroupLink.setOnClickListener(this);
        binding.qqchannelLink.setOnClickListener(this);
        binding.telegramLink.setOnClickListener(this);
        binding.telegramChannelLink.setOnClickListener(this);
        binding.tutorialLink.setOnClickListener(this);

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
                        Toast.makeText(requireContext(), getString(R.string.freezeit_offline), Toast.LENGTH_LONG).show();
                }
                return false;
            }
        }, this.getViewLifecycleOwner());

        binding.cpuImg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imgWidth = binding.cpuImg.getWidth() / imgScale;
                imgHeight = binding.cpuImg.getHeight() / imgScale;
                binding.cpuImg.getViewTreeObserver().removeOnGlobalLayoutListener(this);
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
                if (imgHeight == 0 || imgWidth == 0)
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
            var response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                binding.realtimeLayout.setVisibility(View.GONE);
                return;
            }

            if (imgHeight == 0 || imgWidth == 0)
                return;

            // response[0 ~ imgBuffBytes-1]CPU曲线图像数据, [imgBuffBytes ~ end]是其他实时数据
            int imgBuffBytes = imgWidth * imgHeight * 4; // ARGB 每像素4字节
            if (response.length <= imgBuffBytes) {
                String errorTips = "handleMessage: imgWidth" + imgWidth + " imgHeight" +
                        imgHeight + " response.length" + response.length + " imgBuffBytes" + imgBuffBytes;
                Log.e(TAG, errorTips);
                binding.memInfo.setText(errorTips);
                return;
            }

            var bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(response, 0, imgBuffBytes));
            bitmap = Utils.resize(bitmap, imgScale, imgScale);
            binding.cpuImg.setImageBitmap(bitmap);

            var realTimeInfo = new String(response, imgBuffBytes, response.length - imgBuffBytes).split(" ");

            // [0]全部物理内存 [1]可用物理内存 [2]全部虚拟内存 [3]可用虚拟内存  bytes
            // [4-11]八个核心频率 [12-19]八个核心使用率
            // [20]CPU总使用率 [21]CPU温度(需除以1000) [22]电流(mA)
            if (realTimeInfo.length < 23) {
                var tmp = new StringBuilder("handleMessage: memSplit.length" + realTimeInfo.length);
                for (int i = 0; i < realTimeInfo.length; i++)
                    tmp.append(" [").append(i).append("]").append(realTimeInfo[i]);

                Log.e(TAG, tmp.toString());
                binding.memInfo.setText(tmp);
                return;
            }

            long[] memList = new long[4];
            try {
                for (int i = 0; i < 4; i++)
                    memList[i] = Long.parseLong(realTimeInfo[i]);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder("handleMessage: memList");
                for (int i = 0; i < 4; i++)
                    sb.append('[').append(realTimeInfo[i]).append(']');
                Log.e(TAG, sb.toString());
                binding.memInfo.setText(sb.toString());
                return;
            }

            @SuppressLint("DefaultLocale")
            String tmp = String.format("[物理内存] 全部: %.2f GiB\n已用:%.1f%% 剩余: %.2f %s",
                    (memList[0] / Math.pow(1024, 3)), 100.0 * (memList[0] - availMem) / memList[0],
                    availMem > Math.pow(1024, 3) ? (availMem / Math.pow(1024, 3)) : (availMem / Math.pow(1024, 2)),
                    availMem > Math.pow(1024, 3) ? "GiB" : "MiB");
            binding.memInfo.setText(tmp);

            if (memList[2] > 0) { //可能没有 虚拟内存
                tmp = String.format("[虚拟内存] 全部: %.2f GiB\n已用:%.1f%% 剩余: %.2f %s",
                        (memList[2] / Math.pow(1024, 3)), 100.0 * (memList[2] - memList[3]) / memList[2],
                        memList[3] > Math.pow(1024, 3) ? (memList[3] / Math.pow(1024, 3)) : (memList[3] / Math.pow(1024, 2)),
                        memList[3] > Math.pow(1024, 3) ? "GiB" : "MiB");
                binding.zramInfo.setText(tmp);
            }

            // [4-11]八个核心频率 [12-19]八个核心使用率
            // [20]CPU总使用率 [21]CPU温度(需除以1000) [22]电流(mA)
            int percent = 0, temperature = 0, mA = 0;

            try {
                percent = Integer.parseInt(realTimeInfo[20]);
                temperature = Integer.parseInt(realTimeInfo[21]);
                mA = Integer.parseInt(realTimeInfo[22]);
                mA = (mA == 0) ? 0 : (mA / -1000);
            } catch (Exception e) {
                Log.e(TAG, "fail percent:[" + realTimeInfo[20] + "] temperature[" + realTimeInfo[21] + "] mA[" + realTimeInfo[22] + "]");
            }

            binding.cpu.setText(String.format(getString(R.string.cpu_format), percent, temperature / 1000.0));
            binding.battery.setText(Math.abs(mA) > 2000 ? String.format("%.2f A", mA / 1e3) : (mA + " mA"));

            binding.cpu0.setText("cpu0\n" + realTimeInfo[4] + "MHz\n" + realTimeInfo[12] + "%");
            binding.cpu1.setText("cpu1\n" + realTimeInfo[5] + "MHz\n" + realTimeInfo[13] + "%");
            binding.cpu2.setText("cpu2\n" + realTimeInfo[6] + "MHz\n" + realTimeInfo[14] + "%");
            binding.cpu3.setText("cpu3\n" + realTimeInfo[7] + "MHz\n" + realTimeInfo[15] + "%");
            binding.cpu4.setText("cpu4\n" + realTimeInfo[8] + "MHz\n" + realTimeInfo[16] + "%");
            binding.cpu5.setText("cpu5\n" + realTimeInfo[9] + "MHz\n" + realTimeInfo[17] + "%");
            binding.cpu6.setText("cpu6\n" + realTimeInfo[10] + "MHz\n" + realTimeInfo[18] + "%");
            binding.cpu7.setText("cpu7\n" + realTimeInfo[11] + "MHz\n" + realTimeInfo[19] + "%");
        }
    };

    private final Handler statusHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                binding.stateLayout.setBackgroundResource(R.color.warn_red);
                binding.statusText.setText(R.string.freezeit_offline);
                Log.e(TAG, getString(R.string.freezeit_offline));
                binding.realtimeLayout.setVisibility(View.GONE);
                return;
            }

            // info [0]:moduleID [1]:moduleName [2]:moduleVersion [3]:moduleVersionCode [4]:moduleAuthor
            //      [5]:clusterNum
            String[] info = new String(response, StandardCharsets.UTF_8).split("\n");

            if (info.length < 6 || !info[0].equals("freezeit")) {
                binding.stateLayout.setBackgroundResource(R.color.warn_red);
                binding.statusText.setText(R.string.freezeit_offline);
                Log.e(TAG, getString(R.string.freezeit_offline));
                binding.realtimeLayout.setVisibility(View.GONE);
                return;
            }

            moduleIsRunning = true;
            moduleName = info[1];
            moduleVersion = info[2];
            try {
                moduleVersionCode = Integer.parseInt(info[3]);

                int clusterNum = Integer.parseInt(info[5]);
                switch (clusterNum) {
                    case 3: // 4+3+1
                        binding.cpu4.setTextColor(getColor(requireContext(), R.color.cpu_mid));
                        binding.cpu5.setTextColor(getColor(requireContext(), R.color.cpu_mid));
                        binding.cpu6.setTextColor(getColor(requireContext(), R.color.cpu_mid));
                        break;
                    case 4: // 3+2+2+1
                        binding.cpu3.setTextColor(getColor(requireContext(), R.color.cpu_mid));
                        binding.cpu4.setTextColor(getColor(requireContext(), R.color.cpu_mid));
                        binding.cpu5.setTextColor(getColor(requireContext(), R.color.cpu_mid_plus));
                        binding.cpu6.setTextColor(getColor(requireContext(), R.color.cpu_mid_plus));
                        break;
                }
            } catch (Exception ignored) {
            }

            boolean xposedState = isXposedActive();
            if (xposedState) binding.stateLayout.setBackgroundResource(R.color.normal_green);
            else binding.stateLayout.setBackgroundResource(R.color.warn_orange);

            StringBuilder statusStr = new StringBuilder();
            statusStr.append(getString(R.string.magisk_online)).append(' ').append(moduleVersion).append('\n');
            if (xposedState)
                statusStr.append(getString(R.string.xposed_online)).append(" v").append(BuildConfig.VERSION_NAME);
            else
                statusStr.append(getString(R.string.xposed_offline)).append(" v").append(BuildConfig.VERSION_NAME)
                        .append('\n').append(getString(R.string.xposed_warm));

            binding.statusText.setText(statusStr);

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
                String version = json.getString("version");
                int versionCode = json.getInt("versionCode");

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
                Log.e(TAG, e.toString());
            }
        }
    };

    Runnable realTimeTask = () -> {
        am.getMemoryInfo(memoryInfo);
        availMem = memoryInfo.availMem;

        Utils.freezeitTask(Utils.getRealTimeInfo, ("" + imgHeight + " " + imgWidth + " " + availMem).getBytes(), realTimeHandler);
    };


    private final Handler changelogHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                Toast.makeText(requireContext(), getString(R.string.freezeit_offline), Toast.LENGTH_LONG).show();
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
                Toast.makeText(requireContext(), getString(R.string.freezeit_offline), Toast.LENGTH_LONG).show();
        } else if (id == R.id.realtimeLayout) {
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