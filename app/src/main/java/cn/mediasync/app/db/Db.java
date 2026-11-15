package cn.mediasync.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 本地 SQLite：下载任务、订阅、直播间、解析历史。 */
public class Db extends SQLiteOpenHelper {
    private static final String NAME = "mediasync.db";
    private static final int VER = 1;

    public Db(Context c) { super(c.getApplicationContext(), NAME, null, VER); }

    @Override
    public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE tasks (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT, "
                + "platform TEXT, quality TEXT, path TEXT, status TEXT, progress INTEGER DEFAULT 0, "
                + "total INTEGER DEFAULT 0, err TEXT, created INTEGER)");
        d.execSQL("CREATE TABLE subs (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, platform TEXT, "
                + "url TEXT, enabled INTEGER DEFAULT 1, interval INTEGER DEFAULT 60, "
                + "last_scan INTEGER DEFAULT 0, new_count INTEGER DEFAULT 0)");
        d.execSQL("CREATE TABLE rooms (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, platform TEXT, "
                + "url TEXT, room_id TEXT, quality TEXT, enabled INTEGER DEFAULT 1, "
                + "status TEXT, last_record INTEGER DEFAULT 0)");
        d.execSQL("CREATE TABLE history (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT, "
                + "platform TEXT, created INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase d, int oldV, int newV) {
        d.execSQL("DROP TABLE IF EXISTS tasks");
        d.execSQL("DROP TABLE IF EXISTS subs");
        d.execSQL("DROP TABLE IF EXISTS rooms");
        d.execSQL("DROP TABLE IF EXISTS history");
        onCreate(d);
    }

    private SQLiteDatabase w() { return getWritableDatabase(); }

