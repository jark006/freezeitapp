package io.github.jark006.freezeit.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import java.util.Timer;
import java.util.TimerTask;

import io.github.jark006.freezeit.AppInfoCache;
import io.github.jark006.freezeit.R;
import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.databinding.FragmentLogcatBinding;

public class LogcatFragment extends Fragment {
    private final static String TAG = "LogcatFragment";
    private FragmentLogcatBinding binding;

    Timer timer;

    int lastLogLen = 0;
    long lastTimestamp = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogcatBinding.inflate(inflater, container, false);
        binding.logView.setMovementMethod(ScrollingMovementMethod.getInstance());//流畅滑动

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.logcat_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                var now = System.currentTimeMillis();
                if ((now - lastTimestamp) < 1000) {
                    Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                    return true;
                }
                lastTimestamp = now;

                int id = menuItem.getItemId();
                if (id == R.id.help_log)
                    Utils.layoutDialog(requireContext(), R.layout.help_dialog_logcat);
                else if (id == R.id.update_label) {
                    Toast.makeText(requireContext(), R.string.update_start, Toast.LENGTH_SHORT).show();
                    new Thread(() -> Utils.freezeitTask(Utils.setAppLabel, AppInfoCache.getAppLabelBytes(), appLabelHandler)).start();
                }
                return true;
            }
        }, this.getViewLifecycleOwner());

        binding.fabCheck.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 1000) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            new Thread(() -> Utils.freezeitTask(Utils.printFreezerProc, null, handler)).start();
        });

        binding.fabClear.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 1000) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            new Thread(() -> Utils.freezeitTask(Utils.clearLog, null, handler)).start();
        });

        return binding.getRoot();
    }

    private final Handler appLabelHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                Toast.makeText(requireContext(), R.string.freezeit_offline, Toast.LENGTH_LONG).show();
                return;
            }

            String res = new String(response);
            if (res.equals("success")) {
                Toast.makeText(requireContext(), R.string.update_success, Toast.LENGTH_SHORT).show();
            } else {
                String errorTips = getString(R.string.update_fail) + " Receive:[" + res + "]";
                Toast.makeText(requireContext(), errorTips, Toast.LENGTH_LONG).show();
                Log.e(TAG, errorTips);
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
                Utils.freezeitTask(Utils.getLog, null, handler);
            }
        }, 0, 3000);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0 || binding == null ||
                    lastLogLen == response.length)
                return;

            lastLogLen = response.length;
            binding.logView.setText(new String(response));
            binding.forBottom.requestFocus();//请求焦点，直接到日志底部
            binding.forBottom.clearFocus();
        }
    };
}