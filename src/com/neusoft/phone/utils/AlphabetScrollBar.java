package com.neusoft.phone.utils;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

//画右边的字母
public class AlphabetScrollBar extends View{
	
	private Paint mpaint = new Paint();
	private String[] mAlphabet = new String[]{
			"A", "B", "C", "D", "E", "F", "G","H", "I", "J", "K", "L", "M", "N", "O", "P", "Q",
			"R", "S", "T", "U", "V", "W", "X", "Y", "Z"
	};
	private boolean mPressed;
	private int mCurPosIdx = -1;
	private int mOldPosIdx =-1;
	private OnTouchBarListener mTouchListener;
	private TextView LetterNotice;
	
	//构造函数
	public AlphabetScrollBar(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	public AlphabetScrollBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public AlphabetScrollBar(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	
	public void setTextView(TextView LetterNotice) {
		this.LetterNotice = LetterNotice;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		
		int width = this.getWidth();//获取宽度
		int height = this.getHeight();
		
		//单页显示的长度
		int singleLetterH = height/mAlphabet.length;
		
		if (mPressed) {
			//如果处于按下状态，改变背景及相应字体的颜色
			canvas.drawColor(Color.parseColor("#40000000"));
		}
		
		for (int i = 0; i < mAlphabet.length; i++) {
			//设置画笔的颜色
			mpaint.setColor(Color.parseColor("#FFFFFF"));
			//* 设置是否使用抗锯齿功能，会消耗较大资源，绘制图形速度会变慢。 
			mpaint.setAntiAlias(true);
			mpaint.setTextSize(23);
			
			float x = width/2 - mpaint.measureText(mAlphabet[i])/2;
			float y = singleLetterH*i+singleLetterH;
			
			if (i == mCurPosIdx) {
				mpaint.setColor(Color.parseColor("#0000FF"));
				mpaint.setFakeBoldText(true);
				
			}
			canvas.drawText(mAlphabet[i], x, y, mpaint);
			mpaint.reset();
			
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		// TODO Auto-generated method stub
		int action = arg0.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mPressed = true;
			mCurPosIdx = (int) (arg0.getY()/this.getHeight() * mAlphabet.length);
			if(mTouchListener != null && mOldPosIdx!=mCurPosIdx){
				if((mCurPosIdx>=0) && (mCurPosIdx<mAlphabet.length)) {
					mTouchListener.onTouch(mAlphabet[mCurPosIdx]);
					this.invalidate();
				}
				mOldPosIdx = mCurPosIdx;
			}
			
			LetterNotice.setText(mAlphabet[mCurPosIdx]);
			LetterNotice.setVisibility(View.VISIBLE);
			
			return true;
		case MotionEvent.ACTION_UP:
			
			if (LetterNotice != null) {
				LetterNotice.setVisibility(View.INVISIBLE);
			}
			
			mPressed = false;
			mCurPosIdx = -1;
			this.invalidate();
			return true;
		case MotionEvent.ACTION_MOVE:
			mCurPosIdx =(int)( arg0.getY()/this.getHeight() * mAlphabet.length);
			if(mTouchListener != null && mCurPosIdx!=mOldPosIdx){
				if((mCurPosIdx>=0) && (mCurPosIdx<mAlphabet.length)) {
					mTouchListener.onTouch(mAlphabet[mCurPosIdx]);
					this.invalidate();
				}
				mOldPosIdx = mCurPosIdx;
			}
			
			if(mCurPosIdx >= 0 && mCurPosIdx < mAlphabet.length)
			{
				LetterNotice.setText(mAlphabet[mCurPosIdx]);
				LetterNotice.setVisibility(View.VISIBLE);
			}
			
			return true;
		default:
			return super.onTouchEvent(arg0);
			

		
		}
		
	}
	
	
	
	/**
	 * 接口
	 */
	public static interface OnTouchBarListener {
		void onTouch(String letter);
	}
	
	/**
	 * 向外公开的方法
	 */
	public void setOnTouchBarListener (OnTouchBarListener listener) {
		mTouchListener = listener;
	}

}
