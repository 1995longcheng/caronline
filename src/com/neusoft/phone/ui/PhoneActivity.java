package com.neusoft.phone.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.neusoft.c3alfus.bluetooth.phone.CallManager;

import com.neusoft.phone.listener.OnPbapSyncLinstener;
import com.neusoft.phone.manager.ContactsManager;
import com.neusoft.phone.manager.PbapLoadManager;
import com.neusoft.phone.model.ContactInfo;
import com.neusoft.phone.model.ContactsPhones;
import com.neusoft.phone.utils.AlphabetScrollBar;
import com.neusoft.phone.utils.PhoneUtils;
import com.neusoft.phone.utils.PinyinUtils;
import com.neusoft.phonedemo.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.NeusoftContactsContract;
import android.provider.Settings;

public class PhoneActivity extends Activity implements
		android.view.View.OnClickListener {

	private static final String TAG = "PhoneActivity";// 声明TAG，进行调试输出提示。
	protected static final int UPDATE_CONTACT_OVER = 0;

	private PhoneUtils mPhoneUtils;
	private PbapLoadManager mPbapLoadManager;
	private List<ContactInfo> mContactList;//联系人信息，ContactInfo联系人实体 
	private CallManager mCallManager;
	private boolean canDial = false;

	// 写viewpage
	private ViewPager mViewPager;// 用来放置界面切换
	private PagerAdapter mPagerAdapter;// 初始化View适配器
	private List<View> mViews = new ArrayList<View>();// 用来存放Tab01-03
	// 三个tab，每个tab包含一个按钮
	private LinearLayout mTabDianHua;
	private LinearLayout mTabLianXiRen;
	private LinearLayout mTabBoHao;

	// 三个按钮
	private Button mDianHuaImg;
	private Button mLianXiRen;
	private Button mBoHao;
	/**
	 * 通话记录
	 */
	// 写通话记录的viewpager
	private ViewPager mViewPager_tab01;// 用来放置界面切换
	private PagerAdapter mPagerAdapter_tab01;// 初始化View适配器
	private List<View> mViews_tab01 = new ArrayList<View>();// 用来存放phone_Tab01-03
	// 三个phone_tab，每个tab包含一个按钮
	private LinearLayout mAllPhone;
	private LinearLayout mGonePhone;
	private LinearLayout mNeverPhone;
	// 三个文本
	private TextView mAllPhoneTv;
	private TextView mGonePhoneTv;
	private TextView mNeverPhoneTv;
	//三个listview
	private ListView all_phone_listview;
	private ListView gone_phone_listview;
	private ListView never_phone_listview;
	
	
	
//	//加载联系人
//	//联系人包含的信息
	public	 class Persons {
			public String Name;  //姓名
			public String PY;      //姓名拼音 (花花大神:huahuadashen)
			public String Number;      //电话号码
			public String FisrtSpell;      //中文名首字母 (花花大神:hhds)
		} 
		//字母列视图View
		private AlphabetScrollBar m_asb;
		//显示选中的字母
		private TextView m_letterNotice;
		//联系人的列表
		private ListView m_contactslist;
		//联系人列表的适配器
		private ListAdapter m_listadapter;
		//所有联系人数组
		private ArrayList<Persons> persons = new ArrayList<Persons>();
		//搜索过滤联系人EditText
		private EditText m_FilterEditText;
		//没有匹配联系人时显示的TextView
		private TextView m_listEmptyText;
		
		

	// 声明拨号键盘
	TextView tv;
	Button but0;
	ImageButton but1;
	ImageButton but2;
	ImageButton but3;
	ImageButton but4;
	ImageButton but5;
	ImageButton but6;
	ImageButton but7;
	ImageButton but8;
	ImageButton but9;
	ImageButton but10;
	ImageButton but11;
	ImageButton but12;
	ImageButton but13;//拨打键
	ImageButton but15;
	private ListView lv;
	private List<Map<String, String>> dataList;
	private List<Map<String, String>> dataListGone;
	private List<Map<String, String>> dataListNever;
	private SimpleAdapter adapter;//通话记录适配器
	private SimpleAdapter adapterGone;//通话记录适配器
	private SimpleAdapter adapterNever;//通话记录适配器
	private static final int DTMF_DURATION_MS = 120; // 声音的播放时间
	private Object mToneGeneratorLock = new Object(); // 监视器对象锁
	private ToneGenerator mToneGenerator; // 声音产生器
	private static boolean mDTMFToneEnabled; // 系统参数“按键操作音”标志位

	// 这是demo写好的
	private Handler mHander = new Handler() {

		private List<ContactsPhones> mPhoneList;

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case UPDATE_CONTACT_OVER:
				Log.d(TAG, "PhoneActivity receive updateContactsList over msg");
				mContactList = mContactsManager.getContactsList();
				Iterator<ContactInfo> iter = mContactList.iterator();
				//Iterator迭代器是一种设计模式，是一个对象，可以遍历并选择序列中的对象。
//				(1) 使用方法iterator()要求容器返回一个Iterator。第一次调用Iterator的next()方法时，
//				它返回序列的第一个元素。注意：iterator()方法是java.lang.Iterable接口,被Collection继承。
//
//				　　(2) 使用next()获得序列中的下一个元素。
//
//				　　(3) 使用hasNext()检查序列中是否还有元素。
//	info：信息，通知
//				　　(4) 使用remove()将迭代器新返回的元素删除。
				while (iter.hasNext()) {
					ContactInfo info = iter.next();
					mPhoneList = info.getPhones();
					Iterator<ContactsPhones> phoneiter = mPhoneList.iterator();
					Log.d(TAG, "name is " + info.getName());
					while (phoneiter.hasNext()) {
						ContactsPhones phones = phoneiter.next();
						Log.d(TAG, "phone is " + phones.getNumber());
					}
				}

				break;
			default:
				break;
			}
		}

	};
	//实时更新
	private OnPbapSyncLinstener mOnPbapSyncLinstener = new OnPbapSyncLinstener() {

		@Override
		public void onCallLogSyncStart() {
			// TODO Auto-generated method stub
			Log.d(TAG, "PhoneActivity onCallLogSyncStart");

		}

		@Override
		public void onCallLogSyncEnd() {
			// TODO Auto-generated method stub
			Log.d(TAG, "PhoneActivity onCallLogSyncEnd");
			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					mContactsManager.updateContactsList();
					Log.d(TAG, "PhoneActivity updateContactsList over");
					Message msg = mHander.obtainMessage(UPDATE_CONTACT_OVER);
					msg.sendToTarget();
				}

			}).start();
		}

		@Override
		public void onPhoneBookSyncStart() {
			// TODO Auto-generated method stub
			Log.d(TAG, "PhoneActivity onPhoneBookSyncStart");
		}

		@Override
		public void onPhoneBookSyncEnd() {
			// TODO Auto-generated method stub
			Log.d(TAG, "PhoneActivity onPhoneBookSyncEnd");
		}

	};
	private ContactsManager mContactsManager;
	private Button mCallButton;

	
	// 初始化活动
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		Log.d(TAG, "PhoneActivity onCreate");
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_phone);

		initView();
		initViewPage();
		initEvent();
		
		//老师写好的

		mPhoneUtils = PhoneUtils.getInstance(this);//分配内存给这个对象
		mPbapLoadManager = PbapLoadManager.getInstance(this);//pbap蓝牙同步管理
		mPbapLoadManager.setSyncListener(mOnPbapSyncLinstener);
		mContactsManager = ContactsManager.getInstance(this);//联系人管理

		/*
		 * mCallButton = (Button)findViewById(R.id.button1);
		 * mCallButton.setOnClickListener(new OnClickListener(){
		 * 
		 * @Override public void onClick(View arg0) { 
		 * // TODO Auto-generated method stub 
		 * 
		 * mPhoneUtils.dialTo("中国移动客服", "10086");
		 * mPhoneUtils.startCall(true); }
		 * 
		 * });
		 */

		
		 



	}
	
	/**
	 * 主页面的viewpager初始化设置
	 */
	private void initView() {
		// TODO Auto-generated method stub
		mViewPager = (ViewPager) findViewById(R.id.id_viewpage);
		// 初始化三个Linearlayout
		mTabDianHua = (LinearLayout) findViewById(R.id.id_tab_tonghuajilu);
		mTabLianXiRen = (LinearLayout) findViewById(R.id.id_tab_user);
		mTabBoHao = (LinearLayout) findViewById(R.id.id_tab_bohao);
		// 初始化三个按钮
		mDianHuaImg = (Button) findViewById(R.id.id_tab_tonghuajilu_img);
		mLianXiRen = (Button) findViewById(R.id.id_tab_user_img);
		mBoHao = (Button) findViewById(R.id.id_tab_number_img);
		}

	/**
	 * 初始化ViewPage
	 */
	private void initViewPage() {
		// TODO Auto-generated method stub
		// 初始化三个布局
		LayoutInflater mLayoutInflater = LayoutInflater.from(PhoneActivity.this);// 生成一个对象，用来动态加载view
		View tab01 = mLayoutInflater.inflate(R.layout.tab01, null);
		View tab02 = mLayoutInflater.inflate(R.layout.tab02, null);
		View tab03 = mLayoutInflater.inflate(R.layout.tab03, null);
		mViews.add(tab01);
		mViews.add(tab02);
		mViews.add(tab03);
		// viewpager适配器初始化并设置
		mPagerAdapter = new PagerAdapter() {

			// PagerAdapter只缓存三张要显示的图片，
			// 如果滑动的图片超出了缓存的范围，就会调用destroyItem这个方法，将图片销毁
			@Override
			public void destroyItem(ViewGroup container, int position,
					Object object) {
				container.removeView(mViews.get(position));

			}

			// 当要显示的图片可以进行缓存的时候，会调用这个方法进行显示图片的初始化，
			// 我们将要显示的ImageView加入到ViewGroup中，然后作为返回值返回即可
			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				View view = mViews.get(position);
				container.addView(view);
				return view;
			}

			// 获取要滑动的控件的数量
			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return mViews.size();
			}

			// 来判断显示的是否是同一张图片，这里我们将两个参数相比较返回即可
			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				// TODO Auto-generated method stub
				return arg0 == arg1;
			}
		};
		mViewPager.setAdapter(mPagerAdapter);//把数据放到控件中
		
		
		/**
		 *  通话记录页面
		 */
		mViewPager_tab01 = (ViewPager) tab01.findViewById(R.id.id_viewpage);
		// 三个phone_tab，每个tab包含一个按钮
		mAllPhone = (LinearLayout) tab01.findViewById(R.id.id_tab_all_phone);
		mGonePhone = (LinearLayout) tab01.findViewById(R.id.id_tab_gone_phone);
		mNeverPhone = (LinearLayout) tab01.findViewById(R.id.id_tab_never_phone);
		// 三个文本
		mAllPhoneTv = (TextView) tab01.findViewById(R.id.all_phone_tv);
		mGonePhoneTv = (TextView) tab01.findViewById(R.id.gone_phone_tv);
		mNeverPhoneTv = (TextView) tab01.findViewById(R.id.never_phone_tv);
		
		//初始化三个通话布局
		LayoutInflater mPhoneLayoutInflater = LayoutInflater.from(PhoneActivity.this);// 生成一个对象，用来动态加载view		
		View phone_tab01 = mPhoneLayoutInflater.inflate(R.layout.phone_tab01, null);
		View phone_tab02 = mPhoneLayoutInflater.inflate(R.layout.phone_tab02, null);
		View phone_tab03 = mPhoneLayoutInflater.inflate(R.layout.phone_tab03, null);
		mViews_tab01.add(phone_tab01);
		mViews_tab01.add(phone_tab02);
		mViews_tab01.add(phone_tab03);
		
		//初始化三个listview
		all_phone_listview = (ListView) phone_tab01.findViewById(R.id.all_phone_listview);
		gone_phone_listview = (ListView) phone_tab02.findViewById(R.id.gone_phone_listview);
		never_phone_listview = (ListView) phone_tab03.findViewById(R.id.never_phone_listview);
		
		mPagerAdapter_tab01 = new PagerAdapter() {
			
			// PagerAdapter只缓存三张要显示的图片，
			// 如果滑动的图片超出了缓存的范围，就会调用destroyItem这个方法，将图片销毁
			@Override
			public void destroyItem(ViewGroup container, int position,
					Object object) {
				container.removeView(mViews_tab01.get(position));

			}

			// 当要显示的图片可以进行缓存的时候，会调用这个方法进行显示图片的初始化，
			// 我们将要显示的ImageView加入到ViewGroup中，然后作为返回值返回即可
			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				View view = mViews_tab01.get(position);
				container.addView(view);
				return view;
			}
			
			// 来判断显示的是否是同一张图片，这里我们将两个参数相比较返回即可
			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				// TODO Auto-generated method stub
				return arg0 == arg1;
			}
			
			//获得列表的大小
			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return mViews_tab01.size();
			}
		};
		mViewPager_tab01.setAdapter(mPagerAdapter_tab01);
		
		
		// tab02联系人代码11
		// 获取手机中的联系人,并将所有联系人保存perosns数组中
		// 联系人比较多的话,初始化中会比较耗时,以后再优化
		getContacts();
		// 得到字母列的对象,并设置触摸响应监听器
		m_asb = (AlphabetScrollBar) tab02.findViewById(R.id.alphabetscrollbar);
		m_asb.setOnTouchBarListener(new ScrollBarListener());
		m_letterNotice = (TextView) tab02.findViewById(R.id.pb_letter_notice);
		m_asb.setTextView(m_letterNotice);
		// 根据拼音为联系人数组进行排序
		Collections.sort(persons, new ComparatorPY());
		// 得到联系人列表,并设置适配器
		m_contactslist = (ListView) tab02.findViewById(R.id.pb_listvew);
		m_listadapter = new ListAdapter(this, persons);
		m_contactslist.setAdapter(m_listadapter);
		m_listEmptyText = (TextView) tab02.findViewById(R.id.pb_nocontacts_notice);
		
		// 点击事件(20170911)
		m_contactslist.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				// TODO Auto-generated method stub
				Persons num = persons.get(position);
				mPhoneUtils.dialTo(num.Name, num.Number);
				mPhoneUtils.startCall(true);
				

			}
		});

		// 初始化搜索编辑框,设置文本改变时的监听器
		m_FilterEditText = (EditText) tab02.findViewById(R.id.pb_search_edit);
		m_FilterEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

				if (!"".equals(s.toString().trim())) {
					// 根据编辑框值过滤联系人并更新联系列表
					filterContacts(s.toString().trim());
					m_asb.setVisibility(View.GONE);
				} else {
					m_asb.setVisibility(View.VISIBLE);
					m_listadapter.updateListView(persons);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});
		//
		
		//tab03拨号界面的id
		lv = (ListView) tab03.findViewById(R.id.lv);
		tv = (TextView) tab03.findViewById(R.id.tv);
		but1 = (ImageButton) tab03.findViewById(R.id.but1);
		but2 = (ImageButton) tab03.findViewById(R.id.but2);
		but3 = (ImageButton) tab03.findViewById(R.id.but3);
		but4 = (ImageButton) tab03.findViewById(R.id.but4);
		but5 = (ImageButton) tab03.findViewById(R.id.but5);
		but6 = (ImageButton) tab03.findViewById(R.id.but6);
		but7 = (ImageButton) tab03.findViewById(R.id.but7);
		but8 = (ImageButton) tab03.findViewById(R.id.but8);
		but9 = (ImageButton) tab03.findViewById(R.id.but9);
		but10 = (ImageButton) tab03.findViewById(R.id.but10);
		but11 = (ImageButton) tab03.findViewById(R.id.but11);
		but12 = (ImageButton) tab03.findViewById(R.id.but12);
		but13 = (ImageButton) tab03.findViewById(R.id.but13);
		but15 = (ImageButton) tab03.findViewById(R.id.but15);
		
		/**
		 * 通话记录数据
		 */
		 dataList = getDataList();
		 dataListGone = getGoneDataList();
		 dataListNever = getNeverDataList();
		 adapter = new SimpleAdapter(this, dataList, R.layout.simple_calllog_item//
		        , new String[] { "name", "number", "date", "duration", "type" }//
		        , new int[] { R.id.tv_name, R.id.tv_number, R.id.tv_date, R.id.tv_duration, R.id.tv_type });
		 adapterGone = new SimpleAdapter(this, dataListGone, R.layout.simple_calllog_item//
			        , new String[] { "name", "number", "date", "duration", "type" }//
			        , new int[] { R.id.tv_name, R.id.tv_number, R.id.tv_date, R.id.tv_duration, R.id.tv_type });
		 adapterNever = new SimpleAdapter(this, dataListNever, R.layout.simple_calllog_item//
			        , new String[] { "name", "number", "date", "duration", "type" }//
			        , new int[] { R.id.tv_name, R.id.tv_number, R.id.tv_date, R.id.tv_duration, R.id.tv_type });
		 all_phone_listview.setAdapter(adapter);
		 gone_phone_listview.setAdapter(adapterGone);
		 never_phone_listview.setAdapter(adapterNever);
		 lv.setAdapter(adapter);
		 //通话记录点击操作
		 lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				// TODO Auto-generated method stub
				Map<String, String> map1 = dataList.get(position);
				mPhoneUtils.dialTo(map1.get("name"), map1.get("number"));
				mPhoneUtils.startCall(true);
			}
		});
		 
		 all_phone_listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				// TODO Auto-generated method stub
				Map<String, String> map1 = dataList.get(position);
				mPhoneUtils.dialTo(map1.get("name"), map1.get("number"));
				mPhoneUtils.startCall(true);
			}
		});
		 
		 gone_phone_listview.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position,
						long arg3) {
					// TODO Auto-generated method stub
					Map<String, String> map1 = dataList.get(position);
					mPhoneUtils.dialTo(map1.get("name"), map1.get("number"));
					mPhoneUtils.startCall(true);
				}
			});
		 never_phone_listview.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position,
						long arg3) {
					// TODO Auto-generated method stub
					Map<String, String> map1 = dataList.get(position);
					mPhoneUtils.dialTo(map1.get("name"), map1.get("number"));
					mPhoneUtils.startCall(true);
				}
			});
			 
		 
		 
		// 拨号代码
		// 按键声音播放设置及初始化
		try {
			// 获取系统参数“按键操作音”是否开启
			mDTMFToneEnabled = Settings.System.getInt(getContentResolver(),
					Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
			synchronized (mToneGeneratorLock) {
				if (mDTMFToneEnabled && mToneGenerator == null) {
					mToneGenerator = new ToneGenerator(
							AudioManager.STREAM_DTMF, 80); // 设置声音的大小
					setVolumeControlStream(AudioManager.STREAM_DTMF);
				}
			}
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
			mDTMFToneEnabled = false;
			mToneGenerator = null;
		}

		but1.setOnClickListener(this);
		but2.setOnClickListener(this);
		but3.setOnClickListener(this);
		but4.setOnClickListener(this);
		but5.setOnClickListener(this);
		but6.setOnClickListener(this);
		but7.setOnClickListener(this);
		but8.setOnClickListener(this);
		but9.setOnClickListener(this);
		but10.setOnClickListener(this);
		but11.setOnClickListener(this);
		but12.setOnClickListener(this);
		but13.setOnClickListener(this);
		but15.setOnClickListener(this);
		// 设置长按删除键，触发删除全部
		but15.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				tv.setText("");
				return false;
			}
		});

		//拨号键盘输入号码后处理
		tv.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				// 文本变化中
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				// 文本变化前
			}

			@Override
			public void afterTextChanged(Editable s) {
				// 文本变化后

			}
		});
		 

	}

	// 处理事件
	private void initEvent() {
		// TODO Auto-generated method stub
		mTabDianHua.setOnClickListener(this);
		mTabLianXiRen.setOnClickListener(this);
		mTabBoHao.setOnClickListener(this);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			/**
			 * ViewPage左右滑动时
			 */
			@Override
			public void onPageSelected(int arg0) {
				// TODO Auto-generated method stub
				int currentItem = mViewPager.getCurrentItem();
				switch (currentItem) {
				case 0:
					resetImg();
					mDianHuaImg.setBackgroundResource(R.drawable.tonghujiludone);
					break;
				case 1:
					resetImg();
					mLianXiRen.setBackgroundResource(R.drawable.lianxiren_done);
					break;
				case 2:
					resetImg();
					mBoHao.setBackgroundResource(R.drawable.bohao_done);
				default:
					break;

				}

			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub

			}
		});
		
		/**
		 * 通话记录
		 */
		mAllPhone.setOnClickListener(this);
		mGonePhone.setOnClickListener(this);
		mNeverPhone.setOnClickListener(this);
		/**
		 * 通话记录ViewPage左右滑动时
		 */
		mViewPager_tab01.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				int currentItem = mViewPager_tab01.getCurrentItem();
				switch (currentItem) {
				case 0:
					resetTv();
					mAllPhoneTv.setTextColor(android.graphics.Color.WHITE);
					break;
				case 1:
					resetTv();
					mGonePhoneTv.setTextColor(android.graphics.Color.WHITE);
					break;
				case 2:
					resetTv();
					mNeverPhoneTv.setTextColor(android.graphics.Color.WHITE);
				default:
					break;

				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPageSelected(int arg0) {
				// TODO Auto-generated method stub

			}

		});
		
		

	}

	/**
	 * 判断哪个要显示，及设置按钮图片
	 */
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.id_tab_tonghuajilu:
			mViewPager.setCurrentItem(0);
			resetImg();
			mDianHuaImg.setBackgroundResource(R.drawable.tonghujiludone);
			break;
		case R.id.id_tab_user:
			mViewPager.setCurrentItem(1);
			mLianXiRen.setBackgroundResource(R.drawable.lianxiren_done);
			break;
		case R.id.id_tab_bohao:
			mViewPager.setCurrentItem(2);
			mBoHao.setBackgroundResource(R.drawable.bohao_done);
			break;
		// 通话记录
		case R.id.id_tab_all_phone:
			mViewPager_tab01.setCurrentItem(0);
			resetTv();
			mAllPhoneTv.setTextColor(android.graphics.Color.WHITE);
			break;
		case R.id.id_tab_gone_phone:
			mViewPager_tab01.setCurrentItem(1);
			resetTv();
			mGonePhoneTv.setTextColor(android.graphics.Color.WHITE);
			break;
		case R.id.id_tab_never_phone:
			mViewPager_tab01.setCurrentItem(2);
			resetTv();
			mNeverPhoneTv.setTextColor(android.graphics.Color.WHITE);
			break;
		// 拨号
		case R.id.but1:
			playTone(ToneGenerator.TONE_DTMF_1);
			change("1");
			break;
		case R.id.but2:
			playTone(ToneGenerator.TONE_DTMF_2);
			change("2");
			break;
		case R.id.but3:
			playTone(ToneGenerator.TONE_DTMF_3);
			change("3");
			break;
		case R.id.but4:
			playTone(ToneGenerator.TONE_DTMF_4);
			change("4");
			break;
		case R.id.but5:
			playTone(ToneGenerator.TONE_DTMF_5);
			change("5");
			break;
		case R.id.but6:
			playTone(ToneGenerator.TONE_DTMF_6);
			change("6");
			break;
		case R.id.but7:
			playTone(ToneGenerator.TONE_DTMF_7);
			change("7");
			break;
		case R.id.but8:
			playTone(ToneGenerator.TONE_DTMF_8);
			change("8");
			break;
		case R.id.but9:
			playTone(ToneGenerator.TONE_DTMF_9);
			change("9");
			break;
		case R.id.but10:
			playTone(ToneGenerator.TONE_DTMF_S);
			change("*");
			break;
		case R.id.but11:
			playTone(ToneGenerator.TONE_DTMF_0);
			change("0");
			break;
		case R.id.but12:
			playTone(ToneGenerator.TONE_DTMF_P);
			change("#");
			break;
		case R.id.but13:
			call();
			break;
		case R.id.but15:
			delete();
			break;
		default:
			break;
		}

	}

	/**
	 *  主界面底部，把所有图片变暗
	 */
	private void resetImg() {
		// TODO Auto-generated method stub
		mDianHuaImg.setBackgroundResource(R.drawable.tonghuajilu);
		mLianXiRen.setBackgroundResource(R.drawable.lianxiren);
		mBoHao.setBackgroundResource(R.drawable.bohao);

	}
	//通话记录，把文字变亮
		private void resetTv(){
			mAllPhoneTv.setTextColor(android.graphics.Color.BLACK);
			mGonePhoneTv.setTextColor(android.graphics.Color.BLACK);
			mNeverPhoneTv.setTextColor(android.graphics.Color.BLACK);
		}

	
		/**
		 * tab02联系人列表相关
		 *
		 */
	public class ComparatorPY implements Comparator<Persons>{

		@Override
		public int compare(Persons lhs, Persons rhs) {
			String str1 = lhs.PY;
			String str2 = rhs.PY;
			return str1.compareToIgnoreCase(str2);
		}
	}
	//联系人列表适配器
	 class ListAdapter extends BaseAdapter{
		private LayoutInflater m_inflater;
		private ArrayList<Persons> Persons;
    	private Context context;
		
        public ListAdapter(Context context,
        		ArrayList<Persons> persons) {
    	    this.m_inflater = LayoutInflater.from(context);
    	    this.Persons = persons;
    	    this.context = context;
        }
        
    	//当联系人列表数据发生变化时,用此方法来更新列表
    	public void updateListView(ArrayList<Persons> persons){
    		this.Persons = persons;
    		notifyDataSetChanged();
    	}

		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return Persons.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return Persons.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
            convertView = m_inflater.inflate(R.layout.list_item, null);
            
            TextView name = (TextView) convertView.findViewById(R.id.contacts_name);
            name.setText(Persons.get(position).Name);
    	    
            TextView number = (TextView) convertView.findViewById(R.id.contacts_number);
            number.setText(Persons.get(position).Number);
            
			//字母提示textview的显示 
			TextView letterTag = (TextView)convertView.findViewById(R.id.pb_item_LetterTag);
			//获得当前姓名的拼音首字母
			String firstLetter = Persons.get(position).PY.substring(0,1).toUpperCase();
			
			//如果是第1个联系人 那么letterTag始终要显示
			if(position == 0)
			{
				letterTag.setVisibility(View.VISIBLE);
				letterTag.setText(firstLetter);
			}			
			else
			{
				//获得上一个姓名的拼音首字母
				String firstLetterPre = Persons.get(position-1).PY.substring(0,1).toUpperCase();
				//比较一下两者是否相同
				if(firstLetter.equals(firstLetterPre))
				{
					letterTag.setVisibility(View.GONE);
				}
				else 
				{
					letterTag.setVisibility(View.VISIBLE);
					letterTag.setText(firstLetter);
				}
			}

			return convertView;
		}
		
	}
	
	//字母列触摸的监听器
	private class ScrollBarListener implements AlphabetScrollBar.OnTouchBarListener {

		@Override
		public void onTouch(String letter) {
			
			//触摸字母列时,将联系人列表更新到首字母出现的位置
	        for (int i = 0;   i < persons.size(); i++) {  
	            if (persons.get(i).PY.substring(0, 1).compareToIgnoreCase(letter) == 0) { 
	            	m_contactslist.setSelection(i);
	            	break;
	            }  
	        } 
		}
	}
	
    public void getContacts() {
            ContentResolver contentResolver = getContentResolver();
            // 获得所有联系人数据集的游标
            Cursor cursor = contentResolver.query(NeusoftContactsContract.NEUSOFT_CONTACT_URI,null,
            		null, null, null);
            // 循环遍历
            if (cursor.moveToFirst()) {
                    
                    int idColumn = cursor.getColumnIndex(NeusoftContactsContract.ContactColumns._ID);
                    int displayNameColumn = cursor.getColumnIndex(NeusoftContactsContract.ContactColumns.DISPLAY_NAME);
                    int NumberColumn = cursor.getColumnIndex(NeusoftContactsContract.ContactColumns.PHONE1);

                    while (cursor.moveToNext()){
                    		Persons person = new Persons();
                            // 获得联系人的ID号
                            String contactId = cursor.getString(idColumn);

                            // 获得联系人姓名
                            person.Name = cursor.getString(displayNameColumn);
                            person.PY = PinyinUtils.getPingYin(person.Name);
                            person.FisrtSpell = PinyinUtils.getFirstSpell(person.Name);
                            person.Number = cursor.getString(NumberColumn);
                            Log.v("lianxiren", "名字:"+person.Name + "号码:"+person.Number + "姓名首字母:"+person.FisrtSpell );

                            persons.add(person);
                    }
                    cursor.close();
            }
    }
    
	private void filterContacts(String filterStr){
		ArrayList<Persons> filterpersons = new ArrayList<Persons>();
		
        //遍历所有联系人数组,筛选出包含关键字的联系人
        for (int i = 0; i < persons.size(); i++) {  
            //过滤的条件
              if (isStrInString(persons.get(i).Number,filterStr)
            		||isStrInString(persons.get(i).PY,filterStr)
            		||persons.get(i).Name.contains(filterStr)
            		||isStrInString(persons.get(i).FisrtSpell,filterStr)){
                //将筛选出来的联系人重新添加到filterpersons数组中
            	Persons filterperson = new Persons();
            	filterperson.Name = persons.get(i).Name;
            	filterperson.PY = persons.get(i).PY;
            	filterperson.Number = persons.get(i).Number;
            	filterperson.FisrtSpell = persons.get(i).FisrtSpell;
            	filterpersons.add(filterperson);
            }  
        }  
        
        //如果没有匹配的联系人
		if(filterpersons.isEmpty())
		{
			m_contactslist.setEmptyView(m_listEmptyText);
		}
        
        //将列表更新为过滤的联系人
        m_listadapter.updateListView(filterpersons);
	}
	
	public boolean isStrInString(String bigStr,String smallStr){
		  if(bigStr.toUpperCase().indexOf(smallStr.toUpperCase())>-1){
			  return true;
		  }else{
			  return false;
		  }
		 }
	
	
	/**
	 * tab03
	 * 
	 * @author Lenovo
	 * 
	 */

