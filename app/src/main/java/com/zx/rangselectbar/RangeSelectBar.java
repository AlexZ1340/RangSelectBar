package com.zx.rangselectbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangxuan on 2018/5/15.<br/>
 * 范围选择控件<br/>
 * 支持单滑块或者双滑块<br/>
 * 双滑块时，滑块不可重合，单滑块可以重合<br/>
 * 滑块支持bitmap，shape，color，推荐使用shape<br/>
 */

public class RangeSelectBar extends View {
    Context mContext;
    // 画笔
    Paint paint;

    // 长条选中部分颜色
    private int barSelectColor;
    // 未选中部分颜色，即背景色
    private int barBackground;
    // 长条的高度
    private float barHeight;
    // 长条的上下左右位置
    private float barTop;
    private float barBottom;
    private float barLeft;
    private float barRight;
    // 计算后，长条中心点位置
    private float barCenter;

    // 长条上的滑块，滑块有两个
    private Slider leftSlider;
    private Slider rightSlider;
    private float halfSliderWidth;
    private float halfSliderHeight;
    private Drawable sliderDrawable;
    private Bitmap sliderBitmap;
    private boolean hideLeftSlider;
    private boolean hideRightSlider;

    // 左右滑块的默认位置，主要用于预览
    private int leftSliderPosition;
    private int rightSliderPosition;

    // 长条上的圆形点点的属性
    private float pointRadius;
    private int pointColors;

    // 长条与滑块中，高度较大的一个
    private float lagerBetweenTheTwo;

    // 主体与下方文字的间隙
    private float barBottomPadding;

    // 长条下方的文字属性
    private float textSize;
    @IdRes
    private int textColor;

    // 长条分段
    private float pointLength;
    private List<Float> pointlist;

    // 默认文字
    private CharSequence[] textList = {"1", "2", "3", "4", "5"};
    // 文字数量就是点的数量
    private int textNumber;

    public RangeSelectBar(Context context) {
        this(context, null);
    }

    public RangeSelectBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray t = context.obtainStyledAttributes(attrs, R.styleable.RangeSelectBar);

        // 进度条下方的文字，因为要用，所以先获取
        textList = t.getTextArray(R.styleable.RangeSelectBar_RSBHintTextList);
        // 获取长条相关属性
        barHeight = t.getDimension(R.styleable.RangeSelectBar_RSBHeight, 4);
        barSelectColor = t.getColor(R.styleable.RangeSelectBar_RSBSelectColor, getResources().getColor(R.color.colorPrimary));
        barBackground = t.getColor(R.styleable.RangeSelectBar_RSBBackgroundColor, getResources().getColor(R.color.gray_word_light));
        // 滑块资源文件
        sliderDrawable = t.getDrawable(R.styleable.RangeSelectBar_RSBSliderRes);
        leftSliderPosition = t.getInt(R.styleable.RangeSelectBar_RSBLeftSliderPosition, 0);
        rightSliderPosition = t.getInt(R.styleable.RangeSelectBar_RSBRightSliderPosition, textList.length - 1);
        hideLeftSlider = t.getBoolean(R.styleable.RangeSelectBar_RSBHideLeftSlide, false);
        hideRightSlider = t.getBoolean(R.styleable.RangeSelectBar_RSBHideRightSlide, false);
        // 长条上的圆形
        pointRadius = t.getDimension(R.styleable.RangeSelectBar_RSBPointRadius, 4);
        pointColors = t.getColor(R.styleable.RangeSelectBar_RSBPointColor, getResources().getColor(R.color.white));
        // 主体与文字的间隙
        barBottomPadding = t.getDimension(R.styleable.RangeSelectBar_RSBBottomPadding, 4);

        // 获取文字属性
        textSize = t.getDimension(R.styleable.RangeSelectBar_RSBTextSize, 14);
        textColor = t.getColor(R.styleable.RangeSelectBar_RSBTextColor, getResources().getColor(R.color.black_word));

