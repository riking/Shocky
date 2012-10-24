import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

import com.sun.istack.internal.NotNull;

import pl.shockah.Config;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;	
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.cmds.CommandCallback;

/*
 *  Shocky UNO! Module
 *  This program is intended for Shocky the IRC Bot.
 *  Project page: https://github.com/clone1018/Shocky/
 *  
 *  This program was adapted from the bMotion Tcl script "Marky's Color Uno v0.98" authored by Mark A. Day.
 *  A mirror of the original file can be found at http://pastebin.com/n2RiAFqD
 *  Changes to the default configuration are commented.
 *  
 *  Shocky UNO! Module is Copyright (C) 2012 Kane P. York (kanepyork+coding@gmail.com) and distribuited under the GPL.
 *  Marky's Color Uno v0.98 is Copyright (C) 2004-2011 Mark A. Day (techwhiz@embarqmail.com) and distribuited under the GPL.
 *  Uno(tm) is Copyright (C) 2001 Mattel, Inc.
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

 /**
 *  Shocky UNO! Module
 *  @author Mark A. Day
 *  @author Kane P. York (Riking)
 *  @version [Incomplete]
 */
public class ModuleUno extends Module {
    public class CmdUnoHelp extends Command {
        /*package*/ boolean isEnabled = true;
        public String command() {return "uno";}
        public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
            if ((!isEnabled) || (channel.toString() != unochannel)) {
                callback.type = EType.Notice;
                callback.append(help(bot,type,channel,sender));
            }
            unocommands.get("!uno").doCommand(bot,type,callback,channel,sender,message);
        }
        public String help(PircBotX bot, EType type, Channel channel, User sender) {
            if (!isEnabled) return "uno - Shocky's UNO! is currently disabled. Sorry!";
            return "uno\nStarts an UNO! game, when used in "+unochannel+".";
        }
    }
    public enum EnumUnoMode {
	    Off, Joining, Playing, Postgame
	}
    public class UCommand {
	    public String trigger;
	    public UCommand() {
			// TODO Auto-generated constructor stub
		}
		public UCommand(String cmd) {
	        trigger = cmd;
	    }
	    public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {}
	}
    public class UnoStartTask implements Runnable {
    	final PircBotX bot;
    	final Channel channel;
    	public UnoStartTask(PircBotX bot, Channel channel) {
    		this.bot = bot; this.channel = channel;
    	}
        @Override public void run() { UnoStart(bot,channel); }
    }
    public class UnoSkipTask implements Runnable {
    	final PircBotX bot;
    	final Channel channel;
    	public UnoSkipTask(PircBotX bot, Channel channel) {
    		this.bot = bot; this.channel = channel;
    	}
    	@Override public void run() {
    		for( String player : players) {
    			
    		}
    	}
    }
    // As a note, I am using package-access a lot. This is to signify the concept of "it shouldn't be public, but it's not really private either".
    // It also makes a nice analog to the original script.
    Config config = new Config();
    public String unochannel;
    public static Random random = new Random(); // static random instance for shuffling
    File dataDir = new File("data","uno");
    /**
     *  This file logs all UNO! games and is only read when recalculating all-time highscores and records.
     *  Generally, it will be safe to delete the file if it is getting too large.
     *  Located at ./data/uno/uno_games.log
     */
    String gameLog;
    
    /**
     *  This file is the subset of the gameLog starting from the beginning of the month.
     *  This file will be cleared on the first of the month.
     *  Located at ./data/uno/uno_month.log
     */
    String gameLogMonth;
    /**
     *  Enables or disables logging. Is in config.
     */
    public boolean logEnabled;
    public HashMap<String,UCommand> unocommands;
    protected CmdUnoHelp unoHelpCommand;
    /*
     *  Begin .tcl imports
     */
    static final String UnoRedCard		  = "\0030,04 Red ";
    static final String UnoGreenCard     = "\0030,03 Green ";
    static final String UnoBlueCard      = "\0030,12 Blue ";
    static final String UnoYellowCard    = "\0031,08 Yellow ";
    static final String UnoSkipCard	  = "\002Skip\002 \003 ";
    static final String UnoReverseCard	  = "\002Reverse\002 \003 ";
    static final String UnoDrawTwoCard	  = "\002Draw Two\002 \003 ";
    static final String UnoWildCard		  = "\0031,8 \002W\0030,3I \0030,4L\0030,12D\002 \003 ";
    static final String UnoWildDrawFourCard = "\0031,8 \002W\0030,3I \0030,4L\0030,12D \0031,8D\0030,3r\0030,4a\0030,12w \0031,8F\0030,3o\0030,4u\0030,12r\002 \003 ";
    static final String UnoLogo		      = "\002\0033U\00312N\00313O\00308!\002\003";
    // Re-do this variable as an int[] or something
    static final String[]   UnoNickColors = "6 13 3 7 12 10 4 11 9 8 5".split(" ");
    static String[] lastMonthTop_CardsPlayed = new String[3];
    static String[] lastMonthTop_Wins = new String[3];
    static String   recordFast;
    static String   recordTopScore;
    static String   recordCardsPlayed;
    static String   recordGamesPlayed;
    static String   recordWins;
    static String   recordWinStreak;
    /**
     *  The Executor that runs the idle and game-start timers.
     *  Its thread pool size is 3 because we can only have 3 tasks scheduled at any one time (the start timer and skip timers are mutually exclusive).
     */
    ScheduledThreadPoolExecutor timerPool = new ScheduledThreadPoolExecutor(3);
    private Runnable UnoStartRun;
    private Delayed UnoStartTimer;
    
    private Runnable UnoSkipRun;
    private Delayed UnoSkipTimer;
    private Runnable UnoCycleRun;
    private Delayed UnoCycleTimer;
    private Runnable UnoBotRun;
    private Delayed UnoBotTimer;
    boolean UnoOn = false;
    int     UnoMode = 0;
    boolean UnoPaused = false;
    /** If someone is picking a color. */
    boolean isColorChange = false;
    boolean UnoWinDefault = false;
    int     UnoAdNumber = 0;
    int     CardsPlayed = 0;
    int    	currentPlayerIndex = 0;
    /** Direction of play. */
    boolean reversed = false;
    /** If someone just drew a card and has to play or pass. */
    boolean isDraw = false;
    int     UnoWinsInARow = 0;
    int     extraTimeCount = 0;
    Date	gameStart;
    /** LinkedList of players  */
    LinkedList</*PlayerStructure*/String>  players = new LinkedList</*PlayerStructure*/String>();
    // TODO: We shouldn't be using Strings for the cards..
    
    /** The master copy of the UNO! deck. */
    /*UnoCardStructure*/String[]    MasterDeck = new /*UnoCardStructure*/String[] { "B0","B1","B1","B2","B2","B3","B3","B4","B4","B5","B5","B6","B6","B7","B7","B8","B8","B9","B9","BR","BR","BS","BS","BD","BD","R0","R1","R1","R2","R2","R3","R3","R4","R4","R5","R5","R6","R6","R7","R7","R8","R8","R9","R9","RR","RR","RS","RS","RD","RD","Y0","Y1","Y1","Y2","Y2","Y3","Y3","Y4","Y4","Y5","Y5","Y6","Y6","Y7","Y7","Y8","Y8","Y9","Y9","YR","YR","YS","YS","YD","YD","G0","G1","G1","G2","G2","G3","G3","G4","G4","G5","G5","G6","G6","G7","G7","G8","G8","G9","G9","GR","GR","GS","GS","GD","GD","W","W","W","W","WD","WD","WD","WD"};
    
    /** The current UNO! deck. */
    LinkedList</*UnoCardStructure*/String>  UnoDeck = new LinkedList</*UnoCardStructure*/String>();
    
    /** The current UNO! discard pile. */
    LinkedList</*UnoCardStructure*/String>    DiscardPile = new LinkedList<String>();
    
    /** HashMap of Player -> ArrayList of cards. */
    HashMap</*PlayerStructure*/String,ArrayList</*UnoCardStructure*/String>> UnoHands = new HashMap</*PlayerStructure*/String,ArrayList</*UnoCardStructure*/String>>(3);
    
    /** HashMap of Player -> a color number-string. */
    HashMap</*PlayerStructure*/String,String> PlayerColors = new HashMap</*PlayerStructure*/String,String>(3);
    
    /** Last-spoke time for idle skipping. */
    HashMap</*PlayerStructure*/String,Date> IdleMap = new HashMap</*PlayerStructure*/String,Date>();
    /** The face-up card on the discard pile. */
    /*UnoCardStructure*/String  PlayCard = "";
    
    /** The card held from 'dr'. */
    /*UnoCardStructure*/String CardInHand = "";
    /** Whose turn it is. */
    /*PlayerStructure*/String  CurrentPlayer = "";
    /*PlayerStructure*/String  ColorPicker = "";
    /*PlayerStructure*/String  UnoLastIdler = "";
    /*PlayerStructure*/String  UnoLastWinner = "";
    /**
     *  Cycles the round-robin player list.
     *  @throws NoSuchElementException - if the players list is empty
     */
    public String cyclePlayer() {
        if(reversed) players.addFirst(players.removeLast());
        else         players.addLast(players.removeFirst());
        return getNextPlayer();
    }
    /** Draws cards from the deck. */
    ArrayList</*UnoCardStructure*/String> drawCards(int num) {
    	UnoShuffle(num);
    	ArrayList</*UnoCardStructure*/String> ret = new ArrayList</*UnoCardStructure*/String>(num);
    	for(int i=0; i<num; i++) {
    		ret.add(UnoDeck.removeFirst());
    	}
    	return ret;
    }
    /**
     *  Gets the next player to play, respecting the direction of play. Is not null.
     *  @return Name of the next player to play.
     *  @throws NoSuchElementException - if the players list is empty
     */
    @NotNull public String getNextPlayer() { if(reversed) return players.getLast(); else return players.getFirst(); }
    public boolean isListener() {return true;}
    
    /**
     * Logs an UNO! game to disk. The log is used for the top-scores.
     * @param bot
     */
    public void logGame(PircBotX bot, Channel channel, /*PlayerStructure*/String winner, int score) {
        if (!logEnabled) return;
        try {
             FileWriter fw1 = new FileWriter(gameLog,true);
             FileWriter fw2 = new FileWriter(gameLogMonth,true);
             StringBuilder sb = new StringBuilder();
             /**
              * Format:
              * date+duration;winner:points;loser;loser;.....;loser\n
              */
             sb.append(gameStart);
             sb.append('+');
             sb.append(new Date().getTime() - gameStart.getTime());
             sb.append(';');
             sb.append(winner.toString());
             sb.append(':');
             sb.append(score);
             for(/*PlayerStructure*/String s : players) {
            	 if (s != winner) {
            		 sb.append(';');
            		 sb.append(s.toString());
            	 }
             }
             sb.append('\n');
             String res = sb.toString();
             fw1.write(res);
             fw2.write(res);
             fw1.close();
             fw2.close();
        } catch(IOException e) {
            Shocky.sendChannel(bot,channel,"IO Exception during writing log: "+e.toString());
            e.printStackTrace();
        } finally {
        	
        }
    }
    public String name() {return "uno";}
    
    public void onDisable() {
        unoHelpCommand.isEnabled = false;
        // Clean up scheduled tasks
        timerPool.remove(UnoStartRun);
        timerPool.remove(UnoCycleRun);
        timerPool.remove(UnoSkipRun);
    }
    public void onEnable() {
        Command.addCommand("uno",unoHelpCommand = new CmdUnoHelp());
        unoHelpCommand.isEnabled = true;
        
        Data.config.setNotExists("unochannel","#uno");
        
        dataDir.mkdir();
        gameLog = new File(dataDir,"log_forever.log").getAbsolutePath();
        gameLogMonth = new File(dataDir,"log_month.log").getAbsolutePath();
        
        config.load(new File(dataDir,"config.cfg"));
        
        config.setNotExists("Debug",true); //############# Change this after testing is complete ######################
                                           //##########################################################################
        config.setNotExists("Ads",false);
        config.setNotExists("PointsName","Points");
        config.setNotExists("RobotName",Data.config.getString("main-botname"));
        config.setNotExists("OnlyOpsRequestBotPlayer",true); // Changed - added flag to disable
        config.setNotExists("JoinCycleDuration",45000); // Changed - 30 seconds -> 45 seconds
        config.setNotExists("ExtraJoinTime",20000); // Changed - added !wait
        config.setNotExists("ExtraJoinTime-rateLimit",3);
        config.setNotExists("JoinAnyTime",false);
        config.set/*NotExists*/("UseDCC",false); // Changed - setNotExists -> set
                                                 // DCC will not be implemented until version 2.
        config.setNotExists("BlackjackBonus",500); // Changed - 1000 -> 500
        config.setNotExists("BlackjackEnabled",true); // Changed - added flag to disable
        config.setNotExists("WildDrawTwos",false);
        config.setNotExists("WDFAnyTime",false);
        config.setNotExists("TruncateNicks",true); // Changed - added flag to disable
        config.setNotExists("MaxNickLen",18); // Changed - 9 -> 18
        config.setNotExists("MaxPlayers",10);
        config.setNotExists("GreetOnJoin",true);
        config.setNotExists("AutoSkipDelay",120000);
        
        // Uno static commands
        unocommands.put("!uno",new UCommand("!uno") {
            @Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
                UnoBindCmds();
                UnoNext(bot,channel);
            }
        });
        unocommands.put("!stop",new UCommand("!stop") {
            @Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
            	if (!Command.canUseAny(bot,type,channel,sender)) return;
            	UnoStop();
                Shocky.sendChannel(bot,channel,"Stopped by "+sender);
            }
        });
        unocommands.put("!join",new UCommand("!join") {
            @Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
                if (config.getBoolean("OnlyOpsRequestBotPlayer"))
                    if (!Command.canUseAny(bot,type,channel,sender)) return;
                if (UnoMode == 1 || (UnoMode == 2 && config.getBoolean("JoinAnyTime"))) {
                    UnoBotJoin();
                }
            }
        });
        unocommands.put("!wait",new UCommand("!wait") {
            @Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
                if (extraTimeCount > config.getInt("ExtraJoinTime-rateLimit"))
                {
                    Shocky.sendChannel(bot,channel,"You can only do 3 delays per game!");
                }
                extraTimeCount++;
                timerPool.remove(UnoStartRun);
                UnoStartRun = new UnoStartTask(bot,channel);
                UnoStartTimer = timerPool.schedule(UnoStartRun, UnoStartTimer.getDelay(TimeUnit.MILLISECONDS) + config.getInt("ExtraJoinTime"),TimeUnit.MILLISECONDS);
            }
        });
        // TODO: Highscore commands 
    }
    public void onMessage(MessageEvent<PircBotX> event) {
        //(event.getBot(),event.getUser(),Command.EType.Channel,callback,event.getMessage())
    }
    public int playerCount() { return players.size(); }
    void UnoBindCmds() {
        unocommands.put("jo", new UCommand("jo") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		if (!config.getBoolean("JoinAnyTime") && UnoMode == 2) {
        			Shocky.sendNotice(bot, sender, "The game is already in progress! Wait for the game to finish first.");
        			return;
        		}
        		if (playerCount() >= config.getInt("MaxPlayers")) {
        			Shocky.sendNotice(bot, sender, "Sorry, this game is full! Try next time.");
        		}
        		String nick = sender.toString();
        		if (players.contains(nick)) {
        			Shocky.sendNotice(bot, sender, "You are already in the game!");
        			return;
        		}
        		PlayerColors.put(nick,UnoNickColors[playerCount() % UnoNickColors.length]);
        		players.add(nick);
        		//deal hand
        		UnoHands.put(nick, drawCards(7));
        		Shocky.sendNotice(bot, sender, UnoPrintHand(nick));
        		//announce
        		StringBuilder sb = new StringBuilder(UnoNick(nick));
        		sb.append("\00301 joins ");
        		sb.append(UnoLogo);
        		Shocky.sendChannel(bot, channel, sb.toString());
        	}
        });
        unocommands.put("od", new UCommand("od") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		StringBuilder sb = new StringBuilder("Player order: \00314");
        		Iterator</*PlayerStructure*/String> i;
        		if(reversed) {
        			i = players.descendingIterator();
        		} else {
        			i = players.iterator();
        		}
        		while(i.hasNext()) {
        			/*PlayerStructure*/String nick = i.next();
        			sb.append(nick);
        			sb.append(' ');
        		}
        		Shocky.sendChannel(bot, channel, Utils.mungeAllNicks(channel, sb.toString()));
        	}
        });
        unocommands.put("ti", new UCommand("ti") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		Calendar c = Calendar.getInstance();
        		c.setTimeInMillis(new Date().getTime() - gameStart.getTime());
        		StringBuilder sb = new StringBuilder("Run time: ");
        		if(c.get(Calendar.HOUR) != 0) {
        			sb.append(c.get(Calendar.HOUR));
        			sb.append(" hours ");
        		}
        		if(c.get(Calendar.MINUTE) != 0) {
        			sb.append(c.get(Calendar.MINUTE));
        			sb.append(" minutes ");
        		}
        		if(c.get(Calendar.SECOND) != 0) {
        			sb.append(c.get(Calendar.SECOND));
        			sb.append(" seconds ");
        		}
        		if(c.get(Calendar.MILLISECOND) != 0) {
        			sb.append(c.get(Calendar.MILLISECOND));
        			sb.append(" milliseconds");
        		}
        		
        		Shocky.sendChannel(bot, channel, sb.toString());
        	}
        });
        unocommands.put("ca", new UCommand("ca") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		Shocky.sendNotice(bot, sender, UnoPrintHand(sender.toString()));
        	}
        });
        unocommands.put("cd", new UCommand("cd") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		Shocky.sendChannel(bot, channel, UnoCardColor(PlayCard));
        	}
        });
        unocommands.put("tu", new UCommand("tu") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		Shocky.sendChannel(bot, channel,  Utils.mungeAllNicks(channel, "It is "+CurrentPlayer+"'s turn"));
        	}
        });
        unocommands.put("ct", new UCommand("ct") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		StringBuilder sb = new StringBuilder();
        		for (String nick : players) {
        			sb.append(UnoNick(nick));
        			sb.append(" ");
        			sb.append(UnoHands.get(nick).size());
        			sb.append("cards\00301 ");
        		}
        		Shocky.sendChannel(bot, channel,  Utils.mungeAllNicks(channel, "It is "+CurrentPlayer+"'s turn"));
        	}
        });
        unocommands.put("st", new UCommand("st") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		Shocky.sendChannel(bot, channel, CardsPlayed + " cards played this round");
        	}
        });
        unocommands.put("pl", new UCommand("pl") {
        	@Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
        		// substring(2) is to erase the "pl"
        		UnoPlayCard(bot,channel,sender,message:substring(2));
        	}
        });
        //TODO
    }
    /** Colorizes a given card. */
    String UnoCardColor(/*UnoCardStructure*/String card) {
    	String c0,c1;
    	StringBuilder sb;
    	c0 = card.substring(0,0).toUpperCase();
    	// correctly handle length-1 cards
    	if (card.length() == 1) c1 = "";
    	else c1 = card.substring(1, 1).toUpperCase();

    	// *** Do not convert to switch unless running JRE 7
    	if (c0 == "W") {
    		if (c1 == "D") {
    			return UnoWildDrawFourCard;
    		}
    		return UnoWildCard;
    	}
		sb = new StringBuilder();
		if (c0 == "R") {
			sb.append(UnoRedCard);
    	} else if (c0 == "G") {
    		sb.append(UnoGreenCard);
    	} else if (c0 == "Y") {
    		sb.append(UnoYellowCard);
    	} else if (c0 == "B") {
    		sb.append(UnoBlueCard);
    	}
		if (c1 == "S") {
			sb.append(UnoSkipCard);
		} else if (c1 == "R") {
			sb.append(UnoReverseCard);
		} else if (c1 == "D") {
			sb.append(UnoDrawTwoCard);
		} else {
			sb.append(c1); sb.append(" \003");
		}
		return sb.toString();
    }
    /**
     *  Prepare for the 'next' game.
     */
    void UnoNext(PircBotX bot, Channel channel) {
        if (!UnoOn) return;
        UnoReset();
        UnoMode = 1;
        // Changed - Shuffling
        ArrayList</*UnoCardStructure*/String> newDeck = new ArrayList</*UnoCardStructure*/String>(108);
        for(String s : MasterDeck) {
        	newDeck.add(s);
        }
        Collections.shuffle(newDeck,random);
        UnoDeck.addAll(newDeck);
        newDeck = null;
        Shocky.sendChannel(bot, channel, "You have \00314\002[UnoDuration $StartGracePeriod]\002\003 to join "+UnoLogo);
        Shocky.sendChannel(bot, channel, "Say 'jo' to join or '!wait' to increase wait time.");
        
        // Start the waiting thread
        UnoStartRun = new UnoStartTask(bot,channel);
        UnoStartTimer = timerPool.schedule(UnoStartRun, config.getInt("JoinCycleDuration"), TimeUnit.MILLISECONDS);
    }
    /** Adds a color code in front of the player's name. */
    String UnoNick(String player) {
    	StringBuilder sb = new StringBuilder('\003');
    	sb.append(PlayerColors.get(player));
    	sb.append(player);
    	return sb.toString();
    }
    public void UnoPlayCard(PircBotX bot, Channel channel, User sender, String card) {
    	if (isColorChange) {
    		if (sender.toString() != CurrentPlayer) {
        		Shocky.sendNotice(bot, sender, "Please wait for "+CurrentPlayer+" to choose a color.");
    		} else {
        		Shocky.sendNotice(bot, sender, "Please choose a color with 'co [r|g|b|y]'.");
    		}
    		return;
    	}
    	if (sender.toString() != CurrentPlayer) {
    		Shocky.sendNotice(bot, sender, "It is not your turn! It is "+CurrentPlayer+"'s turn right now.");
    		return;
    	}
    	if (UnoMode != 2) {
    		Shocky.sendNotice(bot, sender, "It is not your turn! It is "+CurrentPlayer+"'s turn right now.");
    		return;
    	}
    	ResetSkipTimer();
    	// Erase all punctuation
    	card = card.replaceAll("[`,\\\\.!{}\t :;]", "");
    	if (card == "") {
    		if (isDraw) {
    			card = CardInHand;
    		} else {
				Shocky.sendNotice(bot, sender, "pl [card] - plays a card");
				Shocky.sendNotice(bot, sender, "[card] is a 1-character suit followed by a 0-9 r, d, or s.");
				return;
    		}
    	}
    	card = card.toUpperCase().substring(0, 1);
    	if(isDraw) {
    		if (card != CardInHand) {
    			Shocky.sendNotice(bot,sender, "You can only play the card you just drew. Otherwise you have to pass.");
    		}
    	}
    	
    	//TODO
    }
    void ResetIdleTime(User user) {
		
    	/*timerPool.remove(UnoSkipRun);
    	if (user.toString() == UnoLastIdler) {
    		UnoLastIdler = "";
    	}
    	if (UnoMode == 2) {
    		timerPool.schedule(new UnoSkipTask(bot,channel),30,TimeUnit.SECONDS);
    	}*/
    }
    String UnoPrintHand(/*PlayerStructure*/String nick) {
    	ArrayList</*UnoCardStructure*/String> hand = UnoHands.get(nick);
    	StringBuilder sb = new StringBuilder();
    	for(/*UnoCardStructure*/String card : hand) {
    		sb.append(UnoCardColor(card));
    	}
    	return sb.toString();
    }
    
    /** Resets variables and cancels timers in preparation to start a new game.*/
    void UnoReset() {
        UnoMode = 0;
        UnoPaused = false;
        players.clear();
        // MasterDeck = [list B0 B1 B1 B2 B2 B3 B3 B4 B4 B5 B5 B6 B6 B7 B7 B8 B8 B9 B9 BR BR BS BS BD BD R0 R1 R1 R2 R2 R3 R3 R4 R4 R5 R5 R6 R6 R7 R7 R8 R8 R9 R9 RR RR RS RS RD RD Y0 Y1 Y1 Y2 Y2 Y3 Y3 Y4 Y4 Y5 Y5 Y6 Y6 Y7 Y7 Y8 Y8 Y9 Y9 YR YR YS YS YD YD G0 G1 G1 G2 G2 G3 G3 G4 G4 G5 G5 G6 G6 G7 G7 G8 G8 G9 G9 GR GR GS GS GD GD W W W W WD WD WD WD};
        UnoDeck = new LinkedList<String>();
        DiscardPile = new LinkedList<String>();
        PlayCard = "";
        CurrentPlayer = "";
        isColorChange = false;
        currentPlayerIndex = 0;
        ColorPicker = "";
        isDraw = false;
        CardsPlayed = 0;
        timerPool.remove(UnoStartRun);
        timerPool.remove(UnoCycleRun);
        timerPool.remove(UnoSkipRun);
        timerPool.remove(UnoBotRun);
    }
    
    /** Reshuffles the discard pile into the deck if necessary. */
    void UnoShuffle(int num) {
    	if (UnoDeck.size() <= num)
    	{
    		// Do the truffle shuffle as shuffling a LinkedList is a bad idea.
    		ArrayList</*UnoCardStructure*/String> newDeck = new ArrayList</*UnoCardStructure*/String>(108);
    		newDeck.addAll(UnoDeck);
    		newDeck.addAll(DiscardPile);
    		DiscardPile.clear();
    		Collections.shuffle(newDeck,random);
    		UnoDeck = new LinkedList</*UnoCardStructure*/String>();
    		UnoDeck.addAll(newDeck);
    	}
    }
	/**
     *  Start the game!
     */
    void UnoStart(PircBotX bot, Channel channel) {
        if (!UnoOn) return;
        if (players.isEmpty()) {
            Shocky.sendChannel(bot,channel,"No players! Use !uno to restart.");
            UnoStop();
        }
        gameStart = new Date();
        if (players.size() == 1) {
        	UnoBotPlayerJoins();
        }
        Shocky.sendChannel(bot,channel,"Welcome to "+UnoLogo);
        {
        	StringBuilder sb = new StringBuilder(playerCount());
        	sb.append(" players this round: ");
        	for (String nick : players) {
        		sb.append(nick);
        		sb.append(" ");
        	}
        	Shocky.sendChannel(bot,channel,sb.toString());
        }
        UnoMode = 2;
        CurrentPlayer = getNextPlayer();
        String drawncard = drawCards(1).get(0);
        while(drawncard.substring(0,0) == "W") {
        	Shocky.sendChannel(bot,channel,"Drew "+UnoWildCard+" card, trying again...");
        	UnoDeck.addLast(drawncard); // Add back into the deck so that the card doesn't disappear
        	drawncard = drawCards(1).get(0);
        }
        Shocky.sendChannel(bot,channel,"Card: "+UnoCardColor(drawncard));
        
    }
	/** Stops the UNO! game. */
    void UnoStop() {
        timerPool.remove(UnoStartRun);
        timerPool.remove(UnoBotRun);
        UnoOn = false;
        UnoPaused = false;
        UnoLastWinner = "";
        UnoWinsInARow = 0;
        UnoUnbindCmds();
        UnoReset();
    }
}