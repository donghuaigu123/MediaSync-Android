package cn.mediasync.app.core;

import java.util.Locale;

/** 平台识别与链接类型判断。 */
public final class Platforms {
    private Platforms() {}

    public static final String[] ALL = {
            "Bilibili", "YouTube", "抖音", "小红书", "Twitter/X", "Instagram", "Pinterest",
            "微博", "微信视频号", "微信公众号", "Telegram", "新片场",
            "优酷", "爱奇艺", "腾讯视频", "央视频", "Twitch", "虎牙", "斗鱼", "通用媒体"
    };

    private static final String[][] RULES = {
            {"bilibili.com", "Bilibili"}, {"b23.tv", "Bilibili"},
            {"youtube.com", "YouTube"}, {"youtu.be", "YouTube"},
            {"douyin.com", "抖音"}, {"iesdouyin.com", "抖音"},
            {"xiaohongshu.com", "小红书"}, {"xhslink.com", "小红书"},
            {"twitter.com", "Twitter/X"}, {"x.com", "Twitter/X"},
            {"instagram.com", "Instagram"}, {"pinterest.com", "Pinterest"},
            {"weibo.com", "微博"}, {"weibo.cn", "微博"},
            {"channels.weixin.qq.com", "微信视频号"}, {"mp.weixin.qq.com", "微信公众号"},
            {"t.me", "Telegram"}, {"xinpianchang.com", "新片场"},
            {"youku.com", "优酷"}, {"iqiyi.com", "爱奇艺"},
            {"v.qq.com", "腾讯视频"}, {"yangshipin.cn", "央视频"},
            {"twitch.tv", "Twitch"}, {"huya.com", "虎牙"}, {"douyu.com", "斗鱼"}
    };

    public static String detect(String url) {
        if (url == null) return "通用媒体";
        String u = url.toLowerCase(Locale.ROOT);
        for (String[] r : RULES) if (u.contains(r[0])) return r[1];
        return "通用媒体";
    }

    /** 是否是可直接下载的媒体直链（无需服务端解析）。 */
    public static boolean isDirect(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.ROOT);
        int q = u.indexOf('?');
        if (q > 0) u = u.substring(0, q);
        return u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".mkv")
                || u.endsWith(".mov") || u.endsWith(".m4v") || u.endsWith(".flv")
                || u.endsWith(".mp3") || u.endsWith(".m4a") || u.endsWith(".aac")
                || u.endsWith(".wav") || u.endsWith(".flac") || u.endsWith(".ogg")
                || u.endsWith(".jpg") || u.endsWith(".jpeg") || u.endsWith(".png")
                || u.endsWith(".gif") || u.endsWith(".webp") || u.endsWith(".bmp")
                || u.endsWith(".m3u8") || u.endsWith(".mpd") || u.endsWith(".ts")
                || u.endsWith(".zip") || u.endsWith(".pdf") || u.endsWith(".srt")
                || u.endsWith(".ass") || u.endsWith(".vtt");
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        String u = raw.trim();
        if (u.isEmpty()) return "";
        int sp = u.indexOf(' ');
        if (sp > 0) u = u.substring(0, sp);
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://" + u;
        return u;
    }

    public static String fileName(String url) {
        String u = url;
        int q = u.indexOf('?');
        if (q > 0) u = u.substring(0, q);
        int slash = u.lastIndexOf('/');
        String n = slash >= 0 ? u.substring(slash + 1) : u;
        if (n.isEmpty()) n = "media";
        try { n = java.net.URLDecoder.decode(n, "UTF-8"); } catch (Exception ignored) { }
        return n;
    }

    public static String ext(String url, String contentType) {
        String n = fileName(url);
        int dot = n.lastIndexOf('.');
        if (dot > 0 && dot < n.length() - 1 && n.length() - dot <= 6) return n.substring(dot);
        if (contentType != null) {
            String c = contentType.toLowerCase(Locale.ROOT);
            if (c.contains("mp4")) return ".mp4";
            if (c.contains("jpeg")) return ".jpg";
            if (c.contains("png")) return ".png";
            if (c.contains("audio")) return ".mp3";
        }
        return ".mp4";
    }

    public static String mimeOf(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".mp4") || n.endsWith(".m4v") || n.endsWith(".webm")) return "video/*";
        if (n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".flac")) return "audio/*";
        if (n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".gif") || n.endsWith(".webp")) return "image/*";
        if (n.endsWith(".zip")) return "application/zip";
        if (n.endsWith(".pdf")) return "application/pdf";
        return "*/*";
    }
}
