package com.mhgroup.translator;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryManager {

    private final Context context;
    private final File historyDir;
    private static final String DIR_NAME = "MH그룹통역기록";

    public HistoryManager(Context context) {
        this.context = context;

        File baseDir;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10 이상은 앱 전용 외부 저장소 권장 (권한 문제 방지)
            baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        } else {
            baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        }

        if (baseDir == null) baseDir = context.getFilesDir();
        
        historyDir = new File(baseDir, DIR_NAME);
        if (!historyDir.exists()) historyDir.mkdirs();
    }

    public File getHistoryDir() {
        return historyDir;
    }

    public boolean saveSession(String content) {
        if (content == null || content.trim().isEmpty()) return false;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date());
        String filename = "통역_" + timestamp + ".txt";
        File file = new File(historyDir, filename);

        try (FileWriter writer = new FileWriter(file)) {
            String header = "=== MH그룹 AI통역 대화 기록 ===\n"
                    + "날짜: " + new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss", Locale.KOREA).format(new Date())
                    + "\n================================\n\n";
            writer.write(header);
            writer.write(content);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public List<File> getAllFiles() {
        List<File> files = new ArrayList<>();
        File[] arr = historyDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (arr != null) {
            Arrays.sort(arr, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            files.addAll(Arrays.asList(arr));
        }
        return files;
    }

    public boolean deleteFile(File file) {
        return file != null && file.exists() && file.delete();
    }

    public boolean deleteAll() {
        File[] files = historyDir.listFiles();
        if (files == null) return true;
        boolean allDeleted = true;
        for (File f : files) {
            if (!f.delete()) allDeleted = false;
        }
        return allDeleted;
    }

    public String readFile(File file) {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }
}
