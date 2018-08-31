# About
在刚开始使用android中的MediaPlayer的时候由于对整个播放的流程不太熟悉会经常在不合适的状态下调用了某个方法导致异常，这个库对MediaPlayer
进行了封装，内部会处理各种状态下的是否合法调用处理，并且定义了播放的各种状态，使用起来更方便

## Gradle
[![](https://jitpack.io/v/zj565061763/player.svg)](https://jitpack.io/#zj565061763/player)

## 常用方法
```java
mPlayer.setOnStateChangeCallback(new FMediaPlayer.OnStateChangeCallback()
{
    @Override
    public void onStateChanged(FMediaPlayer.State oldState, FMediaPlayer.State newState, FMediaPlayer player)
    {
        //状态变化回调
    }
});
mPlayer.setOnProgressCallback(new FMediaPlayer.OnProgressCallback()
{
    @Override
    public void onProgress(int currentPosition, int totalDuration, FMediaPlayer player)
    {
        //设置播放进度回调，每隔250毫秒触发一次此方法，用于更新播放进度
    }
});
mPlayer.setDataRawResId(R.raw.cbg, this); //设置要播放的数据
mPlayer.setDataPath("http://xxx.xxx.mp4"); //设置在线视频地址或者本地文件路径

mPlayer.start(); //播放
mPlayer.pause(); //暂停
mPlayer.stop(); //停止
mPlayer.performPlayPause(); // 调用此方法会在 播放和暂停之间切换
mPlayer.performPlayStop(); //调用此方法会在 播放和停止之间切换

mPlayer.setLooping(true); //循环播放
mPlayer.getDuration(); //返回总时长（毫秒）
mPlayer.getCurrentPosition(); //返回当前播放的进度位置（毫秒）
mPlayer.setDisplay(sfv_media.getHolder()); //设置SurfaceHolder
mPlayer.setVolume(1, 1); //设置左右声道音量
mPlayer.getVideoWidth(); //返回视频宽度
mPlayer.getVideoHeight(); //返回视频高度

mPlayer.reset(); //重置
mPlayer.release(); //释放，释放后如果需要重新使用需要调用init()方法重新初始化
```
## 播放器状态
```java
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
```
