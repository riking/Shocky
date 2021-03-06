import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.Helper;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.IFactoidData;
import pl.shockah.shocky.sql.Factoid;

public class ModulePHP extends ScriptModule implements IFactoidData {
	public static final File savedData = new File("data", "php").getAbsoluteFile();
	
	protected Command cmd;
	
	public String name() {return "php";}
	public String identifier() {return "php";}
	public char stringCharacter() {return '\'';}
	public void onEnable(File dir) {
		if (!savedData.exists())
			savedData.mkdirs();
		
		if (!Data.config.exists("php-version") || Data.config.getInt("php-version") < 2) {
			Data.config.set("php-version", 2);
			Pattern pattern = Pattern.compile("^(.+?)_(-?[0-9]+)$");
			for (File f : savedData.listFiles()) {
				Matcher m = pattern.matcher(f.getName());
				if (!m.find())
					continue;
				int hash;
				try {
					hash = Integer.parseInt(m.group(2));
				} catch(Throwable t) {
					continue;
				}
				f.renameTo(new File(f.getParentFile(),String.format("%s_%08X",m.group(1),hash)));
			}
		}
		
		Data.config.setNotExists("php-url","http://localhost/shocky/shocky.php");
		Command.addCommands(this, cmd = new CmdPHP());
		Data.protectedKeys.add("php-url");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public File[] getReadableFiles() {
		return new File[]{savedData};
	}
	
	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		
		StringBuilder sb = new StringBuilder();
		buildInit(sb,getParams(bot, channel, sender, message).entrySet());
		String data = getData(factoid);
		if (data != null) {
			sb.append("$_STATE=json_decode(");
			appendEscape(sb,data);
			sb.append(");");
		}
		
		sb.append(code);
		
		HTTPQuery q = HTTPQuery.create(Data.forChannel(channel).getString("php-url"),HTTPQuery.Method.POST);
		q.connect(true,true);
		q.write(HTTPQuery.parseArgs("code",sb.toString()));
		
		String ret = null;
		try {
			ret = q.readWhole();
			q.close();
			JSONObject json = new JSONObject(ret);
			//json = json.getJSONObject("output");
			JSONObject error = json.optJSONObject("error");
			if (error != null)
				return error.getString("message");
			String safe_errors = json.optString("safe_errors", null);
			if (safe_errors != null)
				return safe_errors;
			String newdata = json.optString("data", null);
			if (factoid != null && newdata != null && (data == null || !newdata.contentEquals(data)))
				setData(factoid,newdata);
			return json.optString("output");
		} catch (JSONException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return (ret != null)?ret.trim():null;
	}
	
	private void buildInit(StringBuilder sb, Iterable<Map.Entry<String,Object>> set) {
		for (Map.Entry<String,Object> pair : set) {
			sb.append('$').append(pair.getKey()).append('=');
			appendObject(sb, pair.getValue());
			sb.append(';');
		}
	}
	
	private void appendObject(StringBuilder sb, Object obj) {
		if (obj == null) {
				sb.append("null");
		} else if (obj.getClass().isArray()) {
			Object[] a = (Object[])obj;
			sb.append("array(");
			for (int i = 0; i < a.length; ++i) {
				if (i > 0)
					sb.append(',');
				appendObject(sb, a[i]);
			}
			sb.append(')');
		} else if (obj instanceof String) {
			appendEscape(sb,(String)obj);
		} else if (obj instanceof Number) {
			sb.append(obj.toString());
		}
	}
	
	public String readFile(File file) {
		try {
			FileInputStream fs = new FileInputStream(file);
			InputStreamReader sr = new InputStreamReader(fs, Helper.utf8);
			char[] buffer = new char[(int)file.length()];
			sr.read(buffer);
			sr.close();
			return new String(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean writeFile(File file, CharSequence str) {
		try {
			FileOutputStream fs = new FileOutputStream(file);
			OutputStreamWriter sr = new OutputStreamWriter(fs, Helper.utf8);
			sr.append(str);
			//sr.flush();
			sr.close();
			//fs.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public File getFactoidSave(Factoid f) {
		if (f != null)
			return new File(savedData,String.format("%s_%08X", f.channel== null? "global" : f.channel, f.name.hashCode()));
		return null;
	}
	
	@Override
	public String getData(Factoid f) {
		File saveFile = getFactoidSave(f);
		if (saveFile != null && saveFile.exists() && saveFile.canRead())
			return readFile(saveFile);
		return null;
	}
	@Override
	public boolean setData(Factoid f, CharSequence data) {
		if (f == null)
			return false;
		if (data != null && data.length() < (1024 * 10)) {
			File saveFile = getFactoidSave(f);
			try {
				if (saveFile.exists() || saveFile.createNewFile())
					return writeFile(saveFile,data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public class CmdPHP extends ScriptCommand {
		public String command() {return "php";}
		public String help(Parameters params) {
			return "\nphp {code} - runs PHP code";
		}
	}
}