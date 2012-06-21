package pl.shockah.shocky;

import java.io.Console;
import java.io.File;
import java.util.ArrayList;

import org.pircbotx.User;

import pl.shockah.Config;
import pl.shockah.FileLine;
import pl.shockah.StringTools;
import pl.shockah.shocky.lines.Line;
import pl.shockah.shocky.lines.LineAction;
import pl.shockah.shocky.lines.LineEnterLeave;
import pl.shockah.shocky.lines.LineKick;
import pl.shockah.shocky.lines.LineMessage;
import pl.shockah.shocky.lines.LineOther;

public class Data {
	public static final Config config = new Config();
	public static final ArrayList<String> controllers = new ArrayList<String>();
	public static final ArrayList<String> channels = new ArrayList<String>();
	public static final ArrayList<String> blacklistNicks = new ArrayList<String>();
	static {
		initializeLineTypes();
	}
	
	private static synchronized void blank() {
		Data.config.setNotExists("main-botname","Shocky");
		Data.config.setNotExists("main-server","irc.esper.net");
		Data.config.setNotExists("main-version","Shocky - PircBotX 1.6 - https://github.com/clone1018/Shocky - http://pircbotx.googlecode.com");
		Data.config.setNotExists("main-verbose",false);
		Data.config.setNotExists("main-maxchannels",10);
		Data.config.setNotExists("main-nickservpass","");
		Data.config.setNotExists("main-cmdchar","`~");
		Data.config.setNotExists("main-messagelength",400);
		Data.config.setNotExists("main-sqlurl","http://localhost/shocky/sql.php");
		Data.config.setNotExists("main-sqlhost","localhost");
		Data.config.setNotExists("main-sqluser","");
		Data.config.setNotExists("main-sqlpass","");
		Data.config.setNotExists("main-sqldb","shocky");
		Data.config.setNotExists("main-sqlprefix","");
		
		Console c = System.console();
		if (c == null) System.out.println("--- Not running in console, using default first-run settings ---"); else {
			System.out.println("--- First-run setup ---\n(just press Enter for default value)\n");
			
			firstRunSetupString(c,"main-server");
			firstRunSetupBoolean(c,"main-verbose");
			firstRunSetupString(c,"main-botname");
			firstRunSetupPassword(c,"main-nickservpass");
			firstRunSetupString(c,"main-version");
			firstRunSetupInt(c,"main-maxchannels");
			firstRunSetupString(c,"main-cmdchar");
			firstRunSetupInt(c,"main-messagelength");
			firstRunSetupString(c,"main-sqlurl");
			firstRunSetupString(c,"main-sqlhost");
			firstRunSetupString(c,"main-sqluser");
			firstRunSetupPassword(c,"main-sqlpass");
			firstRunSetupString(c,"main-sqldb");
			firstRunSetupString(c,"main-sqlprefix");
			System.out.println();
		}
	}
	protected static synchronized void load() {
		File dir = new File("data"); dir.mkdir();
		
		config.load(new File(dir,"config.cfg"));
		if (new File(dir,"config.cfg").exists()) {
			controllers.addAll(FileLine.read(new File(dir,"controllers.cfg")));
			channels.addAll(FileLine.read(new File(dir,"channels.cfg")));
			blacklistNicks.addAll(FileLine.read(new File(dir,"blacklistNicks.cfg")));
		} else blank();
	}
	protected static synchronized void save() {
		File dir = new File("data"); dir.mkdir();
		
		config.save(new File(dir,"config.cfg"));
		FileLine.write(new File(dir,"controllers.cfg"),controllers);
		FileLine.write(new File(dir,"channels.cfg"),channels);
		FileLine.write(new File(dir,"blacklistNicks.cfg"),blacklistNicks);
		
		for (Module module : Module.getModules()) module.onDataSave();
	}
	
	private static void firstRunSetupString(Console c, String key) {
		String input = c.readLine(key+" (def: "+Data.config.getString(key)+"): ");
		if (input != null && !input.isEmpty()) Data.config.set(key,input);
	}
	private static void firstRunSetupPassword(Console c, String key) {
		String input = new String(c.readPassword(key+" (def: "+Data.config.getString(key)+"): "));
		if (input != null && !input.isEmpty()) Data.config.set(key,input);
	}
	private static void firstRunSetupBoolean(Console c, String key) {
		while (true) {
			String input = c.readLine(key+" (def: "+Data.config.getString(key)+"): ");
			if (input != null && !input.isEmpty()) {
				if (input.toLowerCase().matches("^(t(rue)?)|(f(alse)?)$")) {
					Data.config.set(key,input);
					return;
				}
			} else return;
		}
	}
	private static void firstRunSetupInt(Console c, String key) {
		while (true) {
			String input = c.readLine(key+" (def: "+Data.config.getString(key)+"): ");
			if (input != null && !input.isEmpty()) {
				if (StringTools.isNumber(input)) {
					Data.config.set(key,input);
					return;
				}
			} else return;
		}
	}
	public static void initializeLineTypes() {
		Line.registerLineType((byte) 0, LineOther.class);
		Line.registerLineType((byte) 1, LineMessage.class);
		Line.registerLineType((byte) 2, LineAction.class);
		Line.registerLineType((byte) 3, LineEnterLeave.class);
		Line.registerLineType((byte) 4, LineKick.class);
	}
	public static boolean isBlacklisted(User user) {
		start: for (int i = 0; i < blacklistNicks.size(); i++) {
			String blacklisted = blacklistNicks.get(i);
			char type = 'n';
			if (blacklisted.charAt(1) == ':') {
				String[] blacklistParts = blacklisted.split(":", 2);
				type = blacklistParts[0].charAt(0);
				blacklisted = blacklistParts[1];
			}
			String[] array = blacklisted.split("\\*");
			String value = "";
			int o = 0;
			switch (type) {
			case 'n':
				value = user.getNick().toLowerCase();
				break;
			case 'h':
				value = user.getHostmask().toLowerCase();
				break;
			case 'i':
				value = user.getLogin().toLowerCase();
				break;
			}
			for (String part : array) {
				int idx = value.indexOf(part, o);
				if (idx == -1)
					continue start;
				o += idx;
			}
			return true;
		}

		return false;
	}
}