        t.recycle();
        init(context);
    }

    public RangeSelectBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        // 初始化滑块
        leftSlider = new Slider();
        rightSlider = new Slider();

        // 进度条上点的数量
        textNumber = textList.length;
        // 初始化画笔
        paint = new Paint();
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        // 确定滑块的样子以及宽高
        sliderBitmap = drawableToBitmap(sliderDrawable);
        leftSlider.setSliderSize(sliderBitmap);
        rightSlider.setSliderSize(sliderBitmap);

        // 滑块和者进度条，两者较大的一个，作为这个整体的高度
        lagerBetweenTheTwo = leftSlider.height > barHeight ? leftSlider.height : barHeight;

        // 设置隐藏状态，只能有一个被隐藏
        if (hideLeftSlider) {
            leftSlider.isShow = false;
            rightSlider.isShow = true;
        }
        if (hideRightSlider) {
            rightSlider.isShow = false;
            leftSlider.isShow = true;
        }

        setRange(leftSliderPosition, rightSliderPosition);
    }

    /**
     * 设置选中的范围，最大值为点的数量，最小值为0
     *
     * @param positionLeft   第一个滑块的角标
     * @param postitionRight 第二个滑块的角标
     */
    public void setRange(int positionLeft, int postitionRight) {
        // 先将其控制在安全的范围之内
        if (positionLeft >= textNumber) positionLeft = textNumber - 1;
        if (postitionRight >= textNumber) postitionRight = textNumber - 1;
        if (positionLeft < 0) positionLeft = 0;
        if (postitionRight < 0) postitionRight = 0;

        // 将其设置给滑块，然后重绘
        leftSlider.position = positionLeft;
        rightSlider.position = postitionRight;

        // 确定滑块中心位置
        leftSlider.center = barLeft + pointLength * leftSlider.position;
        rightSlider.center = barLeft + pointLength * rightSlider.position;

        if (mListener != null) mListener.onRangeSelected(leftSlider.position, rightSlider.position);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        if (heightSpecMode == MeasureSpec.AT_MOST) {
            heightSpecSize = (int) (lagerBetweenTheTwo + barBottomPadding + textSize + getPaddingTop() + getPaddingBottom());
            setMeasuredDimension(widthSpecSize, heightSpecSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        /*
        此时，控件被测量完毕，开始计算放置位置
        控件总共由三部分组成，从上到下依次是
        控件主体，长条加滑块的一个组合，取其中高度最高者
        控件主体与下方文字间隙的高度
        下方文字高度
         */
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int viewWidth = measuredWidth - getPaddingLeft() - getPaddingRight();
        int viewHeight = measuredHeight - getPaddingTop() - getPaddingBottom();

        // 先计算出空间一共需要的高度，来计算出空间主体的长条的中心点位置
        float heightTotal = lagerBetweenTheTwo + barBottomPadding + textSize;

        // 求出长条的中心位置，
        barCenter = (viewHeight - heightTotal + lagerBetweenTheTwo) / 2 + getPaddingTop();

        // 计算长条的各种位置信息
        halfSliderWidth = leftSlider.width / 2;
        halfSliderHeight = leftSlider.height / 2;

        barTop = barCenter - barHeight / 2;
        barBottom = barCenter + barHeight / 2;
        barLeft = getPaddingLeft() + halfSliderWidth;
        barRight = measuredWidth - getPaddingRight() - halfSliderWidth;

        // 初始化分段
        pointLength = (viewWidth - leftSlider.width) / (textNumber - 1);
        pointlist = new ArrayList<>();
        for (int i = 0; i < textNumber; i++) {
            pointlist.add(barLeft + pointLength * i);
        }

        // 确定滑块中心位置
        leftSlider.center = barLeft + pointLength * leftSlider.position;
        rightSlider.center = barLeft + pointLength * rightSlider.position;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 计算滑块的绘制信息
        leftSlider.left = leftSlider.center - halfSliderWidth;
        leftSlider.right = leftSlider.center + halfSliderWidth;
        leftSlider.top = barCenter - halfSliderHeight;
        leftSlider.bottom = barCenter + halfSliderHeight;

        rightSlider.left = rightSlider.center - halfSliderWidth;
        rightSlider.right = rightSlider.center + halfSliderWidth;
        rightSlider.top = barCenter - halfSliderHeight;
        rightSlider.bottom = barCenter + halfSliderHeight;

        // 画进度条底色
        paint.setColor(barBackground);
        canvas.drawRect(barLeft, barTop, barRight, barBottom, paint);
        canvas.drawCircle(barLeft, barCenter, barHeight / 2, paint);
        canvas.drawCircle(barRight, barCenter, barHeight / 2, paint);

        // 画进度条选中部分
        paint.setColor(barSelectColor);
        canvas.drawRect(leftSlider.center, barTop, rightSlider.center, barBottom, paint);
        canvas.drawCircle(leftSlider.center, barCenter, barHeight / 2, paint);
        canvas.drawCircle(rightSlider.center, barCenter, barHeight / 2, paint);

        // 画长条上的白点点，还有写字
        for (int i = 0; i < textNumber; i++) {

            float cx = pointlist.get(i);
            float cy = barCenter + lagerBetweenTheTwo / 2 + textSize + barBottomPadding;

            paint.setColor(pointColors);
            canvas.drawCircle(cx, barCenter, pointRadius, paint);

            paint.setColor(textColor);
            canvas.drawText(textList[i].toString(), cx - (paint.measureText(textList[i].toString()) / 2), cy, paint);
        }

        // 画滑块
        if (leftSlider.isShow)
            canvas.drawBitmap(sliderBitmap, leftSlider.left, leftSlider.top, paint);
        if (rightSlider.isShow)
            canvas.drawBitmap(sliderBitmap, rightSlider.left, rightSlider.top, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                if (isTouchSlider(leftSlider, event) && leftSlider.isShow) {
                    leftSlider.isTouching = true;
                } else if (isTouchSlider(rightSlider, event) && rightSlider.isShow) {
                    rightSlider.isTouching = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (leftSlider.isTouching) {
                    updateTouch(leftSlider, event);
                } else if (rightSlider.isTouching) {
                    updateTouch(rightSlider, event);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (leftSlider.isTouching) {
                    autoScroll(leftSlider);
                } else if (rightSlider.isTouching) {
                    autoScroll(rightSlider);
                }
                rightSlider.isTouching = false;
                leftSlider.isTouching = false;
                invalidate();
                break;
        }
        return true;
    }

    /**
     * 判断是否触摸某个 Slider
     **/
    private boolean isTouchSlider(Slider slider, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        // 增加触摸响应范围
        return x >= slider.left - slider.width / 2
                && x <= slider.right + slider.width / 2
                && y >= slider.top - slider.height / 2
                && y <= slider.bottom + slider.height / 2;
    }

    private void updateTouch(Slider slider, MotionEvent event) {
        float x = event.getX();
        // 如果两个滑块都显示的话，就需要控制两个滑块不能重合
        if (leftSlider.isShow && rightSlider.isShow)
            if (leftSlider.isTouching) {
                // 当前是左边 slider， 判断是否越过右边 slider，如果是，不进行赋值操作
                if ((rightSlider.center - x) < pointLength) {
                    slider.center = rightSlider.center - pointLength;
                    invalidate();
                    return;
                }
            } else if (rightSlider.isTouching) {
                // 当前是左边 slider， 判断是否越过右边 slider，如果是，不进行赋值操作
                if ((x - leftSlider.center) < pointLength) {
                    slider.center = leftSlider.center + pointLength;
                    invalidate();
                    return;
                }
            }
        if (x <= barLeft) {
            slider.center = barLeft;
        }
        if (x >= barRight) {
            slider.center = barRight;
        }
        if (x > barLeft && x < barRight) {
            slider.center = x;
        }
        invalidate();
    }

    /**
     * 每次松开手指，自动滚动到区间点
     *
     * @param slider
     */
    private void autoScroll(Slider slider) {
        float distance = slider.center - getPaddingLeft() - halfSliderWidth;
        int position = (int) (distance / pointLength);// 除数
        float remainder = distance % pointLength;// 余数
        if (remainder <= (pointLength / 2)) {
            slider.position = position;
            slider.center = pointlist.get(position);
        } else {
            int index = position + 1;
            slider.position = index;
            slider.center = pointlist.get(index);
        }
        Log.e("autoScroll: ", "__" + leftSlider.position + "," + rightSlider.position);
        if (mListener != null) mListener.onRangeSelected(leftSlider.position, rightSlider.position);

    }

    public Bitmap drawableToBitmap(Drawable drawable) {
        // 取 drawable 的长宽
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w < 0) w = dp2px(10);
        if (h < 0) h = dp2px(10);

        // 取 drawable 的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        // 建立对应 bitmap
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        // 把 drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }

    // dp px转换
    public int dp2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    // 监听器
    private OnRangeSelectedListener mListener;

    public void setOnRangeSelectedListener(OnRangeSelectedListener l) {
        this.mListener = l;
    }

    public interface OnRangeSelectedListener {
        /**
         * return selected position. 返回选中的位置
         *
         * @param left
         * @param right
         */
        void onRangeSelected(int left, int right);
    }

    /**
     * 滑块
     */
    private class Slider {
        public Slider() {
            isShow = true;
        }

        public void setSliderSize(Bitmap bitmap) {
            if (bitmap != null) {
                height = bitmap.getHeight();
                width = bitmap.getWidth();
            } else {
                height = 0;
                width = 0;
            }
        }

        // 交互信息
        public boolean isTouching;
        public boolean isShow;
        // 坐标信息，代表滑块静止时候的位置
        public int position;
        // 实际控制滑块滑动时候的位置
        public float center;
        public float top;
        public float right;
        public float bottom;
        public float left;
        // 尺寸信息
        public float height;
        public float width;
    }
}
