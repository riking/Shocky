package pl.shockah;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

import pl.shockah.shocky.Data;

public class StringTools {
	private static HashMap<String,String> htmlEntities;
	public static final Pattern unicodePattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
	public static final Pattern htmlTagPattern = Pattern.compile("<([^>]+)>");
	public static final UnicodeUnescaper unicodeEscape = new UnicodeUnescaper();
	
	static {
		htmlEntities = new HashMap<String,String>();
		htmlEntities.put("&lt;","<"); htmlEntities.put("&gt;",">");
		htmlEntities.put("&amp;","&"); htmlEntities.put("&quot;","\"");
		htmlEntities.put("&agrave;","a"); htmlEntities.put("&Agrave;","A");
		htmlEntities.put("&acirc;","�"); htmlEntities.put("&auml;","�");
		htmlEntities.put("&Auml;","�"); htmlEntities.put("&Acirc;","�");
		htmlEntities.put("&aring;","a"); htmlEntities.put("&Aring;","A");
		htmlEntities.put("&aelig;","a"); htmlEntities.put("&AElig;","A" );
		htmlEntities.put("&ccedil;","�"); htmlEntities.put("&Ccedil;","�");
		htmlEntities.put("&eacute;","�"); htmlEntities.put("&Eacute;","�" );
		htmlEntities.put("&egrave;","e"); htmlEntities.put("&Egrave;","E");
		htmlEntities.put("&ecirc;","e"); htmlEntities.put("&Ecirc;","E");
		htmlEntities.put("&euml;","�"); htmlEntities.put("&Euml;","�");
		htmlEntities.put("&iuml;","i"); htmlEntities.put("&Iuml;","I");
		htmlEntities.put("&ocirc;","�"); htmlEntities.put("&Ocirc;","�");
		htmlEntities.put("&ouml;","�"); htmlEntities.put("&Ouml;","�");
		htmlEntities.put("&oslash;","o"); htmlEntities.put("&Oslash;","O");
		htmlEntities.put("&szlig;","�"); htmlEntities.put("&ugrave;","u");
		htmlEntities.put("&Ugrave;","U"); htmlEntities.put("&ucirc;","u");
		htmlEntities.put("&Ucirc;","U"); htmlEntities.put("&uuml;","�");
		htmlEntities.put("&Uuml;","�"); htmlEntities.put("&nbsp;"," ");
		htmlEntities.put("&copy;","\u00a9");
		htmlEntities.put("&reg;","\u00ae");
		htmlEntities.put("&euro;","\u20a0");
		htmlEntities.put("&bull;","�");
	}
	
	public static String getFilenameStrippedWindows(String fname) {
		fname = fname.replace('\\','-');
		fname = fname.replace('/','-');
		fname = fname.replace(": "," - ");
		fname = fname.replace(':','-');
		fname = fname.replace("*","");
		fname = fname.replace("?","");
		fname = fname.replace('"','\'');
		fname = fname.replace('<','[');
		fname = fname.replace('>',']');
		fname = fname.replace("|"," - ");
		return fname;
	}
	
	public static String escapeHTML(String s) {
		s = s.replace("&","&amp;");
		for (String key : htmlEntities.keySet()) {
			if (key.equals("&amp;")) continue;
			s = s.replace(htmlEntities.get(key),key);
		}
		return s;
	}
	public static String unescapeHTML(String source) {
		boolean continueLoop;
		int skip = 0;
		do {
			continueLoop = false;
			int i = source.indexOf("&",skip);
			if (i > -1) {
				int j = source.indexOf(";",i);
				if (j > i) {
					String entityToLookFor = source.substring(i,j+1);
					if (entityToLookFor.indexOf("#")==1) {
						char chr = (char)Integer.parseInt(source.substring(i+2,j));
						source = source.substring(0,i)+chr+source.substring(j+1);
						continueLoop = true;
					} else {
						String value = htmlEntities.get(entityToLookFor);
						if (value != null) {
							source = source.substring(0,i)+value+source.substring(j+1);
							continueLoop = true;
						} else if (value == null) {
							skip = i+1;
							continueLoop = true;
						}
					}
				}
			}
		} while (continueLoop);
		return source;
	}
	public static String stripHTMLTags(CharSequence s) {
		Matcher m = htmlTagPattern.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (m.find())
			m.appendReplacement(sb, "");
		m.appendTail(sb);
		return sb.toString();
	}
	public static String unicodeParse(CharSequence s) {
		Matcher m = unicodePattern.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			int hex = Integer.valueOf(m.group(1),16);
			m.appendReplacement(sb, Character.toString((char)hex));
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	public static boolean isNumber(CharSequence s) {
		boolean ret = true;
		for (int i = 0; ret && i < s.length(); i++) {
			Character c = s.charAt(i);
			if (i == 0 && c == '-')
				continue;
			if (!Character.isDigit(c))
				ret = false;
		}
		return ret;
	}
	
	public static boolean isBoolean(String s) {
		return (s.equalsIgnoreCase("true")||s.equalsIgnoreCase("false"));
	}
	
	public static String ircFormatted(CharSequence s, boolean urlDecode) {
		String output = unicodeEscape.translate(s);
		output = StringEscapeUtils.unescapeHtml4(output);
		output = output.replaceAll("</?b>", "\u0002");
		output = output.replaceAll("</?u>", "\u001f");
		if (urlDecode) {
			try {
				output = URLDecoder.decode(output, "utf8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		output = StringEscapeUtils.unescapeJava(output);
		output = stripHTMLTags(output);
		output = deleteWhitespace(output);
		output = limitLength(output);
		return output;
	}
	
    public static String deleteWhitespace(CharSequence str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
        	char c = str.charAt(i);
            if (i > 0 && Character.isWhitespace(c) && Character.isWhitespace(str.charAt(i-1))) {
                continue;
            }
            if (c == '\r' || c == '\n') {
            	sb.append(' ');
            	continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    public static String limitLength(CharSequence str) {
    	if (str.length() > Data.config.getInt("main-messagelength"))
    		str = str.subSequence(0, Data.config.getInt("main-messagelength"))+"...";
    	return str.toString();
    }
	
	public static <T> String implode(T[] spl, String separator) {return implode(spl,0,spl.length-1,separator);}
	public static <T> String implode(T[] spl, int a, String separator) {return implode(spl,a,spl.length-1,separator);}
	public static <T> String implode(T[] spl, int a, int b, String separator) {
		StringBuffer sb = new StringBuffer();
		while (a <= b) {
			if (sb.length() != 0) sb.append(separator);
			sb.append(spl[a++]);
		}
		return sb.toString();
	}
}