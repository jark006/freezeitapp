package io.github.jark006.freezeit.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
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
import io.github.jark006.freezeit.StaticData;
import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.databinding.FragmentLogcatBinding;

public class LogcatFragment extends Fragment {
    private FragmentLogcatBinding binding;
    final int NEW_LOG_CONTENT = 1,
            UPDATE_LABEL_SUCCESS = 2,
            UPDATE_LABEL_FAIL = 3;

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
                    new Thread(() -> {
                        var recvLen = Utils.freezeitTask(Utils.setAppLabel, AppInfoCache.getAppLabelBytes());

                        handler.sendEmptyMessage((recvLen == 7 &&
                                new String(StaticData.response, 0, 7).equals("success")) ?
                                UPDATE_LABEL_SUCCESS : UPDATE_LABEL_FAIL);
                    }).start();
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

            new Thread(() -> logTask(Utils.printFreezerProc)).start();
        });

        binding.fabClear.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 1000) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            new Thread(() -> logTask(Utils.clearLog)).start();
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logTask(Utils.getLog);
            }
        }, 0, 3000);
    }

    void logTask(byte cmd) {
        var recvLen = Utils.freezeitTask(cmd, null);
        if (recvLen == 0 || recvLen == lastLogLen)
            return;
        lastLogLen = recvLen;
        handler.sendEmptyMessage(NEW_LOG_CONTENT);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (binding == null)
                return;

            switch (msg.what) {
                case NEW_LOG_CONTENT:
                    binding.logView.setText(new String(StaticData.response, 0, lastLogLen));
                    binding.forBottom.requestFocus();//请求焦点，直接到日志底部
                    binding.forBottom.clearFocus();
                    break;

                case UPDATE_LABEL_SUCCESS:
                    Toast.makeText(requireContext(), R.string.update_success, Toast.LENGTH_SHORT).show();
                    break;

                case UPDATE_LABEL_FAIL:
                    Toast.makeText(requireContext(), R.string.update_fail, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}