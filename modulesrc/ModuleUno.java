import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Random;

import pl.shockah.Config;
import pl.shockah.shocky.cmds.Command;

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
public class ModuleUno extends Module implements ActionListener {
    // As a note, I am using package-access a lot. This is to signify the concept of "it shouldn't be public, but it's not really private either".
    // It also makes a nice analog to the Tcl script.
    Config config = new Config();
    public String unochannel;
    public static Random random = new Random(); // static random instance for shuffling
    File dataDir = new File("data","uno");
    /**
     *  This file logs all UNO! games and is only read when recalculating all-time highscores and records.
     *  Generally, it will be safe to delete the file if it is getting too large.
     *  Located at ./data/uno/uno_games.log
     */
    File gameLog;
    /**
     *  This file is the subset of the gameLog starting from the beginning of the month.
     *  This file will be cleared on the first of the month.
     *  Located at ./data/uno/uno_month.log
     */
    File gameLogMonth;
    /**
     *  Enables or disables logging. Is in config.
     */
    public boolean logEnabled;
    public synchronized HashMap<String,UCommand> unocommands;
    protected Command unoHelpCmd;
    
    public String name() {return "uno";}
    public boolean isListener() {return true;}
    public void onDisable() {
        unoHelpCmd.isEnabled = false;
        // Clean up scheduled tasks
        timerPool.remove(UnoStartTimer);
        timerPool.remove(UnoCycleTimer);
        timerPool.remove(UnoSkipTimer);
    }
    public void onEnable() {
        Command.addCommand("uno",unoHelpCmd = new CmdUnoHelp());
        unoHelpCmd.isEnabled = true;
        
        Data.config.setNotExists("unochannel","#uno");
        
        dataDir.mkdir();
        gameLog = new File(logDir,"log_forever.log");
        gameLogMonth = new File(logDir,"log_month.log");
        
        config.load(new File(logDir,"config.cfg"));
        
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
                                                 // We're not using DCC due to limited resources on our server.
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
        unocommands.put("!uno",UCommand("!uno") {
            @Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
                UnoBindCmds();
                UnoNext(bot,channel);
            }
        });
        unocommands.put("!stop",new UCommand("!stop") {
            @Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
                if (!Command.canUseAny(bot,type,channel,sender)) return;
                timerPool.remove(UnoStartTimer);
                timerPool.remove(UnoBotTimer);
                UnoOn = false;
                UnoPaused = false;
                joinCyclesElapsed = 0;
                UnoLastWinner = "";
                UnoWinsInARow = 0;
                UnoUnbindCmds();
                UnoReset();
                Shocky.sendChannel(bot,channel,"Stopped by "+sender);
            }
        });
        unocommands.put("!join",new UCommand("!join") {
            @Override public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
                if (config.getBoolean("OnlyOpsRequestBotPlayer"))
                    if (!Command.canUseAny(bot,type,channel,sender)) return;
                if (UnoMode == 1 || (UnoMode == 2 && config.getBoolean("JoinAnyTime"))) {
                    UnoBotJoin()
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
                timerPool.remove(UnoStartTimer);
                UnoStartTimer = timerPool.schedule(new UnoStartTask, UnoStartTimer.getDelay() + config.getInt("ExtraJoinTime"));
            }
        });
    }
    public void onMessage(MessageEvent<PircBotX> event) {
        //(event.getBot(),event.getUser(),Command.EType.Channel,callback,event.getMessage())
    }
    public class CmdUnoHelp extends Command {
        /*package*/ boolean isEnabled = true;
        public String command() {return "uno";}
        public String help(PircBotX bot, EType type, Channel channel, User sender) {
            if (!isEnabled) return "uno - Shocky's UNO! is currently disabled. Sorry!"
            return "uno\nStarts an UNO! game, when used in "+unochannel+".";
        }
        public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
            if ((!isEnabled) || (channel.toString() != unochannel)) {
                callback.type = EType.Notice;
                callback.append(help(bot,type,channel,sender));
            }
            unocommands.get("!uno").doCommand(bot,type,callback,channel,sender,message);
        }
    }
    /**
     * Logs an UNO! game to disk. The log is used for the top-scores.
     * @param bot
     */
    public void logGame(PircBotX bot, Channel channel) {
        if (!logEnabled) return;
        try {
            FileWriter fw1 = new FileWriter(gameLog);
        } catch(IOException e) {
            Shocky.say(
        }
    }
    /*
     *  Begin direct .tcl imports
     */
    static final String UnoRedCard		  = "\0030,04 Red ";
    static final String UnoGreenCard	      = "\0030,03 Green ";
    static final String UnoBlueCard	      = "\0030,12 Blue ";
    static final String UnoYellowCard	      = "\0031,08 Yellow ";
    static final String UnoSkipCard		  = "\002Skip\002 \003 ";
    static final String UnoReverseCard	  = "\002Reverse\002 \003 ";
    static final String UnoDrawTwoCard	  = "\002Draw Two\002 \003 ";
    static final String UnoWildCard		  = "\0031,8 \002W\0030,3I \0030,4L\0030,12D\002 \003 ";
    static final String UnoWildDrawFourCard = "\0031,8 \002W\0030,3I \0030,4L\0030,12D \0031,8D\0030,3r\0030,4a\0030,12w \0031,8F\0030,3o\0030,4u\0030,12r\002 \003 ";
    static final String UnoLogo		      = "\002\0033U\00312N\00313O\00308!\002\003";
    // Re-do this variable as an int[] or something
    static String   UnoNickColors = "6 13 3 7 12 10 4 11 9 8 5";
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
    private ScheduledFuture UnoStartTimer;
    private ScheduledFuture UnoSkipTimer;
    private ScheduledFuture UnoCycleTimer;
    private ScheduledFuture UnoBotTimer;
    boolean UnoOn = false;
    int     UnoMode = 0;
    boolean UnoPaused = false;
    boolean isColorChange = false;
    boolean UnoWinDefault = false;
    int     UnoAdNumber = 0;
    int     CardsPlayed = 0;
    int     currentPlayerIndex = 0;
    boolean reversed = false;
    boolean isDraw = false;
    int     UnoWinsInARow = 0;
    int     extraTimeCount = 0;
    /**
     *  The tcl script used a string to do a round-robin. We're using a LinkedList.
     */
    LinkedList</*PlayerStructure*/String>  players = new LinkedList</*PlayerStructure*/String>();
    public int playerCount() { return players.size(); }
    // We shouldn't be using Strings for the cards. Should optimize.
    /*UnoCardStructure*/String[]    MasterDeck = new /*UnoCardStructure*/String[] { "B0","B1","B1","B2","B2","B3","B3","B4","B4","B5","B5","B6","B6","B7","B7","B8","B8","B9","B9","BR","BR","BS","BS","BD","BD","R0","R1","R1","R2","R2","R3","R3","R4","R4","R5","R5","R6","R6","R7","R7","R8","R8","R9","R9","RR","RR","RS","RS","RD","RD","Y0","Y1","Y1","Y2","Y2","Y3","Y3","Y4","Y4","Y5","Y5","Y6","Y6","Y7","Y7","Y8","Y8","Y9","Y9","YR","YR","YS","YS","YD","YD","G0","G1","G1","G2","G2","G3","G3","G4","G4","G5","G5","G6","G6","G7","G7","G8","G8","G9","G9","GR","GR","GS","GS","GD","GD","W","W","W","W","WD","WD","WD","WD"};
    LinkedList</*UnoCardStructure*/String>  UnoDeck = new LinkedList</*UnoCardStructure*/String>();
    List</*UnoCardStructure*/String>    DiscardPile = new LinkedList<String>();
    HashMap</*PlayerStructure*/String,/*UnoHandStructure*/String> UnoHands = new HashMap</*PlayerStructure*/String,/*UnoHandStructure*/String>(3);
    /*UnoCardStructure*/String  PlayCard = "";
    /*PlayerStructure*/String  CurrentPlayer = "";
    /*PlayerStructure*/String  ColorPicker = "";
    /*PlayerStructure*/String  UnoLastIdler = "";
    
    /**
     *  Gets the next player to play, respecting the direction of play. Is not null.
     *  @return Name of the next player to play.
     *  @throws NoSuchElementException - if the players list is empty
     */
    @NotNull public String getNextPlayer() { if(reversed) return players.getLast(); else return players.getFirst(); }
    /**
     *  Cycles the round-robin playerlist.
     *  @throws NoSuchElementException - if the players list is empty
     */
    public void cyclePlayer() {
        if(reversed) players.addFirst(players.removeLast());
        else         players.addLast(players.removeFirst());
    }
    
    void UnoBindCmds() {
        unocommands.put("jo",
    }
    void UnoReset() {
        UnoMode = 0
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
        timerPool.remove(UnoStartTimer);
        timerPool.remove(UnoCycleTimer);
        timerPool.remove(UnoSkipTimer);
        timerPool.remove(UnoBotTimer);
    }
    /**
     *  Prepare for the 'next' game.
     */
    void UnoNext(PircBotX bot, Channel channel) {
        if (!UnoOn) return;
        UnoReset();
        UnoMode = 1;
        // Changed - Shuffling
        UnoDeck.addAll(MasterDeck);
        Collections.shuffle(UnoDeck,random);
        Shocky.sendChannel(bot, channel, "You have \00314\002[UnoDuration $StartGracePeriod]\002\003 to join uno.");
        Shocky.sendChannel(bot, channel, "Say 'jo' to join or '!wait' to increase wait time.");
        
        // Start the waiting thread
        final PircBotX pbot = bot;
        final Channel chan = channel;
        UnoStartTimer = timerPool.schecule(new UnoStartTask(),config.getInt("JoinCycleDuration"));        
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
    }
    
    
    
    public class UnoStartTask extends Runnable() {
        @Override public void run() { UnoStart(pbot,chan); }
    }
}
private class UCommand {
    public UCommand(String cmd) {
        trigger = cmd;
    }
    public String trigger;
    public abstract void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message);
}
private enum UnoMode {
    Off, Joining, 
}