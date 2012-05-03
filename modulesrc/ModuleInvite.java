import org.pircbotx.PircBotX;
import org.pircbotx.UserSnapshot;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.KickEvent;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.MultiChannel;
import pl.shockah.Config;
import pl.shockah.shocky.Shocky;

public class ModuleInvite extends Module {
	private HashMap<String,long> lastKick = new HashMap<String,long>();
	// TODO: put in config
	/*package*/ long waitTime = 300000; // 5 min * 60 s * 1000 ms
	public String name() {return "invite";}
	public void load() {}
	public void unload() {}
	
	public void onInvite(InviteEvent<PircBotX> event) {
		if (Data.getBlacklistNicks().contains(event.getUser().toLowerCase())) return;
		try {
			MultiChannel.join(event.getChannel());
		} catch (Exception e) {Shocky.sendNotice(event.getBot(),event.getBot().getUser(event.getUser()),"I'm already in channel "+event.getChannel());}
	}
	public void onKick(KickEvent<PircBotX> event)
	{
		lastKick.put(event.getChannel(),System.currentTimeMillis());
	}

	public class CmdInvite extends Command {
		public String command() {return "invite";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "invite #<channel> - uses ChanServ to invite self to channel";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}

		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			if (Data.getBlacklistNicks().contains(event.getUser().toLowerCase())) return;
			String[] args = message.split(" ");
			if (args.length != 2) { // No parameters
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
				return;
			}
			
			String targetchannel = args[1];
			if ((System.currentTimeMillis() - lastKick.get(targetchannel)) < waitTime)
			{
				Shocky.sendNotice(bot,sender,"Too close to kick event; not joining "+targetchannel);
			}
			Shocky.sendPrivate(bot,new UserSnapshot("ChanServ"),"INVITE "+targetchannel);
			Shocky.send(bot,type,EType.Channel,EType.Notice,EType.Notice,EType.Console,channel,sender,"CS request sent");
		}
	}
}