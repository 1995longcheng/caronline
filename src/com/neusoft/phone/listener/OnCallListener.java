package com.neusoft.phone.listener;

import com.neusoft.c3alfus.bluetooth.aidl.HfpPhoneNumberInfo;

public interface OnCallListener {

    public void onHfpStateChanged(boolean isConnected);

    public void onNumberChanged(HfpPhoneNumberInfo mHfpPhoneNumberInfo);

    public void onHangup();

    public void onAudioTransferChanged();

}
