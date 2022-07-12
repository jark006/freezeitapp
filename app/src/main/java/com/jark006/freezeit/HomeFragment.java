package com.jark006.freezeit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.jark006.freezeit.databinding.FragmentHomeBinding;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
//                      佛祖坐镇 尔等bug小怪速速离去

public class HomeFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    TextView moduleInfo, moduleState;
    ConstraintLayout constraintLayout;
    LinearLayout stateLayout, coolApkLink, qqGroupLink, qqChannelLink, tgLink, tgChannelLink,
            githubLink, githubappLink, lanzouLink;
    ImageView wechatPay, aliPay, qqpay, ecnyPay, ethereumPay, bitcoinPay;
    boolean moduleIsRunning = false;
    String moduleName;
    String moduleVersion;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        constraintLayout = binding.constraintLayoutHome;
        moduleInfo = binding.infoText;
        moduleState = binding.stateText;
        stateLayout = binding.stateLayout;

        wechatPay = binding.wechatpay;
        aliPay = binding.alipay;
        qqpay = binding.qqpay;
        ecnyPay = binding.ecnypay;
        ethereumPay = binding.ethereumpay;
        bitcoinPay = binding.bitcoinpay;

        coolApkLink = binding.coolapkLink;
        qqGroupLink = binding.qqgroupLink;
        qqChannelLink = binding.qqchannelLink;
        tgLink = binding.telegramLink;
        tgChannelLink = binding.telegramChannelLink;
        githubLink = binding.githubLink;
        githubappLink = binding.githubappLink;
        lanzouLink = binding.lanzouLink;

        stateLayout.setOnClickListener(this);

        wechatPay.setOnClickListener(this);
        aliPay.setOnClickListener(this);
        qqpay.setOnClickListener(this);
        ecnyPay.setOnClickListener(this);
        ethereumPay.setOnClickListener(this);
        bitcoinPay.setOnClickListener(this);

        coolApkLink.setOnClickListener(this);
        qqGroupLink.setOnClickListener(this);
        qqChannelLink.setOnClickListener(this);
        tgLink.setOnClickListener(this);
        tgChannelLink.setOnClickListener(this);
        githubLink.setOnClickListener(this);
        githubappLink.setOnClickListener(this);
        lanzouLink.setOnClickListener(this);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.home_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.update_app_name) {
                    Snackbar.make(constraintLayout, getString(R.string.update_start), Snackbar.LENGTH_SHORT).show();
                    new Thread(updateAppNameTask).start();
                } else if (id == R.id.about) {
                    aboutDialog();
                }
                return false;
            }
        }, this.getViewLifecycleOwner());

        new Thread(statusTask).start();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private final Handler appNameHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                String errorTips = getString(R.string.freezeit_offline);
                Snackbar.make(constraintLayout, errorTips, Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, errorTips);
                return;
            }

            String res = new String(response, StandardCharsets.UTF_8);
            if (res.equals("success")) {
                Snackbar.make(constraintLayout, R.string.update_seccess, Snackbar.LENGTH_SHORT).show();
            } else {
                String errorTips = getString(R.string.update_fail) + " Receive:[" + res + "]";
                Snackbar.make(constraintLayout, errorTips, Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, errorTips);
            }
        }
    };

    Runnable updateAppNameTask = () -> {
        StringBuilder appName = new StringBuilder();

        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> applicationsInfo = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : applicationsInfo) {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)
                continue;

            String label = pm.getApplicationLabel(appInfo).toString();
            if (label.endsWith("Application") || label.endsWith(".xml") || label.endsWith("false"))
                label = appInfo.packageName;

            appName.append(appInfo.packageName).append("####").append(label).append('\n');
        }
        Utils.freezeitTask(Utils.setAppName, appName.toString().getBytes(StandardCharsets.UTF_8), appNameHandler);
    };

    public void aboutDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.about);
        dialog.show();
    }


    private final Handler statusHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                stateLayout.setBackgroundResource(R.color.warn_red);
                moduleInfo.setText(R.string.freezeit_offline);
                moduleState.setText(R.string.freezeit_offline_tips);
                Log.e(TAG, getString(R.string.freezeit_offline));
                return;
            }

            // info [0]:moduleID [1]:moduleName [2]:moduleVersion [3]:moduleVersionCode [4]:moduleAuthor
            String[] info = new String(response, StandardCharsets.UTF_8).split("\n");

            if (info.length < 5 || !info[0].equals("freezeit")) {
                stateLayout.setBackgroundResource(R.color.warn_red);
                moduleInfo.setText(R.string.freezeit_offline);
                moduleState.setText(R.string.freezeit_offline_tips);
                Log.e(TAG, getString(R.string.freezeit_offline));
                return;
            }

            moduleIsRunning = true;
            moduleName = info[1];
            moduleVersion = info[2];

            stateLayout.setBackgroundResource(R.color.normal_green);
            moduleInfo.setText(R.string.freezeit_online);
            moduleState.setText(getString(R.string.freezeit_mod_name)+": "+moduleVersion + "\n"+
                    getString(R.string.freezeit_app_name)+": v"+BuildConfig.VERSION_NAME);

        }
    };

    Runnable statusTask = () -> Utils.freezeitTask(Utils.getInfo, null, statusHandler);


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

    Runnable changelogTask = () -> Utils.freezeitTask(Utils.getChangelog, null, changelogHandler);


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.stateLayout && moduleIsRunning) {
            new Thread(changelogTask).start();
        } else if (id == R.id.wechatpay) {
            donateDialog(R.layout.wechatpay);
        } else if (id == R.id.qqpay) {
            donateDialog(R.layout.qqpay);
        } else if (id == R.id.alipay) {
            donateDialog(R.layout.alipay);
        } else if (id == R.id.ecnypay) {
            donateDialog(R.layout.ecnypay);
        } else if (id == R.id.ethereumpay) {
            donateDialog(R.layout.ethereumpay);
        } else if (id == R.id.bitcoinpay) {
            donateDialog(R.layout.bitcoinpay);
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
        } else if (id == R.id.qqgroup_link) {
            try {
                //【冻它模块 freezeit】(781222669) 的 key 为： ntLAwm7WxB0hVcetV7DsxfNTVN16cGUD
                String key = "ntLAwm7WxB0hVcetV7DsxfNTVN16cGUD";
                Intent intent = new Intent();
                intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.qqgroup_link))));
            }
        } else if (id == R.id.qqchannel_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.qqchannel_link))));
        } else if (id == R.id.telegram_link) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tg_link))));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tg_https_link))));
            }
        } else if (id == R.id.telegram_channel_link) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tgchannel_link))));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tgchannel_https_link))));
            }
        } else if (id == R.id.github_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link))));
        } else if (id == R.id.githubapp_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.githubapp_link))));
        } else if (id == R.id.lanzou_link) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.lanzou_link))));
        }
    }

    public void donateDialog(int ID) {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(ID);
        dialog.show();
    }

}