/**
 * 读取数据，通话记录
 * 
 * @return 读取到的数据
 */
private List<Map<String, String>> getDataList() {
  // 1.获得ContentResolver
  ContentResolver resolver = getContentResolver();
  // 2.利用ContentResolver的query方法查询通话记录数据库
  /**
   * @param uri 需要查询的URI，（这个URI是ContentProvider提供的）
   * @param projection 需要查询的字段
   * @param selection sql语句where之后的语句
   * @param selectionArgs ?占位符代表的数据
   * @param sortOrder 排序方式
   * 
   */
  Cursor cursor = resolver.query(NeusoftContactsContract. NEUSOFT_CALLLOG_URI 
, // 查询通话记录的URI
      new String[] { NeusoftContactsContract.CallLogColumns.DISPLAY_NAME// 通话记录的联系人
          , NeusoftContactsContract.CallLogColumns. NUMBER// 通话记录的电话号码
          , NeusoftContactsContract.CallLogColumns.DATE// 通话记录的日期
          , null//NeusoftContactsContract. CallLogColumns.DURATION// 通话时长
          , NeusoftContactsContract.CallLogColumns.TYPE }// 通话类型
      , null, null, CallLog.Calls.DEFAULT_SORT_ORDER// 按照时间逆序排列，最近打的最先显示
  );
  // 3.通过Cursor获得数据
  List<Map<String, String>> list = new ArrayList<Map<String, String>>();
  while (cursor.moveToNext()) {
    String name = cursor.getString(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DISPLAY_NAME));
    String number = cursor.getString(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns. NUMBER));
    long dateLong = cursor.getLong(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DATE));
    String date = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(dateLong));
    //int duration = cursor.getInt(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DATE));
    int type = cursor.getInt(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.TYPE));
    String typeString = "";
    switch (type) {
    case NeusoftContactsContract.CallLogType.INCOMING_TYPE:
      typeString = "打入";
      break;
    case NeusoftContactsContract.CallLogType.OUTGOING_TYPE:
      typeString = "打出";
      break;
    case NeusoftContactsContract.CallLogType.MISSED_TYPE:
      typeString = "未接";
      break;
    default:
      break;
    }
    Map<String, String> map = new HashMap<String, String>();
    map.put("name", (name == null) ? "未备注联系人" : name);
    map.put("number", number);
    map.put("date", date);
    //map.put("duration", (duration / 60) + "分钟");
    map.put("type", typeString);
    list.add(map);
  }
  return list;
}

