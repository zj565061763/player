/*
 * Copyright (C) 2017 zhengjun, fanwe (http://www.fanwe.com)
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
package com.fanwe.lib.player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;

public class SDMediaPlayer
{
    private static SDMediaPlayer sInstance;

    private MediaPlayer mPlayer;
    private State mState = State.Idle;

    private String mDataPath;
    private int mDataRawResId;

    private boolean mIsDataInitialized;

    private WeakReference<SurfaceHolder> mSurfaceHolder;
    private boolean mIsLooping;

    private CountDownTimer mProgressTimer;

    private OnStateChangeCallback mOnStateChangeCallback;
    private OnExceptionCallback mOnExceptionCallback;
    private OnProgressCallback mOnProgressCallback;

    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnPreparedListener mOnPreparedListener;

    public SDMediaPlayer()
    {
        //构造方法为public权限，可以单独new对象而不用全局单例对象
        init();
    }

    public static SDMediaPlayer getInstance()
    {
        if (sInstance == null)
        {
            synchronized (SDMediaPlayer.class)
            {
                if (sInstance == null)
                {
                    sInstance = new SDMediaPlayer();
                }
            }
        }
        return sInstance;
    }

    /**
     * 初始化播放器，调用release()后如果想要继续使用，要调用此方法初始化
     */
    public void init()
    {
        if (mPlayer != null)
        {
            release();
        }
        mPlayer = new MediaPlayer();
        mPlayer.setOnErrorListener(mInternalOnErrorListener);
        mPlayer.setOnPreparedListener(mInternalOnPreparedListener);
        mPlayer.setOnCompletionListener(mInternalOnCompletionListener);
        mPlayer.setOnVideoSizeChangedListener(mInternalOnVideoSizeChangedListener);
    }

    /**
     * 设置状态变化回调
     *
     * @param onStateChangeCallback
     */
    public void setOnStateChangeCallback(OnStateChangeCallback onStateChangeCallback)
    {
        mOnStateChangeCallback = onStateChangeCallback;
    }

    /**
     * 设置异常回调
     *
     * @param onExceptionCallback
     */
    public void setOnExceptionCallback(OnExceptionCallback onExceptionCallback)
    {
        mOnExceptionCallback = onExceptionCallback;
    }

    /**
     * 设置播放进度回调
     *
     * @param onProgressCallback
     */
    public void setOnProgressCallback(OnProgressCallback onProgressCallback)
    {
        mOnProgressCallback = onProgressCallback;
        startProgressTimerIfNeed();
    }

    /**
     * 设置视频宽高发生变化回调
     *
     * @param onVideoSizeChangedListener
     */
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener onVideoSizeChangedListener)
    {
        mOnVideoSizeChangedListener = onVideoSizeChangedListener;
    }

    /**
     * 设置播放完毕回调
     *
     * @param onCompletionListener
     */
    public void setOnCompletionListener(OnCompletionListener onCompletionListener)
    {
        mOnCompletionListener = onCompletionListener;
    }

    /**
     * 设置准备完毕回调
     *
     * @param onPreparedListener
     */
    public void setOnPreparedListener(OnPreparedListener onPreparedListener)
    {
        mOnPreparedListener = onPreparedListener;
    }

    //----------proxy method start----------

    /**
     * 返回总时长（毫秒）
     *
     * @return
     */
    public int getDuration()
    {
        int value = 0;
        switch (mState)
        {
            case Playing:
            case Paused:
            case Stopped:
            case Completed:
                value = mPlayer.getDuration();
                break;
        }
        return value;
    }

    /**
     * 返回当前播放的进度位置（毫秒）
     *
     * @return
     */
    public int getCurrentPosition()
    {
        int value = 0;
        switch (mState)
        {
            case Playing:
            case Paused:
            case Completed:
                value = mPlayer.getCurrentPosition();
                break;
        }
        return value;
    }

    /**
     * 设置SurfaceHolder
     *
     * @param holder
     */
    public void setDisplay(SurfaceHolder holder)
    {
        if (mState == State.Initialized)
        {
            mPlayer.setDisplay(holder);
        }

        setSurfaceHolder(holder);
    }

    /**
     * 设置左右声道音量
     *
     * @param leftVolume  [0-1]
     * @param rightVolume [0-1]
     */
    public void setVolume(float leftVolume, float rightVolume)
    {
        mPlayer.setVolume(leftVolume, rightVolume);
    }

    /**
     * 设置是否循环播放
     *
     * @param looping
     */
    public void setLooping(boolean looping)
    {
        mIsLooping = looping;
        if (isDataInitialized())
        {
            mPlayer.setLooping(looping);
        }
    }

    /**
     * 当前是否循环播放模式
     *
     * @return
     */
    public boolean isLooping()
    {
        return mIsLooping;
    }

    public int getVideoWidth()
    {
        return mPlayer.getVideoWidth();
    }

    public int getVideoHeight()
    {
        return mPlayer.getVideoHeight();
    }

    //----------proxy method end----------

    private void setSurfaceHolder(SurfaceHolder holder)
    {
        final SurfaceHolder oldHolder = getSurfaceHolder();
        if (oldHolder != holder)
        {
            if (holder != null)
            {
                mSurfaceHolder = new WeakReference<>(holder);
            } else
            {
                mSurfaceHolder = null;
            }
        }
    }

    /**
     * 返回设置的SurfaceHolder
     *
     * @return
     */
    private SurfaceHolder getSurfaceHolder()
    {
        if (mSurfaceHolder != null)
        {
            return mSurfaceHolder.get();
        } else
        {
            return null;
        }
    }

    //----------data start----------

    public String getDataPath()
    {
        return mDataPath;
    }

    public int getDataRawResId()
    {
        return mDataRawResId;
    }

    /**
     * 设置数据源
     *
     * @param path 本地文件路径或者链接地址
     * @return
     */
    public boolean setDataPath(String path)
    {
        try
        {
            if (!TextUtils.isEmpty(mDataPath) && mDataPath.equals(path))
            {
                return true;
            }

            reset();
            mPlayer.setDataSource(path);
            mDataPath = path;
            setState(State.Initialized);
            return true;
        } catch (Exception e)
        {
            notifyException(e);
            return false;
        }
    }

    /**
     * 设置文件rawResId
     *
     * @param rawResId
     * @param context
     */
    public boolean setDataRawResId(int rawResId, Context context)
    {
        try
        {
            if (mDataRawResId == rawResId)
            {
                return true;
            }

            reset();
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(rawResId);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mDataRawResId = rawResId;
            setState(State.Initialized);
            return true;
        } catch (Exception e)
        {
            notifyException(e);
            return false;
        }
    }

    //----------data end----------

    /**
     * 是否播放状态
     *
     * @return
     */
    public boolean isPlaying()
    {
        return State.Playing == mState;
    }

    /**
     * 是否暂停状态
     *
     * @return
     */
    public boolean isPaused()
    {
        return State.Paused == mState;
    }

    /**
     * 播放数据是否已经初始化
     *
     * @return
     */
    public boolean isDataInitialized()
    {
        return mIsDataInitialized;
    }

    /**
     * 播放进度移动到某个位置
     *
     * @param position 某个时间点（毫秒）
     * @return true-发起seek成功
     */
    public boolean seekTo(int position)
    {
        if (isDataInitialized())
        {
            mPlayer.seekTo(position);
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * 播放和暂停
     */
    public void performPlayPause()
    {
        performPlayInside(false);
    }

    /**
     * 重新播放和停止播放
     */
    public void performPlayStop()
    {
        performPlayInside(true);
    }

    /**
     * 模拟暂停恢复播放
     *
     * @param restart true-恢复的时候重头播放
     */
    private void performPlayInside(boolean restart)
    {
        try
        {
            if (isDataInitialized())
            {
                if (isPlaying())
                {
                    if (restart)
                    {
                        stop();
                    } else
                    {
                        pause();
                    }
                } else
                {
                    start();
                }
            }
        } catch (Exception e)
        {
            notifyException(e);
        }
    }

    /**
     * 开始播放
     */
    public void start()
    {
        switch (mState)
        {
            case Initialized:
                prepareAsyncPlayer();
                break;
            case Prepared:
                startPlayer();
                break;
            case Paused:
                startPlayer();
                break;
            case Completed:
                startPlayer();
                break;
            case Stopped:
                prepareAsyncPlayer();
                break;
            default:
                break;
        }
    }

    /**
     * 暂停播放
     */
    public void pause()
    {
        switch (mState)
        {
            case Playing:
                pausePlayer();
                break;
            default:
                break;
        }
    }

    /**
     * 停止播放
     */
    public void stop()
    {
        switch (mState)
        {
            case Prepared:
                stopPlayer();
                break;
            case Playing:
                stopPlayer();
                break;
            case Paused:
                stopPlayer();
                break;
            case Completed:
                stopPlayer();
                break;
            default:
                break;
        }
    }

    /**
     * 重置播放器，一般用于关闭播放界面的时候调用
     */
    public void reset()
    {
        if (mState != State.Released)
        {
            stop();
            resetPlayer();
        }
    }

    /**
     * 释放播放器，用于不再需要播放器的时候调用，调用此方法后，需要手动调用init()方法初始化后才可以使用
     */
    public void release()
    {
        stop();
        releasePlayer();
    }

    /**
     * 返回当前状态
     *
     * @return
     */
    public State getState()
    {
        return mState;
    }

    private void setState(State state)
    {
        if (mState != state)
        {
            final State oldState = mState;

            mState = state;

            switch (mState)
            {
                case Initialized:
                    setDataInitialized(true);
                    break;
                case Playing:
                    startProgressTimerIfNeed();
                    break;
                case Paused:
                case Stopped:
                    notifyProgressCallback();
                    stopProgressTimer();
                    break;
            }

            if (mOnStateChangeCallback != null)
            {
                mOnStateChangeCallback.onStateChanged(oldState, mState, this);
            }
        }
    }

    /**
     * 设置播放数据是否已经初始化
     *
     * @param dataInitialized
     */
    private void setDataInitialized(boolean dataInitialized)
    {
        mIsDataInitialized = dataInitialized;

        if (mIsDataInitialized)
        {
            setDisplay(getSurfaceHolder());
            setLooping(mIsLooping);
        }
    }

    private void prepareAsyncPlayer()
    {
        mPlayer.prepareAsync();
        setState(State.Preparing);
    }

    private void startPlayer()
    {
        mPlayer.start();
        setState(State.Playing);
    }

    private void pausePlayer()
    {
        mPlayer.pause();
        setState(State.Paused);
    }

    private void stopPlayer()
    {
        mPlayer.stop();
        setState(State.Stopped);
    }

    private void resetPlayer()
    {
        resetDataInternal();

        mPlayer.reset();
        setState(State.Idle);
    }

    private void releasePlayer()
    {
        resetDataInternal();
        setSurfaceHolder(null);

        mPlayer.release();
        setState(State.Released);
    }

    private void resetDataInternal()
    {
        mDataPath = null;
        mDataRawResId = 0;
        setDataInitialized(false);
        if (mState != State.Released)
        {
            mPlayer.setDisplay(null);
        }
        stopProgressTimer();
    }

    /**
     * 通知异常
     *
     * @param e
     */
    private void notifyException(Exception e)
    {
        if (mOnExceptionCallback != null)
        {
            mOnExceptionCallback.onException(e);
        }
    }

    private void startProgressTimerIfNeed()
    {
        stopProgressTimer();

        if (mOnProgressCallback == null)
        {
            //未设置回调不需要启动Timer
            return;
        }

        if (mState == State.Playing)
        {
            if (mProgressTimer == null)
            {
                mProgressTimer = new CountDownTimer(Long.MAX_VALUE, 250)
                {
                    @Override
                    public void onTick(long millisUntilFinished)
                    {
                        notifyProgressCallback();
                    }

                    @Override
                    public void onFinish()
                    {
                    }
                };
                mProgressTimer.start();
            }
        }
    }

    private void stopProgressTimer()
    {
        if (mProgressTimer != null)
        {
            mProgressTimer.cancel();
            mProgressTimer = null;
        }
    }

    private void notifyProgressCallback()
    {
        if (mOnProgressCallback != null)
        {
            mOnProgressCallback.onProgress(getCurrentPosition(), getDuration(), SDMediaPlayer.this);
        }
    }

    //----------listener start----------
    /**
     * 错误监听
     */
    private OnErrorListener mInternalOnErrorListener = new OnErrorListener()
    {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra)
        {
            resetPlayer();
            notifyException(new RuntimeException(mp + ":" + String.valueOf(what) + "," + extra));
            return true;
        }
    };

    /**
     * 准备监听
     */
    private MediaPlayer.OnPreparedListener mInternalOnPreparedListener = new MediaPlayer.OnPreparedListener()
    {
        @Override
        public void onPrepared(MediaPlayer mp)
        {
            setState(State.Prepared);
            start();

            if (mOnPreparedListener != null)
            {
                mOnPreparedListener.onPrepared(SDMediaPlayer.this);
            }
        }
    };

    /**
     * 播放结束监听
     */
    private MediaPlayer.OnCompletionListener mInternalOnCompletionListener = new MediaPlayer.OnCompletionListener()
    {
        @Override
        public void onCompletion(MediaPlayer mp)
        {
            setState(State.Completed);

            if (mOnCompletionListener != null)
            {
                mOnCompletionListener.onCompletion(SDMediaPlayer.this);
            }
        }
    };

    /**
     * 视频宽高发生变化
     */
    private MediaPlayer.OnVideoSizeChangedListener mInternalOnVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener()
    {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height)
        {
            if (mOnVideoSizeChangedListener != null)
            {
                mOnVideoSizeChangedListener.onVideoSizeChanged(width, height, SDMediaPlayer.this);
            }
        }
    };

    //----------listener end----------

    public enum State
    {
        /**
         * 已经释放资源
         */
        Released,
        /**
         * 空闲，还没设置dataSource
         */
        Idle,
        /**
         * 已经设置dataSource，还未播放
         */
        Initialized,
        /**
         * 准备中
         */
        Preparing,
        /**
         * 准备完毕
         */
        Prepared,
        /**
         * 已经启动播放
         */
        Playing,
        /**
         * 已经暂停播放
         */
        Paused,
        /**
         * 已经播放完毕
         */
        Completed,
        /**
         * 调用stop方法后的状态
         */
        Stopped;
    }

    public interface OnStateChangeCallback
    {
        /**
         * 播放器状态发生变化回调
         *
         * @param oldState
         * @param newState
         * @param player
         */
        void onStateChanged(State oldState, State newState, SDMediaPlayer player);
    }

    public interface OnExceptionCallback
    {
        /**
         * 异常回调
         *
         * @param e
         */
        void onException(Exception e);
    }

    public interface OnVideoSizeChangedListener
    {
        /**
         * 视频宽高发生变化回调
         *
         * @param width
         * @param height
         * @param player
         */
        void onVideoSizeChanged(int width, int height, SDMediaPlayer player);
    }

    public interface OnCompletionListener
    {
        /**
         * 播放完毕回调
         *
         * @param player
         */
        void onCompletion(SDMediaPlayer player);
    }

    public interface OnPreparedListener
    {
        /**
         * 准备完毕回调
         *
         * @param player
         */
        void onPrepared(SDMediaPlayer player);
    }

    public interface OnProgressCallback
    {
        /**
         * 播放进度回调
         *
         * @param currentPosition 当前播放进度（毫秒）
         * @param totalDuration   总时长（毫秒）
         * @param player
         */
        void onProgress(int currentPosition, int totalDuration, SDMediaPlayer player);
    }
}
