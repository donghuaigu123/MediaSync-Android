package cn.mediasync.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import cn.mediasync.app.MainActivity;
import cn.mediasync.app.core.Ui;

/** 页面基类：加载布局、套用主题、暴露生命周期回调。 */
public abstract class Page extends FrameLayout {
    protected final MainActivity act;

    public Page(MainActivity a, int layoutRes) {
        super(a);
        this.act = a;
        LayoutInflater.from(a).inflate(layoutRes, this, true);
        Ui.applyTheme(this);
        init();
    }

    protected abstract void init();

    /** 切换到该页面时调用，用于刷新数据。 */
    public void onShow() { }

    /** 返回键：消费返回 true。 */
    public boolean onBack() { return false; }

    @SuppressWarnings("unchecked")
    protected <T extends View> T $(int id) { return (T) findViewById(id); }

    protected Context ctx() { return getContext(); }
}
