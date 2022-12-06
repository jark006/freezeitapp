package io.github.jark006.freezeit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.jark006.freezeit.adapter.AppTimeAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class AppTimeActivity extends AppCompatActivity {
    AppTimeAdapter recycleAdapter;
    String[] lines=new String[]{};
    Timer timer;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_time);

        RecyclerView recyclerView = findViewById(R.id.recyclerviewApp);
        recycleAdapter = new AppTimeAdapter(getBaseContext(), lines);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setAdapter(recycleAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Context ctx = this;
        this.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.task_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.help_task) {
                    Utils.imgDialog(ctx, R.drawable.help_task);
                }
                return true;
            }
        });
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
                new Thread(() -> Utils.freezeitTask(Utils.getUidTime, null, handler)).start();
            }
        }, 0, 2000);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0)
                return;

            String[] newLines = new String(response, StandardCharsets.UTF_8).split("\n");

            if (newLines.length != lines.length) {
                recycleAdapter.update(newLines);
                recycleAdapter.notifyItemRangeChanged(0, newLines.length);
            } else {
                recycleAdapter.update(newLines);
                for (int i = 0; i < lines.length; i++) {
                    if (!lines[i].equals(newLines[i]))
                        recycleAdapter.notifyItemChanged(i);
                }
            }
            lines = newLines;
        }
    };

}