/**
 * 已接读取数据，通话记录
 * 
 * @return 读取到的数据
 */
private List<Map<String, String>> getGoneDataList() {
  // 1.获得ContentResolver
  ContentResolver resolver = getContentResolver();
  // 2.利用ContentResolver的query方法查询通话记录数据库
  /**
   * @param uri 需要查询的URI，（这个URI是ContentProvider提供的）
   * @param projection 需要查询的字段
   * @param selection sql语句where之后的语句
   * @param selectionArgs ?占位符代表的数据
   * @param sortOrder 排序方式
   * 
   */
  Cursor cursor = resolver.query(NeusoftContactsContract. NEUSOFT_CALLLOG_URI 
, // 查询通话记录的URI
      new String[] { NeusoftContactsContract.CallLogColumns.DISPLAY_NAME// 通话记录的联系人
          , NeusoftContactsContract.CallLogColumns. NUMBER// 通话记录的电话号码
          , NeusoftContactsContract.CallLogColumns.DATE// 通话记录的日期
          , null//NeusoftContactsContract. CallLogColumns.DURATION// 通话时长
          , NeusoftContactsContract.CallLogColumns.TYPE }// 通话类型
      , null, null, CallLog.Calls.DEFAULT_SORT_ORDER// 按照时间逆序排列，最近打的最先显示
  );
  // 3.通过Cursor获得数据
  List<Map<String, String>> list = new ArrayList<Map<String, String>>();
  while (cursor.moveToNext()) {
    String name = cursor.getString(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DISPLAY_NAME));
    String number = cursor.getString(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns. NUMBER));
    long dateLong = cursor.getLong(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DATE));
    String date = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(dateLong));
    //int duration = cursor.getInt(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DATE));
    int type = cursor.getInt(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.TYPE));
    String typeString = "";
    switch (type) {
    case NeusoftContactsContract.CallLogType.INCOMING_TYPE:
      typeString = "打入";
      Map<String, String> map = new HashMap<String, String>();
      map.put("name", (name == null) ? "未备注联系人" : name);
      map.put("number", number);
      map.put("date", date);
      //map.put("duration", (duration / 60) + "分钟");
      map.put("type", typeString);
      list.add(map);
      break;
    default:
      break;
    }
   
  }
  return list;
}
/**
 * 未接读取数据，通话记录
 * 
 * @return 读取到的数据
 */
