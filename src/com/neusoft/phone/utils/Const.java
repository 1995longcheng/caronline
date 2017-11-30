package com.neusoft.phone.utils;

public class Const {

    //更新未接来电广播
    public static final String BROAD_UPDATE_MISSCALL = "com.neusoft.phone.updatemisscall";
    //未接来电个数Key值
    public static final String WIDGET_MISSCALL = "miss";
    //未接来电默认值
    public static final int WIDGET_NOTICE_ASC = -1;
    //电检广播
    public static final String ACTION_ELEC_CHECK = "com.neusoft.electrical.start";
    //启动工程师模式广播
    public static final String ACTION_ENGINEER_MODE = "com.neusoft.engineer.start";
    public static final String ACTION_SHOP_SERVE_MODE = "com.neusoft.shopserve.start";
    public static final String ACTION_AFTER_DEVELOPER_MODE = "com.neusoft.after.developer.start";

    // 同步广播（同步通话记录、同步联系人数据）
    public static final String SYNC_CALLLOG_START = "com.neusoft.intent.sync.callog.start";
    public static final String SYNC_CALLLOG_STOP = "com.neusoft.intent.sync.alllog.stop";
    public static final String SYNC_CONTACT_START = "com.neusoft.intent.sync.contact.start";
    public static final String SYNC_CONTACT_STOP = "com.neusoft.intent.sync.contact.stop";
    // VR广播
    public static final String VRMMS_PHONE_BROADCAST = "com.vrmms.intent.PHONE";
    //HardKey压下打开蓝牙电话广播
    public static final String ACTION_HARDKEY_SHORTPRESS = "com.neusoft.hardkey_shortpress";
    // VR广播参数
    public static final String VRMMS_OPERATION_KEY = "operate";
    public static final String VRMMS_TIMER_KEY = "timer";
    public static final String VRMMS_PKG_KEY = "pkg";
    public static final String VRMMS_RESPONSE_KEY = "response";

    // 启动指令
    public static final String VRMMS_COMMAND_START = "app_open";
    // 拨打电话指令
    public static final String VRMMS_COMMAND_PHONE_CALL = "phone_call";

    public static final String VRMMS_COMMAND_PHONE_CALLLOG = "phone_operate";

    public static final String VRMMS_PARAM_PHONE_NUMBER = "phone-number";
    public static final String VRMMS_PARAM_PHONE_INDEX = "telephone_operate";

    /** VRMMS ACTION */
    public final static String VRMMS_ACTION_KEY = "vrmms_action";

    public static final int HFP_INFO_INDEX = 1;
    public static final int HFP_INFO_DIRECTION_OUTGOING = 0;
    public static final int HFP_INFO_DIRECTION_INCOMING = 1;
    public static final int HFP_INFO_STATUS_OUTGOING = 2;
    public static final int HFP_INFO_STATUS_INCOMING = 4;
    public static final int HFP_INFO_MULTIPARTY_NO = 0;

    public static final int MSG_CHANGE_BUTTON_LIST = 1;
    public static final int MSG_CLEAR_MISSED_COUNT = 2;

}
