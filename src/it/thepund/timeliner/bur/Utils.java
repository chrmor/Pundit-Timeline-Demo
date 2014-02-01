package it.thepund.timeliner.bur;

public class Utils {
	
	public static String getLabelFromURL(String url) {
		String result = url;
		if (result.contains("#")) {
			result = result.split("#")[0];
		}
		if (result.contains("/")) {
			result = result.split("/")[result.split("/").length - 1];
		}
		return result;
	}

}
