package cn.mediasync.app.core;

import android.content.Context;
import android.content.SharedPreferences;

/** 应用配置持久化。 */
public class Settings {
    private static final String PREF = "mediasync_settings";
    private final SharedPreferences sp;

    public Settings(Context c) {
        sp = c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private String gs(String k, String d) { return sp.getString(k, d); }
    private void ps(String k, String v) { sp.edit().putString(k, v).apply(); }
    private boolean gb(String k, boolean d) { return sp.getBoolean(k, d); }
    private void pb(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); }
    private int gi(String k, int d) { return sp.getInt(k, d); }
    private void pi(String k, int v) { sp.edit().putInt(k, v).apply(); }

    /** 自托管 MediaSync 服务地址，如 http://192.168.1.10:4399 */
    public String server() { return gs("server", ""); }
    public void server(String v) {
        String t = v == null ? "" : v.trim();
        if (!t.isEmpty() && !t.startsWith("http://") && !t.startsWith("https://")) t = "http://" + t;
        if (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        ps("server", t);
    }

    /** 服务端解析接口路径，可在设置中按实际部署调整。 */
    public String parseApi() {
        String v = gs("parse_api", "/api/parse");
        return v.startsWith("/") ? v : "/" + v;
    }
    public void parseApi(String v) { ps("parse_api", v); }

    public boolean dark() { return gb("dark", false); }
    public void dark(boolean v) { pb("dark", v); }

    public int concurrency() { return Math.max(1, Math.min(5, gi("concurrency", 2))); }
    public void concurrency(int v) { pi("concurrency", Math.max(1, Math.min(5, v))); }

    public boolean wifiOnly() { return gb("wifi_only", false); }
    public void wifiOnly(boolean v) { pb("wifi_only", v); }

    public boolean notifyEnabled() { return gb("notify", true); }
    public void notifyEnabled(boolean v) { pb("notify", v); }

    public String quality() { return gs("quality", "最高画质"); }
    public void quality(String v) { ps("quality", v); }

    public int scanInterval() { return Math.max(15, gi("scan_interval", 60)); }
    public void scanInterval(int v) { pi("scan_interval", Math.max(15, v)); }

    public String downloadDir() { return gs("download_dir", ""); }
    public void downloadDir(String v) { ps("download_dir", v); }

    public String tgToken() { return gs("tg_token", ""); }
    public void tgToken(String v) { ps("tg_token", v); }
    public String tgChat() { return gs("tg_chat", ""); }
    public void tgChat(String v) { ps("tg_chat", v); }
    public String wecom() { return gs("wecom", ""); }
    public void wecom(String v) { ps("wecom", v); }

    public String proxy() { return gs("proxy", ""); }
    public void proxy(String v) { ps("proxy", v); }

    public String webPath() { return gs("web_path", ""); }
    public void webPath(String v) { ps("web_path", v); }

    public String asJson() {
        StringBuilder sb = new StringBuilder("{");
        for (String k : new String[]{"server", "parse_api", "concurrency", "wifi_only", "notify",
                "quality", "scan_interval", "tg_token", "tg_chat", "wecom", "proxy", "dark"}) {
            if (sb.length() > 1) sb.append(',');
            Object v = sp.getAll().get(k);
            sb.append('"').append(k).append("\":");
            if (v instanceof Boolean || v instanceof Integer) sb.append(v);
            else sb.append('"').append(String.valueOf(v == null ? "" : v).replace("\"", "\\\"")).append('"');
        }
        return sb.append('}').toString();
    }

    public void fromJson(String json) {
        try {
            org.json.JSONObject o = new org.json.JSONObject(json);
            if (o.has("server")) server(o.optString("server", ""));
            if (o.has("parse_api")) parseApi(o.optString("parse_api", "/api/parse"));
            if (o.has("concurrency")) concurrency(o.optInt("concurrency", 2));
            if (o.has("wifi_only")) wifiOnly(o.optBoolean("wifi_only", false));
            if (o.has("notify")) notifyEnabled(o.optBoolean("notify", true));
            if (o.has("quality")) quality(o.optString("quality", "最高画质"));
            if (o.has("scan_interval")) scanInterval(o.optInt("scan_interval", 60));
            if (o.has("tg_token")) tgToken(o.optString("tg_token", ""));
            if (o.has("tg_chat")) tgChat(o.optString("tg_chat", ""));
            if (o.has("wecom")) wecom(o.optString("wecom", ""));
            if (o.has("proxy")) proxy(o.optString("proxy", ""));
            if (o.has("dark")) dark(o.optBoolean("dark", false));
        } catch (Exception ignored) { }
    }
}