private List<Map<String, String>> getNeverDataList() {
  // 1.获得ContentResolver
  ContentResolver resolver = getContentResolver();
  // 2.利用ContentResolver的query方法查询通话记录数据库
  /**
   * @param uri 需要查询的URI，（这个URI是ContentProvider提供的）
   * @param projection 需要查询的字段
   * @param selection sql语句where之后的语句
   * @param selectionArgs ?占位符代表的数据
   * @param sortOrder 排序方式
   * 
   */
  Cursor cursor = resolver.query(NeusoftContactsContract. NEUSOFT_CALLLOG_URI 
, // 查询通话记录的URI
      new String[] { NeusoftContactsContract.CallLogColumns.DISPLAY_NAME// 通话记录的联系人
          , NeusoftContactsContract.CallLogColumns. NUMBER// 通话记录的电话号码
          , NeusoftContactsContract.CallLogColumns.DATE// 通话记录的日期
          , null//NeusoftContactsContract. CallLogColumns.DURATION// 通话时长
          , NeusoftContactsContract.CallLogColumns.TYPE }// 通话类型
      , null, null, CallLog.Calls.DEFAULT_SORT_ORDER// 按照时间逆序排列，最近打的最先显示
  );
  // 3.通过Cursor获得数据
  List<Map<String, String>> list = new ArrayList<Map<String, String>>();
  while (cursor.moveToNext()) {
    String name = cursor.getString(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DISPLAY_NAME));
    String number = cursor.getString(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns. NUMBER));
    long dateLong = cursor.getLong(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DATE));
    String date = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(dateLong));
    //int duration = cursor.getInt(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.DATE));
    int type = cursor.getInt(cursor.getColumnIndex(NeusoftContactsContract.CallLogColumns.TYPE));
    String typeString = "";
    switch (type) {
    case NeusoftContactsContract.CallLogType.MISSED_TYPE:
      typeString = "未接";
      Map<String, String> map = new HashMap<String, String>();
      map.put("name", (name == null) ? "未备注联系人" : name);
      map.put("number", number);
      map.put("date", date);
      //map.put("duration", (duration / 60) + "分钟");
      map.put("type", typeString);
      list.add(map);
      break;
    default:
      break;
    }
   
  }
  return list;
}	
	
	/**
	 * 播放按键声音
	 */
	private void playTone(int tone) {
		if (!mDTMFToneEnabled) {
			return;
		}
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = audioManager.getRingerMode();
		if (ringerMode == AudioManager.RINGER_MODE_SILENT
				|| ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
			// 静音或者震动时不发出声音
			return;
		}
		synchronized (mToneGeneratorLock) {
			if (mToneGenerator == null) {
				Log.w(TAG, "playTone: mToneGenerator == null, tone: " + tone);
				return;
			}
			mToneGenerator.startTone(tone, DTMF_DURATION_MS); // 发出声音
		}
	}

	private void change(String number) {
		StringBuffer sb = new StringBuffer(tv.getText());
		tv.setText(sb.append(number));
	}

	private void delete() {
		if (tv.getText() != null && tv.getText().length() > 1) {
			StringBuffer sb = new StringBuffer(tv.getText());
			tv.setText(sb.substring(0, sb.length() - 1));
		} else if (tv.getText() != null && !"".equals(tv.getText())) {
			tv.setText("");
		}
	}

	private void call() {
		/**
		 * 打电话需要获取系统权限，需要到AndroidManifest.xml里面配置权限 <uses-permission
		 * android:name="android.permission.CALL_PHONE"/>
		 */

		mPhoneUtils.dialTo("未知号码", ""+tv.getText());
		mPhoneUtils.startCall(true); 

		
		

	}


	// 结束活动
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

}
