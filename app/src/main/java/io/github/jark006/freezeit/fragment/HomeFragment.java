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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import io.github.jark006.freezeit.AboutActivity;
import io.github.jark006.freezeit.AppTimeActivity;
import io.github.jark006.freezeit.BuildConfig;
import io.github.jark006.freezeit.R;
import io.github.jark006.freezeit.SettingsActivity;
import io.github.jark006.freezeit.StaticData;
import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment implements View.OnClickListener {
    private final static String TAG = "HomeFragment";
    private FragmentHomeBinding binding;

    Timer timer;
    long availMem = 0;
    ActivityManager am;
    ActivityManager.MemoryInfo memoryInfo;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        am = (ActivityManager) requireActivity().getSystemService(ACTIVITY_SERVICE);
        memoryInfo = new ActivityManager.MemoryInfo();

        binding.downloadButton.setOnClickListener(this);
        binding.realtimeLayout.setOnClickListener(this);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.home_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.settings) {
                    if (StaticData.hasGetPropInfo)
                        startActivity(new Intent(requireContext(), SettingsActivity.class));
                    else
                        Toast.makeText(requireContext(), getString(R.string.freezeit_offline), Toast.LENGTH_LONG).show();
                } else if (id == R.id.about) {
                    startActivity(new Intent(requireContext(), AboutActivity.class));
                }
                return true;
            }
        }, this.getViewLifecycleOwner());

        if (StaticData.imgWidth == 0 || StaticData.imgHeight == 0) {
            binding.cpuImg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    StaticData.imgWidth = binding.cpuImg.getWidth() / StaticData.imgScale;
                    StaticData.imgHeight = binding.cpuImg.getHeight() / StaticData.imgScale;
                    binding.cpuImg.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    new Thread(realTimeTask).start();
                }
            });
        } else {
            new Thread(realTimeTask).start();
        }

        if (StaticData.hasGetUpdateInfo) checkUpdateHandler.sendMessage(new Message());
        else
            new Thread(() -> Utils.getData(getString(R.string.update_json_link), checkUpdateHandler)).start();

        if (StaticData.hasGetPropInfo) statusHandler.sendMessage(new Message());
        else new Thread(() -> Utils.freezeitTask(Utils.getPropInfo, null, statusHandler)).start();

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
                if (StaticData.imgHeight == 0 || StaticData.imgWidth == 0)
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

    Handler realTimeHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            var response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                binding.realtimeLayout.setVisibility(View.GONE);
                return;
            }

            if (StaticData.imgHeight == 0 || StaticData.imgWidth == 0)
                return;

            // response[0 ~ imgBuffBytes-1]CPU曲线图像数据, [imgBuffBytes ~ end]是其他实时数据
            int imgBuffBytes = StaticData.imgWidth * StaticData.imgHeight * 4; // ARGB 每像素4字节
            if (response.length < imgBuffBytes) {
                String errorTips = "handleMessage: imgWidth" + StaticData.imgWidth + " imgHeight" +
                        StaticData.imgHeight + " response.length" + response.length + " imgBuffBytes" + imgBuffBytes;
                Log.e(TAG, errorTips);
                binding.memInfo.setText(errorTips);
                return;
            }

            var bitmap = Bitmap.createBitmap(StaticData.imgWidth, StaticData.imgHeight, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(response, 0, imgBuffBytes));
            bitmap = Utils.resize(bitmap, StaticData.imgScale, StaticData.imgScale);
            binding.cpuImg.setImageBitmap(bitmap);

            int extendInfoLen = response.length - imgBuffBytes;
            if (extendInfoLen <= 0)
                return;

            var realTimeInfo = new String(response, imgBuffBytes, extendInfoLen).split(" ");

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
            String tmp = String.format(getString(R.string.physical_ram_text),
                    (memList[0] / Math.pow(1024, 3)), 100.0 * (memList[0] - availMem) / memList[0],
                    availMem > Math.pow(1024, 3) ? (availMem / Math.pow(1024, 3)) : (availMem / Math.pow(1024, 2)),
                    availMem > Math.pow(1024, 3) ? "GiB" : "MiB");
            binding.memInfo.setText(tmp);

            if (memList[2] > 0) { //可能没有 虚拟内存
                tmp = String.format(getString(R.string.virtual_ram_text),
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

    Runnable realTimeTask = () -> {
        am.getMemoryInfo(memoryInfo);
        availMem = memoryInfo.availMem;

        byte[] payload = ("" + StaticData.imgHeight + " " + StaticData.imgWidth + " " + availMem).getBytes();
        Utils.freezeitTask(Utils.getRealTimeInfo, payload, realTimeHandler);
    };

    private final Handler statusHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (!StaticData.hasGetPropInfo) {
                byte[] response = msg.getData().getByteArray("response");

                // info [0]:moduleID [1]:moduleName [2]:moduleVersion [3]:moduleVersionCode [4]:moduleAuthor
                //      [5]:clusterNum: CPU丛集数
                //      [6]:moduleEnv:  Magisk or KernelSU
                String[] info = (response == null || response.length == 0) ? null :
                        new String(response, StandardCharsets.UTF_8).split("\n");

                if (info == null || info.length < 5) {
                    binding.stateLayout.setBackgroundResource(R.color.warn_red);
                    binding.statusText.setText(R.string.freezeit_error_tips);
                    binding.realtimeLayout.setVisibility(View.GONE);
                    return;
                }

                try {
                    StaticData.moduleVersionCode = Integer.parseInt(info[3]);
                    StaticData.clusterNum = info.length >= 6 ? Integer.parseInt(info[5]) : 0;
                } catch (Exception ignored) {
                }
                StaticData.moduleVersion = info[2];
                StaticData.moduleEnv = info.length >= 7 ? info[6] : "Unknown";
                StaticData.hasGetPropInfo = true;
            }

            switch (StaticData.clusterNum) {
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

            boolean xposedState = isXposedActive();
            if (xposedState) binding.stateLayout.setBackgroundResource(R.color.normal_green);
            else binding.stateLayout.setBackgroundResource(R.color.warn_orange);

            StringBuilder statusStr = new StringBuilder();
            if (StaticData.moduleEnv.equals("Magisk")) statusStr.append("Magisk   ");
            else if (StaticData.moduleEnv.equals("KernelSU")) statusStr.append("KernelSU ");
            else statusStr.append("Unknown  ");

            statusStr.append(StaticData.moduleVersion).append('\n');
            statusStr.append("LSPosed  ").append(BuildConfig.VERSION_NAME);
            if (!xposedState)
                statusStr.append(' ').append(getString(R.string.xposed_warn));

            binding.statusText.setText(statusStr);
        }
    };


    private final Handler checkUpdateHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (!StaticData.hasGetUpdateInfo) {
                byte[] response = msg.getData().getByteArray("response");
                if (response == null || response.length == 0)
                    return;
                try {
                    JSONObject json = new JSONObject(new String(response, StandardCharsets.UTF_8));
                    StaticData.onlineVersion = json.getString("version");
                    StaticData.onlineVersionCode = json.getInt("versionCode");
                    StaticData.zipUrl = json.getString("zipUrl");
                    StaticData.changelogUrl = json.getString("changelog");
                    StaticData.hasGetUpdateInfo = true;
                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                }
            }

            if (binding == null)
                return;

            if (StaticData.onlineVersionCode > StaticData.moduleVersionCode) {
                binding.changelogLayout.setVisibility(View.VISIBLE);
                String tmp = (StaticData.moduleVersionCode == 0 ?
                        requireContext().getString(R.string.online_version) :
                        requireContext().getString(R.string.new_version))
                        + " " + StaticData.onlineVersion;
                binding.versionText.setText(tmp);

                if (StaticData.onlineChangelog.length() > 0)
                    binding.changelogText.setText(StaticData.onlineChangelog);
                else if (StaticData.changelogUrl.length() > 0)
                    new Thread(() -> Utils.getData(StaticData.changelogUrl, onlineChangelogHandler)).start();

            } else if (StaticData.onlineVersionCode < StaticData.moduleVersionCode) {
                binding.downloadButton.setVisibility(View.GONE);
                binding.changelogLayout.setVisibility(View.VISIBLE);

                var sb = new StringBuilder();
                sb.append(getString(R.string.beta_version)).append(": ").append(StaticData.moduleVersion)
                        .append('(').append(StaticData.moduleVersionCode).append(")\n");
                sb.append(getString(R.string.online_version)).append(": ").append(StaticData.onlineVersion)
                        .append('(').append(StaticData.onlineVersionCode).append(")\n");
                binding.versionText.setText(sb);

                if (StaticData.localChangelog.length() > 0)
                    binding.changelogText.setText(StaticData.localChangelog);
                else
                    new Thread(() -> Utils.freezeitTask(Utils.getChangelog, null, localChangelogHandler)).start();
            }
        }
    };

    private final Handler onlineChangelogHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0)
                return;

            var split = new String(response, StandardCharsets.UTF_8).split("###");
            if (split.length > 2 && split[1].length() > 2)
                StaticData.onlineChangelog = split[1];

            if (binding != null)
                binding.changelogText.setText(StaticData.onlineChangelog);
        }
    };

    private final Handler localChangelogHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0)
                return;

            var split = new String(response, StandardCharsets.UTF_8).split("###");
            if (split.length > 2 && split[1].length() > 2)
                StaticData.localChangelog = split[1];

            if (binding != null)
                binding.changelogText.setText(StaticData.localChangelog);
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.realtimeLayout) {
            startActivity(new Intent(requireContext(), AppTimeActivity.class));
        } else if (id == R.id.download_button) {
            if (StaticData.zipUrl.length() > 2)
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticData.zipUrl)));
        }
    }

    public boolean isXposedActive() {
        Log.e(TAG, "isXposedActive: Hook Fail");
        return false;
    }

}