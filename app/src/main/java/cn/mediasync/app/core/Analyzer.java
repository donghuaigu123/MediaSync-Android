package cn.mediasync.app.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 媒体链接解析：本地直链直接识别，平台链接转发给自托管服务。 */
public final class Analyzer {
    private Analyzer() { }

    public static class Format {
        public String label;
        public String url;
        public String ext;
    }

    public static class Result {
        public String url = "";
        public String title = "";
        public String platform = "通用媒体";
        public String note = "";
        public String cover = "";
        public boolean direct = false;
        public final List<Format> formats = new ArrayList<>();
    }

    public interface Callback {
        void ok(Result r);
        void fail(String msg);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);

    public static void analyze(Context ctx, String raw, Callback cb) {
        final Context c = ctx.getApplicationContext();
        final String link = Platforms.normalize(raw);
        if (link.isEmpty()) { cb.fail("请输入有效链接"); return; }

        POOL.execute(() -> {
            Result r = new Result();
            r.url = link;
            r.platform = Platforms.detect(link);
            r.title = Platforms.fileName(link);
            Settings cfg = new Settings(c);
            try {
                if (Platforms.isDirect(link)) {
                    r.direct = true;
                    Format f = new Format();
                    f.label = "原始画质 · " + Platforms.ext(link, null).replace(".", "").toUpperCase();
                    f.url = link;
                    f.ext = Platforms.ext(link, null);
                    r.formats.add(f);
                    r.note = "识别为通用媒体直链，可直接在本地下载（支持断点续传）。";
                } else if (!cfg.server().isEmpty()) {
                    String api = cfg.server() + cfg.parseApi()
                            + "?url=" + URLEncoder.encode(link, "UTF-8");
                    String body = httpGet(api, 25000);
                    parseServerJson(r, body);
                    r.note = "由自托管 MediaSync 服务解析；画质以服务端返回为准。";
                } else {
                    r.note = "「" + r.platform + "」的解析需要平台登录态与解析器支持。\n"
                            + "请在「设置」中填写自托管 MediaSync 服务地址，或点击顶部「完整版」使用完整 Web 界面。";
                }
            } catch (Exception e) {
                r.note = "服务端解析失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage())
                        + "\n可检查服务地址与解析接口路径，或直接使用「完整版」Web 界面。";
            }
            MAIN.post(() -> cb.ok(r));
        });
    }

    private static void parseServerJson(Result r, String body) throws Exception {
        JSONObject o = new JSONObject(body);
        if (o.has("data") && o.get("data") instanceof JSONObject) o = o.getJSONObject("data");
        r.title = o.optString("title", o.optString("name", o.optString("webpage_url", r.title)));
        r.cover = o.optString("thumbnail", o.optString("cover", ""));
        JSONArray arr = o.optJSONArray("formats");
        if (arr == null) arr = o.optJSONArray("media");
        if (arr != null) {
            for (int x = 0; x < arr.length(); x++) {
                JSONObject j = arr.optJSONObject(x);
                if (j == null) continue;
                String u = j.optString("url", "");
                if (u.isEmpty()) continue;
                Format f = new Format();
                f.url = u;
                f.label = j.optString("format", j.optString("quality", j.optString("label", "画质 " + (x + 1))));
                f.ext = Platforms.ext(u, j.optString("mime", null));
                r.formats.add(f);
            }
        }
        if (r.formats.isEmpty()) {
            String u = o.optString("url", o.optString("download_url", ""));
            if (!u.isEmpty()) {
                Format f = new Format();
                f.url = u; f.label = "默认画质"; f.ext = Platforms.ext(u, null);
                r.formats.add(f);
            }
        }
    }

    /** 简单的 GET，供解析、订阅扫描与连接测试复用。 */
    public static String httpGet(String url, int timeout) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(timeout);
            conn.setRequestProperty("User-Agent", "MediaSync-Android/1.0");
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            int code = conn.getResponseCode();
            if (code >= 400) throw new Exception("HTTP " + code);
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
