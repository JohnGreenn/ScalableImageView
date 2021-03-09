package com.xq.customviewtouchscalableimageview.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import com.xq.customviewtouchscalableimageview.dp
import com.xq.customviewtouchscalableimageview.getAvatar
import kotlin.math.max
import kotlin.math.min

/**
 * 描述：
 * fileName：com.xq.customviewtouchscalableimageview.view
 * author：GLQ
 * time：2021/03/02 15:54
 */

private val IMAGE_SIZE = 300.dp.toInt()
private val EXTRA_SCALE_FRACTION = 1.5f //额外放缩系数
class ScalableImageView(context: Context?, attrs: AttributeSet?) : View(context, attrs){
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmap = getAvatar(resources, 300.dp.toInt())
    private var offsetX = 0f
    private var offsetY = 0f
    private var originalOffsetX = 0f //初始偏移
    private var originalOffsetY = 0f
    private var smallScale = 0f
    private var bigScale = 0f
    private val geGestureListener = GeGestureListener()
    private val geScaleGestureListener = GeScaleGestureListener()
    private val geFlingRunner = GeFlingRunner()
    private val getstureDetector = GestureDetectorCompat(context, geGestureListener)
    private val scaleGestureDetector = ScaleGestureDetector(context,geScaleGestureListener)
    private var big = false
    //private var scaleFraction = 0f //属性动画放缩比

    private var currentScale = 0f
        set(value) {
            field = value
            invalidate()
        }
    private val scaleAnimator = ObjectAnimator.ofFloat(this, "currentScale", smallScale, bigScale) //起始值0，结束值1 可以正着反着来
    private val scroller = OverScroller(context) //帮助计算坐标

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        originalOffsetX = (width - IMAGE_SIZE) / 2f
        originalOffsetY = (height - IMAGE_SIZE) / 2f

        if (bitmap.width / bitmap.height.toFloat() > width / height.toFloat()) {
            smallScale = width / bitmap.width.toFloat()
            bigScale = height / bitmap.height.toFloat() * EXTRA_SCALE_FRACTION
        } else {
            smallScale = height / bitmap.height.toFloat()
            bigScale = width / bitmap.width.toFloat() * EXTRA_SCALE_FRACTION
        }
        currentScale = smallScale
        scaleAnimator.setFloatValues(smallScale, bigScale)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGestureDetector.onTouchEvent(event) //双指
        //scaleGestureDetector.isInProgress 是否双指操作
        if(!scaleGestureDetector.isInProgress) {
            return getstureDetector.onTouchEvent(event) //双击
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaleFraction = (currentScale - smallScale) / (bigScale - smallScale)

        canvas.translate(
            offsetX * scaleFraction,
            offsetY * scaleFraction
        ) //*scaleFraction 会有双击缩小动画效果，不生硬
        val scale = smallScale + (bigScale - smallScale) * scaleFraction  //初始值 +  差值 * 缩放比
        //val scale = if (big) bigScale else smallScale
        canvas.scale(scale, scale, width / 2f, height / 2f)
        canvas.drawBitmap(bitmap, originalOffsetX, originalOffsetY, paint)

        //双击

    }

    /**
     * 边缘修正
     */
    private fun fixOffsets() {
        offsetX = min(offsetX, (bitmap.width * bigScale - width) / 2)
        offsetX = max(offsetX, -(bitmap.width * bigScale - width) / 2) //边界
        offsetY = min(offsetY, (bitmap.height * bigScale - height) / 2)
        offsetY = max(offsetY, -(bitmap.height * bigScale - height) / 2)

    }


    //简化
    inner class GeGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(p0: MotionEvent?): Boolean {
            return true
        }

        //点击监听的替代
        override fun onSingleTapUp(p0: MotionEvent?): Boolean {
            return false
        }

        //手指移动时反复回调
        override fun onScroll(
            downEvent: MotionEvent?,
            currentEvent: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (big) {
                offsetX -= distanceX
                offsetY -= distanceY
                fixOffsets()
                invalidate()
            }

            return false
        }

        //长按
        override fun onLongPress(p0: MotionEvent?) {
        }

        //滑的比较快，手松开就会触发(惯性滑动)
        override fun onFling(
            downEvent: MotionEvent?,
            currentEvent: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (big) {
                scroller.fling(
                    offsetX.toInt(),
                    offsetY.toInt(),
                    velocityX.toInt(),
                    velocityY.toInt(),
                    (-(bitmap.width * bigScale - width) / 2).toInt(),
                    ((bitmap.width * bigScale - width) / 2).toInt(),
                    (-(bitmap.height * bigScale - height) / 2).toInt(),
                    ((bitmap.height * bigScale - height) / 2).toInt(),
                    80.dp.toInt(),
                    80.dp.toInt()
                )
                ViewCompat.postOnAnimation(this@ScalableImageView, geFlingRunner) //把runnable推到主线程
            }

            return false
        }


        override fun onDoubleTap(e: MotionEvent): Boolean {
            big = !big
            if (big) {
                /**
                 * 按照你点击的位置放大
                 */
                //(e.x - width / 2f, e.y - width / 2f)放大前的坐标
                //bigScale / smallScale -1 比率差值
                //放大后的坐标
                offsetX = (e.x - width / 2f) * (1 - bigScale / smallScale)
                offsetY = (e.y - height / 2f) * (1 - bigScale / smallScale)
                fixOffsets() //边缘修正
                //从小变大动画
                scaleAnimator.start()
            } else {
                /**
                 * 双击缩小，但是会导致生硬 因为直接缩小了，没有动画
                offsetX = 0f
                offsetY = 0f
                 */
                scaleAnimator.reverse()
            }
            //invalidate() //动画就不需要了
            return true
        }

    }

    inner class GeScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener{

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val tempCurrentScale = currentScale * detector.scaleFactor
            if(tempCurrentScale < smallScale || tempCurrentScale> bigScale){
                return false
            } else {

                currentScale *= detector.scaleFactor //0 1; 0 无穷
                //双指放大缩小的范围
                //currentScale = currentScale.coerceAtLeast(smallScale).coerceAtMost(bigScale) //coerceAtLeast kotlin用法  相当于min
                return true
            }

        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            //根据你的落点放大缩小 (detector.focusX,detector.focusY) 落点坐标
            offsetX = (detector.focusX - width / 2f) * (1 - bigScale / smallScale)
            offsetY = (detector.focusY - height / 2f) * (1 - bigScale / smallScale)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {

        }

    }


    inner class GeFlingRunner : Runnable {
        override fun run() {
            if (scroller.computeScrollOffset()) {
                offsetX = scroller.currX.toFloat()
                offsetY = scroller.currY.toFloat()
                invalidate()
                ViewCompat.postOnAnimation(this@ScalableImageView, this)
            }
        }
    }



}