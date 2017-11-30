package com.neusoft.phone.utils;

import android.util.Log;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
/**
 * 拼音帮助类 
 */
public class PinyinUtils {
	/**
	 * 将字符串中的中文转化为拼音,其他字符不变
	 * 花花大神->huahuadashen
	 * @param inputString
	 * @return
	 */
	public static String getPingYin(String inputString) {
		HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		format.setVCharType(HanyuPinyinVCharType.WITH_V);

		char[] input = inputString.trim().toCharArray();
		String output = "";

		try {
			for (char curchar : input) {
				if (java.lang.Character.toString(curchar).matches(
						"[\\u4E00-\\u9FA5]+")) {
					String[] temp = PinyinHelper.toHanyuPinyinStringArray(
							curchar, format);
					output += temp[0];
				} else
					output += java.lang.Character.toString(curchar);
			}
		} catch (BadHanyuPinyinOutputFormatCombination e) {
			e.printStackTrace();
		}
		return output;
	}

	/**
	 * 汉字转换为汉语拼音首字母，英文字符不变
	 * 花花大神->hhds
	 * @param chines
	 *            汉字
	 * @return 拼音
	 */
	public static String getFirstSpell(String chinese) {  
	            StringBuffer pybf = new StringBuffer();  
	            char[] arr = chinese.toCharArray();  
	            HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();  
	            defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);  
	            defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);  
	            //遍历数组的简单方式for(:)
	            for (char curchar : arr) {  
                    if (curchar > 128) {  
                            try {  
                                    String[] temp = PinyinHelper.toHanyuPinyinStringArray(curchar, defaultFormat);  
                                    if (temp != null) {
                                    	if(temp.length > 0){
                                    		//因为会有一些联系人名字是数字，他超过128却没首字母
                                    	Log.d("temp", "不是空数组");
                                    	Log.d("temp", String.valueOf(temp.length));
                                            pybf.append(temp[0].charAt(0));  //修改时这里数组为空len=0,index=0
                                            //.charAt(0)返回这个字符串的第一个位置的字符
                                    	}
                                    }  
                            } catch (BadHanyuPinyinOutputFormatCombination e) {  
                                    e.printStackTrace();  
                            }  
                    } else {  
                            pybf.append(curchar);  
                    }  
            }  
	            return pybf.toString().replaceAll("\\W", "").trim();  
	    } 
    
}
