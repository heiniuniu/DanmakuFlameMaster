/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.View;

import master.flame.danmaku.controller.DanmakuFilters;
import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.ICanvas.IPaint;
import master.flame.danmaku.danmaku.model.android.GLESCanvas;
import master.flame.danmaku.danmaku.model.android.SimplePaint;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import tv.cjump.gl.glrenderer.BasicTexture;
import tv.cjump.gl.glrenderer.BitmapTexture;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class DanmakuGLSurfaceView extends GLSurfaceView implements IDanmakuView,
        View.OnClickListener {

    public static final String TAG = "DanmakuSurfaceView";

    private Callback mCallback;

    private SurfaceHolder mSurfaceHolder;

    private HandlerThread mDrawThread;

    private DrawHandler handler;

    private boolean isSurfaceCreated;

    private boolean mEnableDanmakuDrwaingCache = true;

    private OnClickListener mOnClickListener;

    private boolean mShowFps;

    private boolean mDanmakuVisibile = true;

    protected int mDrawingThreadType = THREAD_TYPE_NORMAL_PRIORITY;

    public DanmakuGLSurfaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(new GLESRenderer());
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mSurfaceHolder = getHolder();
        setZOrderMediaOverlay(true);
      
        setOnClickListener(this);

    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        if (l != this) {
            mOnClickListener = l;
        } else
            super.setOnClickListener(l);
    }

    public DanmakuGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void addDanmaku(BaseDanmaku item) {
        if (handler != null) {
            handler.addDanmaku(item);
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
        if (handler != null) {
            handler.setCallback(callback);
        }
    }

    @Override
    public void release() {
        stop();
        DanmakuFilters.getDefault().clear();
        if (mDrawTimes != null)
            mDrawTimes.clear();
    }

    @Override
    public void stop() {
        stopDraw();
    }

    private void stopDraw() {
        if (handler != null) {
            handler.quit();
            handler = null;
        }
        if (mDrawThread != null) {
            try {
                mDrawThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mDrawThread.quit();
            mDrawThread = null;
        }
    }

    protected Looper getLooper(int type){
        if (mDrawThread != null) {
            mDrawThread.quit();
            mDrawThread = null;
        }
        
        int priority;
        switch (type) {
            case THREAD_TYPE_MAIN_THREAD:
                return Looper.getMainLooper();
            case THREAD_TYPE_HIGH_PRIORITY:
                priority = android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY;
                break;
            case THREAD_TYPE_LOW_PRIORITY:
                priority = android.os.Process.THREAD_PRIORITY_LOWEST;
                break;
            case THREAD_TYPE_NORMAL_PRIORITY:
            default:
                priority = android.os.Process.THREAD_PRIORITY_DEFAULT;
                break;
        }
        String threadName = "DFM Drawing thread #"+priority;
        mDrawThread = new HandlerThread(threadName, priority);
        mDrawThread.start();
        return mDrawThread.getLooper();
    }

    private void prepare() {
        if (handler == null)
            handler = new DrawHandler(getLooper(mDrawingThreadType), this, mDanmakuVisibile);
    }

    @Override
    public void prepare(BaseDanmakuParser parser) {
        prepare();
        handler.setParser(parser);
        handler.setCallback(mCallback);
        handler.prepare();
    }

    @Override
    public boolean isPrepared() {
        return handler != null && handler.isPrepared();
    }

    @Override
    public void showFPS(boolean show) {
        mShowFps = show;
    }

    private static final int MAX_RECORD_SIZE = 50;
    private static final int ONE_SECOND = 1000;
    private LinkedList<Long> mDrawTimes;

    private long dtime = 16;

    private float fps() {
        long lastTime = System.currentTimeMillis();
        mDrawTimes.addLast(lastTime);
        float dtime = lastTime - mDrawTimes.getFirst();
        int frames = mDrawTimes.size();
        if (frames > MAX_RECORD_SIZE) {
            mDrawTimes.removeFirst();
        }
        return dtime > 0 ? mDrawTimes.size() * ONE_SECOND / dtime : 0.0f;
    }

    @Override
    public long drawDanmakus() {
        return dtime;
    }

    public void toggle() {
        if (isSurfaceCreated) {
            if (handler == null)
                start();
            else if (handler.isStop()) {
                resume();
            } else
                pause();
        }
    }

    @Override
    public void pause() {
        if (handler != null)
            handler.pause();
    }

    @Override
    public void resume() {
        if (handler != null && handler.isPrepared())
            handler.resume();
        else {
            restart();
        }
    }

    public void restart() {
        stop();
        start();
    }

    @Override
    public void start() {
        start(0);
    }

    @Override
    public void start(long postion) {
        if (handler == null) {
            prepare();
        } else {
            handler.removeCallbacksAndMessages(null);
        }
        handler.obtainMessage(DrawHandler.START, postion).sendToTarget();
    }

    @Override
    public void onClick(View view) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(view);
        }
    }

    public void seekTo(Long ms) {
        if (handler != null) {
            handler.seekTo(ms);
        }
    }

    public void enableDanmakuDrawingCache(boolean enable) {
        mEnableDanmakuDrwaingCache = enable;
    }

    @Override
    public boolean isDanmakuDrawingCacheEnabled() {
        return mEnableDanmakuDrwaingCache;
    }

    @Override
    public boolean isViewReady() {
        return isSurfaceCreated;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void show() {
        showAndResumeDrawTask(null);
    }

    @Override
    public void showAndResumeDrawTask(Long position) {
        mDanmakuVisibile = true;
        if (handler == null) {
            return;
        }
        handler.showDanmakus(position);
    }

    @Override
    public void hide() {
        mDanmakuVisibile = false;
        if (handler == null) {
            return;
        }
        handler.hideDanmakus(false);
    }

    @Override
    public long hideAndPauseDrawTask() {
        mDanmakuVisibile = false;
        if (handler == null) {
            return 0;
        }
        long position = handler.hideDanmakus(true);
        return position;
    }

    @Override
    public void clear() {
        if (!isViewReady() || mSurfaceHolder == null) {
            return;
        }
//        Canvas canvas = mSurfaceHolder.lockCanvas();
//        if (canvas != null) {
//            DrawHelper.clearCanvas(canvas);
//            mSurfaceHolder.unlockCanvasAndPost(canvas);
//        }
    }

    @Override
    public boolean isShown() {
        if (handler == null || !isViewReady()) {
            return false;
        }
        return handler.getVisibility();
    }

    @Override
    public void setDrawingThreadType(int type) {
        mDrawingThreadType = type;
    }

    private class GLESRenderer implements Renderer {

        GLESCanvas canvas = null;
        private long lastTime = System.currentTimeMillis();
        
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            isSurfaceCreated = true;
            BasicTexture.invalidateAllTextures();
            paint.setColor(Color.GREEN);
            paint.setStyle(Style.FILL);
            paint.setTextSize(50f);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if (canvas == null || canvas.getWidth() != width || canvas.getHeight() != height) {
                canvas = new GLESCanvas(2, (GL11) gl, width, height);
                if (handler != null) {
                    handler.notifyDispSizeChanged(width, height);
                }
            }
        }
        
        SimplePaint paint = new SimplePaint();

        @Override
        public void onDrawFrame(GL10 gl) {
            if (canvas != null) {
//                if(lastTime == -1){
//                    lastTime = System.currentTimeMillis();
//                }
                //handler.updateTimer(System.currentTimeMillis() - lastTime);
                dtime = System.currentTimeMillis() - lastTime;
                lastTime = System.currentTimeMillis();
                handler.draw(canvas);
//                canvas.clear();
                if(mShowFps) {
                    canvas.drawText("fps" + 1000/Math.max(1,dtime) + "," + handler.getCurrentTime()/1000 + "s" + ",h:"+canvas.getHeight() + ",w" + canvas.getWidth(), 0, canvas.getHeight() - 100, paint);
                }
            }
        }

    }

    @Override
    public void removeAllDanmakus() {
        if (handler != null) {
            handler.removeAllDanmakus();
        }
    }
    
    @Override
    public void removeAllLiveDanmakus() {
        if (handler != null) {
            handler.removeAllLiveDanmakus();
        }
    }

    @Override
    public long getCurrentTime() {
        if (handler != null) {
            return handler.getCurrentTime();
        }
        return 0;
    }

}