    public List<ContentValues> query(String sql) {
        List<ContentValues> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql, null);
        if (c != null) {
            while (c.moveToNext()) {
                ContentValues v = new ContentValues();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    if (c.isNull(i)) continue;
                    String col = c.getColumnName(i);
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_INTEGER: v.put(col, c.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT: v.put(col, c.getDouble(i)); break;
                        default: v.put(col, c.getString(i));
                    }
                }
                out.add(v);
            }
            c.close();
        }
        return out;
    }

    // -------- 通用取值工具 --------
    public static String s(ContentValues v, String k) {
        Object o = v == null ? null : v.get(k);
        return o == null ? "" : String.valueOf(o);
    }
    public static long l(ContentValues v, String k) {
        Object o = v == null ? null : v.get(k);
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }
    public static int i(ContentValues v, String k) { return (int) l(v, k); }

    // -------- 下载任务 --------
    public long addTask(String title, String url, String platform, String quality) {
        ContentValues v = new ContentValues();
        v.put("title", title); v.put("url", url); v.put("platform", platform);
        v.put("quality", quality); v.put("status", "queued");
        v.put("progress", 0); v.put("total", 0); v.put("created", System.currentTimeMillis());
        return w().insert("tasks", null, v);
    }

    public List<ContentValues> tasks() { return query("SELECT * FROM tasks ORDER BY created DESC"); }
    public List<ContentValues> queued() { return query("SELECT * FROM tasks WHERE status='queued' OR status='running' ORDER BY created ASC"); }
    public ContentValues task(long id) {
        List<ContentValues> r = query("SELECT * FROM tasks WHERE _id=" + id);
        return r.isEmpty() ? null : r.get(0);
    }
    public void taskUpdate(long id, ContentValues v) {
        w().update("tasks", v, "_id=?", new String[]{String.valueOf(id)});
    }
    public void taskStatus(long id, String st) {
        ContentValues v = new ContentValues(); v.put("status", st); taskUpdate(id, v);
    }
    public void taskProgress(long id, int p, long total) {
        ContentValues v = new ContentValues();
        v.put("progress", p); v.put("total", total); taskUpdate(id, v);
    }
    public void taskDone(long id, String path) {
        ContentValues v = new ContentValues();
        v.put("status", "done"); v.put("progress", 100); v.put("path", path); v.put("err", "");
        taskUpdate(id, v);
    }
    public void taskError(long id, String e) {
        ContentValues v = new ContentValues(); v.put("status", "error"); v.put("err", e);
        taskUpdate(id, v);
    }
    public void clearDone() { w().delete("tasks", "status='done'", null); }
    public void delTask(long id) { w().delete("tasks", "_id=?", new String[]{String.valueOf(id)}); }

    // -------- 订阅 --------
    public long addSub(String title, String platform, String url, int interval) {
        ContentValues v = new ContentValues();
        v.put("title", title); v.put("platform", platform); v.put("url", url);
        v.put("enabled", 1); v.put("interval", interval);
        v.put("last_scan", 0); v.put("new_count", 0);
        return w().insert("subs", null, v);
    }
    public List<ContentValues> subs() { return query("SELECT * FROM subs ORDER BY _id DESC"); }
    public void subToggle(long id, boolean on) {
        ContentValues v = new ContentValues(); v.put("enabled", on ? 1 : 0);
        w().update("subs", v, "_id=?", new String[]{String.valueOf(id)});
    }
    public void subTouch(long id, int newCount) {
        ContentValues v = new ContentValues();
        v.put("last_scan", System.currentTimeMillis()); v.put("new_count", newCount);
        w().update("subs", v, "_id=?", new String[]{String.valueOf(id)});
    }
    public void delSub(long id) { w().delete("subs", "_id=?", new String[]{String.valueOf(id)}); }

    // -------- 直播间 --------
    public long addRoom(String title, String platform, String url, String roomId, String quality) {
        ContentValues v = new ContentValues();
        v.put("title", title); v.put("platform", platform); v.put("url", url);
        v.put("room_id", roomId); v.put("quality", quality); v.put("enabled", 1);
        v.put("status", "监控中"); v.put("last_record", 0);
        return w().insert("rooms", null, v);
    }
    public List<ContentValues> rooms() { return query("SELECT * FROM rooms ORDER BY _id DESC"); }
    public void roomToggle(long id, boolean on) {
        ContentValues v = new ContentValues(); v.put("enabled", on ? 1 : 0);
        v.put("status", on ? "监控中" : "已暂停");
        w().update("rooms", v, "_id=?", new String[]{String.valueOf(id)});
    }
    public void roomStatus(long id, String st) {
        ContentValues v = new ContentValues(); v.put("status", st);
        w().update("rooms", v, "_id=?", new String[]{String.valueOf(id)});
    }
    public void delRoom(long id) { w().delete("rooms", "_id=?", new String[]{String.valueOf(id)}); }

    // -------- 解析历史 --------
    public void addHistory(String title, String url, String platform) {
        ContentValues v = new ContentValues();
        v.put("title", title); v.put("url", url); v.put("platform", platform);
        v.put("created", System.currentTimeMillis());
        w().insert("history", null, v);
        w().execSQL("DELETE FROM history WHERE _id NOT IN (SELECT _id FROM history ORDER BY _id DESC LIMIT 100)");
    }
    public List<ContentValues> history() { return query("SELECT * FROM history ORDER BY _id DESC LIMIT 50"); }
    public void clearHistory() { w().delete("history", null, null); }

    // -------- 备份 / 恢复 --------
    public String exportJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("app", "媒体同步");
            o.put("version", 1);
            o.put("exported", System.currentTimeMillis());
            o.put("subs", toJson(query("SELECT * FROM subs")));
            o.put("rooms", toJson(query("SELECT * FROM rooms")));
            o.put("history", toJson(query("SELECT * FROM history")));
            return o.toString(2);
        } catch (Exception e) { return "{}"; }
    }

    private JSONArray toJson(List<ContentValues> rows) throws Exception {
        JSONArray a = new JSONArray();
        for (ContentValues v : rows) {
            JSONObject j = new JSONObject();
            for (String k : v.keySet()) j.put(k, v.get(k));
            a.put(j);
        }
        return a;
    }

    /** 恢复订阅 / 直播间 / 历史（不恢复下载任务，避免重复下载）。 */
    public int importJson(String json) {
        int n = 0;
        try {
            JSONObject o = new JSONObject(json);
            n += restore("subs", o.optJSONArray("subs"));
            n += restore("rooms", o.optJSONArray("rooms"));
            n += restore("history", o.optJSONArray("history"));
        } catch (Exception ignored) { }
        return n;
    }

    private int restore(String table, JSONArray a) {
        if (a == null) return 0;
        int n = 0;
        for (int x = 0; x < a.length(); x++) {
            try {
                JSONObject j = a.getJSONObject(x);
                ContentValues v = new ContentValues();
                java.util.Iterator<String> it = j.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    if ("_id".equals(k)) continue;
                    Object val = j.get(k);
                    if (val instanceof Integer || val instanceof Long) v.put(k, ((Number) val).longValue());
                    else if (val instanceof Double) v.put(k, ((Number) val).doubleValue());
                    else if (val instanceof Boolean) v.put(k, (Boolean) val);
                    else v.put(k, String.valueOf(val));
                }
                w().insert(table, null, v);
                n++;
            } catch (Exception ignored) { }
        }
        return n;
    }
}
