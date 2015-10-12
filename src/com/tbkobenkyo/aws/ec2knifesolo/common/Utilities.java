package com.tbkobenkyo.aws.ec2knifesolo.common;

import java.util.ResourceBundle;

/**
 * @author shimapee
 *
 */
public class Utilities {
	
	/**
	 * @param key
	 * @return String
	 */
	public static String getValue(String key) {
		ResourceBundle bundle = ResourceBundle.getBundle("awssdk");
		return bundle.getString(key);
		
	}
	
}
