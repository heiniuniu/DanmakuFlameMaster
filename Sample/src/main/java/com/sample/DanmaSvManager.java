package com.sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.BaseCacheStuffer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.util.IOUtils;

/**
 * Created by pengleigang on 2017/2/23.
 * 烈焰弹幕管理类
 */

public class DanmaSvManager {

    private IDanmakuView mDanmakuView;
    private Context mContext;
    private DanmakuContext mDanmakuContext;
    private BaseDanmakuParser mParser;
    private OnClickEvent mOnClickEvent;

    public interface OnClickEvent {
        void onclick();
    }

    public DanmaSvManager(Context context, IDanmakuView danmakuView) {
        this.mContext = context;
        this.mDanmakuView = danmakuView;
        init();
    }

    public DanmaSvManager(Context context, IDanmakuView danmakuView, OnClickEvent event) {
        this.mContext = context;
        this.mDanmakuView = danmakuView;
        mOnClickEvent = event;
        init();
    }

    private void init() {

        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5); // 滚动弹幕最大显示5行
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        mDanmakuContext = DanmakuContext.create();
        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3).setDuplicateMergingEnabled(false).setScrollSpeedFactor(1.2f).setScaleTextSize(1.2f)
                .setCacheStuffer(new SpannedCacheStuffer(), mCacheStufferAdapter) // 图文混排使用SpannedCacheStuffer
//        .setCacheStuffer(new BackgroundCacheStuffer())  // 绘制背景使用BackgroundCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);
        if (mDanmakuView != null) {
            mParser = createParser(mContext.getResources().openRawResource(R.raw.comments));
            mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
//                    Log.d("DFM", "danmakuShown(): text=" + danmaku.text);
                }

                @Override
                public void prepared() {
                    mDanmakuView.start();
                }
            });
            mDanmakuView.setOnDanmakuClickListener(new IDanmakuView.OnDanmakuClickListener() {

                @Override
                public boolean onDanmakuClick(IDanmakus danmakus) {
                    Log.d("DFM", "onDanmakuClick: danmakus size:" + danmakus.size());
                    BaseDanmaku latest = danmakus.last();
                    if (null != latest) {
                        Log.d("DFM", "onDanmakuClick: text of latest danmaku:" + latest.text);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean onViewClick(IDanmakuView view) {
                    if (mOnClickEvent != null) {
                        mOnClickEvent.onclick();
                    }
                    return false;
                }
            });
            mDanmakuView.prepare(mParser, mDanmakuContext);
            mDanmakuView.showFPS(true);
            mDanmakuView.enableDanmakuDrawingCache(true);
        }
    }

    private BaseCacheStuffer.Proxy mCacheStufferAdapter = new BaseCacheStuffer.Proxy() {

        private Drawable mDrawable;

        @Override
        public void prepareDrawing(final BaseDanmaku danmaku, boolean fromWorkerThread) {
            if (danmaku.text instanceof Spanned) { // 根据你的条件检查是否需要需要更新弹幕
                // FIXME 这里只是简单启个线程来加载远程url图片，请使用你自己的异步线程池，最好加上你的缓存池
                new Thread() {

                    @Override
                    public void run() {
                        String url = "http://www.bilibili.com/favicon.ico";
                        InputStream inputStream = null;
                        Drawable drawable = mDrawable;
                        if (drawable == null) {
                            try {
                                URLConnection urlConnection = new URL(url).openConnection();
                                inputStream = urlConnection.getInputStream();
                                drawable = BitmapDrawable.createFromStream(inputStream, "bitmap");
                                mDrawable = drawable;
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                IOUtils.closeQuietly(inputStream);
                            }
                        }
                        if (drawable != null) {
                            drawable.setBounds(0, 0, 100, 100);
                            SpannableStringBuilder spannable = createSpannable(drawable);
                            danmaku.text = spannable;
                            if (mDanmakuView != null) {
                                mDanmakuView.invalidateDanmaku(danmaku, false);
                            }
                            return;
                        }
                    }
                }.start();
            }
        }

        @Override
        public void releaseResource(BaseDanmaku danmaku) {
            // TODO 重要:清理含有ImageSpan的text中的一些占用内存的资源 例如drawable
        }
    };

    private SpannableStringBuilder createSpannable(Drawable drawable) {
        String text = "bitmap";
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
        ImageSpan span = new ImageSpan(drawable);//ImageSpan.ALIGN_BOTTOM);
        spannableStringBuilder.setSpan(span, 0, text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableStringBuilder.append("图文混排");
        spannableStringBuilder.setSpan(new BackgroundColorSpan(Color.parseColor("#8A2233B1")), 0, spannableStringBuilder.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableStringBuilder;
    }

    private BaseDanmakuParser createParser(InputStream stream) {

        if (stream == null) {
            return new BaseDanmakuParser() {

                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }

        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);

        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        BaseDanmakuParser parser = new BiliDanmukuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        return parser;

    }

    /**
     * 绘制背景(自定义弹幕样式)
     */
    private static class BackgroundCacheStuffer extends SpannedCacheStuffer {
        // 通过扩展SimpleTextCacheStuffer或SpannedCacheStuffer个性化你的弹幕样式
        final Paint paint = new Paint();

        @Override
        public void measure(BaseDanmaku danmaku, TextPaint paint, boolean fromWorkerThread) {
            danmaku.padding = 10;  // 在背景绘制模式下增加padding
            super.measure(danmaku, paint, fromWorkerThread);
        }

        @Override
        public void drawBackground(BaseDanmaku danmaku, Canvas canvas, float left, float top) {
            paint.setColor(0x8125309b);
            canvas.drawRect(left + 2, top + 2, left + danmaku.paintWidth - 2, top + danmaku.paintHeight - 2, paint);
        }

        @Override
        public void drawStroke(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, Paint paint) {
            // 禁用描边绘制
        }
    }

    /**
     * 添加图文混排的弹幕
     *
     * @param islive
     */
    public void addDanmaKuShowTextAndImage(boolean islive) {
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.ic_launcher);
        drawable.setBounds(0, 0, 100, 100);
        SpannableStringBuilder spannable = createSpannable(drawable);
        danmaku.text = spannable;
        danmaku.padding = 5;
        danmaku.priority = 1;  // 一定会显示, 一般用于本机发送的弹幕
        danmaku.isLive = islive;
        danmaku.setTime(mDanmakuView.getCurrentTime() + 1200);
        danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.RED;
        danmaku.textShadowColor = 0; // 重要：如果有图文混排，最好不要设置描边(设textShadowColor=0)，否则会进行两次复杂的绘制导致运行效率降低
        danmaku.underlineColor = Color.GREEN;
        mDanmakuView.addDanmaku(danmaku);
    }

    /**
     * 添加普通的弹幕
     *
     * @param islive
     */
    public void addDanmaku(boolean islive) {
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }
        // for(int i=0;i<100;i++){
        // }
        danmaku.text = "这是一条弹幕" + System.nanoTime();
        danmaku.padding = 5;
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = islive;
        danmaku.setTime(mDanmakuView.getCurrentTime() + 1200);
        danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.RED;
        danmaku.textShadowColor = Color.WHITE;
        // danmaku.underlineColor = Color.GREEN;
        danmaku.borderColor = Color.GREEN;
        mDanmakuView.addDanmaku(danmaku);

    }

    public void onPause() {
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
    }

    public void onResume() {
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    public void onDestory() {
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }
    }
}
