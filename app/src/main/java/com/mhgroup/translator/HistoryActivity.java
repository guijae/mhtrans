package com.mhgroup.translator;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private HistoryManager historyManager;
    private List<File> files;
    private TextView tvEmpty;
    private Button btnDeleteAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyManager = new HistoryManager(this);
        recyclerView = findViewById(R.id.rv_history);
        tvEmpty = findViewById(R.id.tv_empty);
        btnDeleteAll = findViewById(R.id.btn_delete_all);

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        btnDeleteAll.setOnClickListener(v -> confirmDeleteAll());

        loadFiles();
    }

    private void loadFiles() {
        files = historyManager.getAllFiles();
        if (files.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnDeleteAll.setEnabled(false);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnDeleteAll.setEnabled(true);
            adapter = new HistoryAdapter(files);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("전체 삭제")
                .setMessage("모든 대화 기록을 삭제합니까?")
                .setPositiveButton("삭제", (d, w) -> {
                    historyManager.deleteAll();
                    loadFiles();
                    Toast.makeText(this, "전체 삭제 완료", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // ── 어댑터 ──
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        List<File> data;

        HistoryAdapter(List<File> data) { this.data = data; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            File file = data.get(position);
            // 파일명에서 날짜 파싱
            String name = file.getName().replace("통역_", "").replace(".txt", "");
            try {
                Date d = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).parse(name);
                String display = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA).format(d);
                holder.tvDate.setText(display);
            } catch (Exception e) {
                holder.tvDate.setText(name);
            }
            holder.tvSize.setText(file.length() / 1024 + "KB");

            // 카카오톡 공유
            holder.btnShare.setOnClickListener(v -> shareKakao(file));

            // 삭제
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("삭제")
                        .setMessage("이 대화 기록을 삭제합니까?")
                        .setPositiveButton("삭제", (d2, w) -> {
                            historyManager.deleteFile(file);
                            data.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, data.size());
                            if (data.isEmpty()) loadFiles();
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });

            // 내용 미리보기 클릭
            holder.itemView.setOnClickListener(v -> {
                String content = historyManager.readFile(file);
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("대화 내용")
                        .setMessage(content.length() > 500 ? content.substring(0, 500) + "..." : content)
                        .setPositiveButton("닫기", null)
                        .setNeutralButton("공유", (d2, w) -> shareKakao(file))
                        .show();
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvSize;
            Button btnShare, btnDelete;

            VH(View v) {
                super(v);
                tvDate   = v.findViewById(R.id.tv_date);
                tvSize   = v.findViewById(R.id.tv_size);
                btnShare = v.findViewById(R.id.btn_share);
                btnDelete = v.findViewById(R.id.btn_delete);
            }
        }
    }

    private void shareKakao(File file) {
        String content = historyManager.readFile(file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.putExtra(Intent.EXTRA_SUBJECT, "MH그룹 AI통역 대화 기록");

        // 카카오톡이 설치된 경우 카카오톡으로, 아니면 공유 메뉴
        try {
            intent.setPackage("com.kakao.talk");
            startActivity(intent);
        } catch (Exception e) {
            // 카카오톡 없으면 일반 공유
            startActivity(Intent.createChooser(intent, "공유하기"));
        }
    }
}
