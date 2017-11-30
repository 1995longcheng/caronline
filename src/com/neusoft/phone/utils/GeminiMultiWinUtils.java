package com.neusoft.phone.utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import com.neusoft.phonedemo.R;

public final class GeminiMultiWinUtils {

    private static final int MSG_SHOW_BANNER = 1;

	private static void alertWindow(Context ctx, View view, int grivity, int x,
			int y, int width, int height) {
		if (ctx == null || view == null)
			return;
		WindowManager mWm = (WindowManager) ctx
				.getSystemService(Context.WINDOW_SERVICE);

		LayoutParams wmParams = new WindowManager.LayoutParams();
		// 设置window type
		wmParams.type = LayoutParams.TYPE_PHONE;
		// 设置图片格式，效果为背景透明
		wmParams.format = PixelFormat.RGBA_8888;
		// 设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
		wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
		// 调整悬浮窗显示的停靠位置为左侧置顶
		wmParams.gravity = grivity;
		// 以屏幕左上角为原点，设置x、y初始值，相对于gravity
		wmParams.x = x;
		wmParams.y = y;
		// 设置悬浮窗口长宽数据
		wmParams.width = width/* WindowManager.LayoutParams.WRAP_CONTENT */;
		wmParams.height = height/* WindowManager.LayoutParams.WRAP_CONTENT */;

		try {
			mWm.addView(view, wmParams);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    public static void showRejecteBanner(final Context ctx, String name) {
        if (ctx == null)
            return;
        View view = LayoutInflater.from(ctx).inflate(R.layout.banner, null);
        TextView tv = (TextView) view.findViewById(R.id.alert_textview);
        String contactName = (String) TextUtils.ellipsize(name, tv.getPaint(), 280,
                TextUtils.TruncateAt.END);
        showBanner(ctx, String.format(ctx.getResources().
                getString(R.string.rejected_call_with_sms_text), contactName));
    }

    public static void showBanner(final Context ctx, String message) {
        if (ctx == null)
            return;
        final WindowManager mWm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater l = LayoutInflater.from(ctx);

        final View view = l.inflate(R.layout.banner, null);
        TextView tv = (TextView) view.findViewById(R.id.alert_textview);
        tv.setText(message);

        alertWindow(ctx, view, Gravity.LEFT | Gravity.TOP, 0, 0, 768, 1024);

        final Runnable callback = new Runnable() {
            public void run() {
                try {
                    mWm.removeView(view);
                } catch (IllegalArgumentException e) {
                    Log.i("GeminiMultiWinUtils", "exception in callback");
                    e.printStackTrace();
                }
            }
        };
        mHandler.postDelayed(callback, 1000 * 6);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOW_BANNER, callback));
        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(callback);
                try {
                    mWm.removeView(v);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Log.e("GeminiMultiWinUtils", "IllegalArgumentException is " + e);
                }
            }
        });
    }

	public static void showBanner(final Context ctx, String message, int time) {
		if (ctx == null)
			return;
		final WindowManager mWm = (WindowManager) ctx
				.getSystemService(Context.WINDOW_SERVICE);

		LayoutInflater l = LayoutInflater.from(ctx);

		final View view = l.inflate(R.layout.banner, null);
		TextView tv = (TextView) view.findViewById(R.id.alert_textview);
		tv.setText(message);

		alertWindow(ctx, view, Gravity.LEFT | Gravity.TOP, 0, 0, 768, 1024);

		final Handler handler = new Handler();
		final Runnable callback = new Runnable() {
			public void run() {
				try {
					mWm.removeView(view);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		};
		handler.postDelayed(callback, 1000 * time);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				handler.removeCallbacks(callback);
				mWm.removeView(v);
			}
		});
	}

	public static void dismissWindow(Context ctx, View view) {
		if (ctx != null && view != null) {
			WindowManager mWm = (WindowManager) ctx
					.getSystemService(Context.WINDOW_SERVICE);
			try {
				mWm.removeView(view);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    private static Handler mHandler = new Handler() {
        Runnable callback;
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SHOW_BANNER) {
                if (callback != null) {
                    removeCallbacks(callback);
                    post(callback);
                }
                callback = (Runnable) msg.obj;
            }
        }
    };
}
