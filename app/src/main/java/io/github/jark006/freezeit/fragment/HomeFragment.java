package io.github.jark006.freezeit.fragment;

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
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();

    final int realTimeInfoIntLen = 23;
    int[] realTimeInfo = new int[realTimeInfoIntLen]; //ARM64和X64  Native层均为小端
    byte[] realTimeRequest = new byte[12];
    int realTimeResponseLen = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

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

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            StaticData.hasGetPropInfo = false;
            StaticData.hasOnlineInfo = false;
            StaticData.onlineChangelog = "";
            StaticData.localChangelog = "";
            refreshStatus();
        });

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    final int HAS_MODULE_INFO = 1,
            NO_MODULE_INFO = 2,
            HANDLE_ONLINE_INFO = 4,
            HANDLE_REALTIME_INFO = 5,
            UPDATE_ONLINE_CHANGELOG = 7,
            UPDATE_LOCAL_CHANGELOG = 8;

    void refreshStatus() {
        if (StaticData.hasGetPropInfo) handler.sendEmptyMessage(HAS_MODULE_INFO);
        else new Thread(() -> {
            var recvLen = Utils.freezeitTask(Utils.getPropInfo, null);

            // info [0]:moduleID [1]:moduleName [2]:moduleVersion [3]:moduleVersionCode [4]:moduleAuthor
            //      [5]:clusterNum: CPU丛集数
            //      [6]:moduleEnv:  Magisk or KernelSU
            //      [7]:workMode:   冻结模式 V1 / V2
            //      [8]:androidVer: 安卓版本
            //      [9]:kernelVer:  内核版本
            //      [10]:extMemory: 内存扩展 MiB
            String[] info = (recvLen == 0) ? null : new String(StaticData.response, 0, recvLen).split("\n");
            if (info == null || info.length < 5) {
                handler.sendEmptyMessage(NO_MODULE_INFO);
                return;
            }

            try {
                StaticData.moduleVersionCode = Integer.parseInt(info[3]);
                StaticData.clusterType = info.length > 5 ? Integer.parseInt(info[5]) : 0;
                StaticData.extMemory = info.length > 10 ? Integer.parseInt(info[10]) : 0;
            } catch (Exception ignored) {
            }
            StaticData.moduleVersion = info[2];
            StaticData.moduleEnv = info.length > 6 ? info[6] : "Unknown";
            StaticData.workMode = info.length > 7 ? info[7] : "Unknown";
            StaticData.androidVer = info.length > 8 ? info[8] : "Unknown";
            StaticData.kernelVer = info.length > 9 ? info[9] : "Unknown";
            StaticData.hasGetPropInfo = true;
            handler.sendEmptyMessage(HAS_MODULE_INFO);
        }).start();
    }

    void getOnlineInfoTask() {
        if (StaticData.hasOnlineInfo)
            handler.sendEmptyMessage(HANDLE_ONLINE_INFO);
        else new Thread(() -> {
            var response = Utils.getNetworkData(getString(R.string.update_json_link));
            if (response == null || response.length == 0)
                return;
            try {
                JSONObject json = new JSONObject(new String(response));
                StaticData.onlineVersion = json.getString("version");
                StaticData.onlineVersionCode = json.getInt("versionCode");
                StaticData.zipUrl = json.getString("zipUrl");
                StaticData.changelogUrl = json.getString("changelog");
                StaticData.hasOnlineInfo = true;
                handler.sendEmptyMessage(HANDLE_ONLINE_INFO);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }).start();
    }

    void initRealTimeInfoTimer() {
        if (timer != null || StaticData.imgHeight == 0 || StaticData.imgWidth == 0)
            return;

        Utils.Int2Byte(StaticData.imgHeight, realTimeRequest, 0);
        Utils.Int2Byte(StaticData.imgWidth, realTimeRequest, 4);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                StaticData.am.getMemoryInfo(memoryInfo); // 底层 /proc/meminfo 的 MemAvailable 不可靠
                Utils.Int2Byte((int) (memoryInfo.availMem >> 20), realTimeRequest, 8); //Unit: MiB

                realTimeResponseLen = Utils.freezeitTask(Utils.getRealTimeInfo, realTimeRequest);
                if (realTimeResponseLen > 0)
                    handler.sendEmptyMessage(HANDLE_REALTIME_INFO);
            }
        }, 0, 3000);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (binding == null)
                return;

            // 状态机
            switch (msg.what) {
                case NO_MODULE_INFO: {
                    binding.swipeRefreshLayout.setRefreshing(false);
                    binding.stateLayout.setBackgroundResource(R.color.warn_red);
                    binding.statusText.setText(R.string.freezeit_error_tips);
                    binding.realtimeLayout.setVisibility(View.GONE);
                    binding.freezeitLogo.setVisibility(View.GONE);
                    binding.versionCard.setVisibility(View.GONE);
                    getOnlineInfoTask();
                }
                break;

                case HANDLE_ONLINE_INFO: {
                    if (StaticData.moduleVersionCode == StaticData.onlineVersionCode) {
                        binding.changelogLayout.setVisibility(View.GONE);
                        return;
                    } else if (StaticData.moduleVersionCode < StaticData.onlineVersionCode) {
                        binding.downloadButton.setVisibility(View.VISIBLE);
                        binding.changelogLayout.setVisibility(View.VISIBLE);
                        String tmp = requireContext().getString(StaticData.moduleVersionCode == 0 ?
                                R.string.online_version : R.string.new_version) + " " + StaticData.onlineVersion +
                                " (" + StaticData.onlineVersionCode + ")";
                        binding.versionText.setText(tmp);

                        if (StaticData.onlineChangelog.length() > 0)
                            binding.changelogText.setText(StaticData.onlineChangelog);
                        else if (StaticData.changelogUrl.length() > 0)
                            new Thread(() -> {
                                var response = Utils.getNetworkData(StaticData.changelogUrl);
                                if (response == null || response.length == 0)
                                    return;

                                var split = new String(response).split("###");
                                if (split.length > 1 && split[1].length() > 2) {
                                    StaticData.onlineChangelog = split[1].trim();
                                    this.sendEmptyMessage(UPDATE_ONLINE_CHANGELOG);
                                }
                            }).start();
                    } else {
                        binding.downloadButton.setVisibility(View.GONE);
                        binding.changelogLayout.setVisibility(View.VISIBLE);

                        var sb = new StringBuilder();
                        sb.append(getString(R.string.beta_version)).append(": ").append(StaticData.moduleVersion)
                                .append(" (").append(StaticData.moduleVersionCode).append(")\n");
                        sb.append(getString(R.string.online_version)).append(": ").append(StaticData.onlineVersion)
                                .append(" (").append(StaticData.onlineVersionCode).append(")\n");
                        binding.versionText.setText(sb);

                        if (StaticData.localChangelog.length() > 0)
                            binding.changelogText.setText(StaticData.localChangelog);
                        else new Thread(() -> {
                            var recvLen = Utils.freezeitTask(Utils.getChangelog, null);
                            if (recvLen == 0)
                                return;

                            var split = new String(StaticData.response, 0, recvLen).split("###");
                            if (split.length > 1 && split[1].length() > 2) {
                                StaticData.localChangelog = split[1].trim();
                                this.sendEmptyMessage(UPDATE_LOCAL_CHANGELOG);
                            }
                        }).start();
                    }
                }
                break;

                case UPDATE_ONLINE_CHANGELOG:
                    binding.changelogText.setText(StaticData.onlineChangelog);
                    break;

                case UPDATE_LOCAL_CHANGELOG:
                    binding.changelogText.setText(StaticData.localChangelog);
                    break;

                case HAS_MODULE_INFO: {
                    binding.swipeRefreshLayout.setRefreshing(false);
                    binding.freezeitLogo.setVisibility(View.VISIBLE);
                    binding.realtimeLayout.setVisibility(View.VISIBLE);
                    binding.versionCard.setVisibility(View.VISIBLE);

                    boolean xposedState = isXposedActive();
                    binding.stateLayout.setBackgroundResource(xposedState ? R.color.normal_green : R.color.warn_orange);
                    binding.statusText.setText(xposedState ? StaticData.workMode : "Xposed " + getString(R.string.xposed_warn));

                    binding.moduleEnv.setText(StaticData.moduleEnv);
                    binding.moduleVer.setText(StaticData.moduleVersion + " (" + StaticData.moduleVersionCode + ")");
                    binding.managerVer.setText(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
                    binding.androidVer.setText(StaticData.androidVer);
                    binding.kernelVer.setText(StaticData.kernelVer);

                    final int colorEfficiency = getColor(requireContext(), R.color.efficiency_core);
                    final int colorPerformance = getColor(requireContext(), R.color.performance_core);
                    final int colorPerformanceEnhance = getColor(requireContext(), R.color.performance_e_core);
                    // default:44  4+4
                    switch (StaticData.clusterType) {
                        case 431: // 4+3+1
                            binding.cpu4.setTextColor(colorPerformance);
                            binding.cpu5.setTextColor(colorPerformance);
                            binding.cpu6.setTextColor(colorPerformance);
                            break;
                        case 3221: // 3+2+2+1
                            binding.cpu3.setTextColor(colorPerformance);
                            binding.cpu4.setTextColor(colorPerformance);
                            binding.cpu5.setTextColor(colorPerformanceEnhance);
                            binding.cpu6.setTextColor(colorPerformanceEnhance);
                            break;
                        case 422: // 4+2+2
                            binding.cpu4.setTextColor(colorPerformance);
                            binding.cpu5.setTextColor(colorPerformance);
                            break;
                        case 62: // 6+2
                            binding.cpu4.setTextColor(colorEfficiency);
                            binding.cpu5.setTextColor(colorEfficiency);
                            break;
                        case 8: //全小核，或全核心一致
                            binding.cpu4.setTextColor(colorEfficiency);
                            binding.cpu5.setTextColor(colorEfficiency);
                            binding.cpu6.setTextColor(colorEfficiency);
                            binding.cpu7.setTextColor(colorEfficiency);
                            break;
                    }

                    getOnlineInfoTask();

                    if (StaticData.imgWidth != 0 && StaticData.imgHeight != 0) {
                        initRealTimeInfoTimer();
                        return;
                    }

                    var listener = new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            int width = binding.cpuImg.getWidth() / StaticData.imgScale;
                            int height = binding.cpuImg.getHeight() / StaticData.imgScale;

                            while (width * height > 1024 * 1024) { // RGBA 最多预留 4MiB 用于绘图
                                StaticData.imgScale++;
                                width = binding.cpuImg.getWidth() / StaticData.imgScale;
                                height = binding.cpuImg.getHeight() / StaticData.imgScale;
                            }
                            StaticData.imgWidth = width;
                            StaticData.imgHeight = height;

                            binding.cpuImg.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            initRealTimeInfoTimer();
                        }
                    };
                    binding.cpuImg.getViewTreeObserver().addOnGlobalLayoutListener(listener);
                }
                break;

                case HANDLE_REALTIME_INFO:
                    realTimeHandleFunc();
                    break;
            }
        }
    };


    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    void realTimeHandleFunc() {

        // response[0 ~ imgBuffBytes-1]CPU曲线图像数据, [imgBuffBytes ~ end]是其他实时数据
        int imgBuffBytes = StaticData.imgWidth * StaticData.imgHeight * 4; // ARGB 每像素4字节
        if (realTimeResponseLen <= imgBuffBytes) {
            String errorTips = "imgWidth" + StaticData.imgWidth +
                    " imgHeight" + StaticData.imgHeight +
                    " response.length" + realTimeResponseLen +
                    " imgBuffBytes" + imgBuffBytes;
            binding.cpu.setText(errorTips);
            return;
        }

        if (StaticData.bitmap == null || StaticData.bitmap.getHeight() != StaticData.imgHeight ||
                StaticData.bitmap.getWidth() != StaticData.imgWidth)
            StaticData.bitmap = Bitmap.createBitmap(StaticData.imgWidth, StaticData.imgHeight, Bitmap.Config.ARGB_8888);

        StaticData.bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(StaticData.response, 0, imgBuffBytes));
        binding.cpuImg.setImageBitmap(Utils.resize(StaticData.bitmap, StaticData.imgScale));

        int realTimeInfoByteLen = realTimeResponseLen - imgBuffBytes;
        if (realTimeInfoByteLen != 4 * realTimeInfoIntLen) {
            String errorTips = "Required bytes: " + (4 * realTimeInfoIntLen) + " Received bytes:" + realTimeInfoByteLen;
            binding.cpu.setText(errorTips);
            return;
        }

        // [0]全部物理内存 [1]可用内存 [2]全部虚拟内存 [3]可用虚拟内存  Unit: MiB
        // [4-11]八个核心频率(MHz) [12-19]八个核心使用率(%)
        // [20]CPU总使用率(%) [21]CPU温度(m℃) [22]电池功率(mW)
        Utils.Byte2Int(StaticData.response, imgBuffBytes, realTimeInfoIntLen * 4, realTimeInfo, 0);

        final double GiB = 1024.0;
        int MemTotal = realTimeInfo[0], MemAvailable = realTimeInfo[1];
        int SwapTotal = realTimeInfo[2], SwapFree = realTimeInfo[3];

        @SuppressLint("DefaultLocale")
        String tmp = MemTotal <= 0 ? "" : String.format(getString(R.string.physical_ram_text),
                MemTotal / GiB, 100.0 * (MemTotal - MemAvailable) / MemTotal,
                MemAvailable > GiB ? MemAvailable / GiB : MemAvailable,
                MemAvailable > GiB ? "GiB" : "MiB");
        binding.memInfo.setText(tmp);

        tmp = SwapTotal <= 0 ? "" : String.format(getString(R.string.virtual_ram_text),
                SwapTotal / GiB, 100.0 * (SwapTotal - SwapFree) / SwapTotal,
                SwapFree > GiB ? SwapFree / GiB : SwapFree,
                SwapFree > GiB ? "GiB" : "MiB");
        if (StaticData.extMemory > 0)
            tmp += "\n" + String.format(getString(R.string.ext_memory), StaticData.extMemory / 1024.0);
        binding.zramInfo.setText(tmp);

        final int percent = realTimeInfo[20];
        final double temperature = realTimeInfo[21] / 1e3; // m℃ -> ℃
        final int mW = realTimeInfo[22]; // mW 毫瓦
        binding.cpu.setText(String.format(getString(R.string.cpu_format), percent, temperature));
        binding.battery.setText(String.format("%.2f W\uD83D\uDD0B", mW / 1e3));

        binding.cpu0.setText("cpu0\n" + realTimeInfo[4] + "MHz\n" + realTimeInfo[12] + "%");
        binding.cpu1.setText("cpu1\n" + realTimeInfo[5] + "MHz\n" + realTimeInfo[13] + "%");
        binding.cpu2.setText("cpu2\n" + realTimeInfo[6] + "MHz\n" + realTimeInfo[14] + "%");
        binding.cpu3.setText("cpu3\n" + realTimeInfo[7] + "MHz\n" + realTimeInfo[15] + "%");
        binding.cpu4.setText("cpu4\n" + realTimeInfo[8] + "MHz\n" + realTimeInfo[16] + "%");
        binding.cpu5.setText("cpu5\n" + realTimeInfo[9] + "MHz\n" + realTimeInfo[17] + "%");
        binding.cpu6.setText("cpu6\n" + realTimeInfo[10] + "MHz\n" + realTimeInfo[18] + "%");
        binding.cpu7.setText("cpu7\n" + realTimeInfo[11] + "MHz\n" + realTimeInfo[19] + "%");
    }

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