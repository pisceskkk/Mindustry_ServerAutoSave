package cn.pisceskkk;

import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.io.SaveIO;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.*;

import static mindustry.Vars.saveDirectory;
import static mindustry.Vars.saveExtension;

public class ServerAutoSave extends Plugin{
    private boolean enabled = true;
    private enum opts {
        wave,
        minute
    }
    private opts mode=opts.wave;
    private int interval = 10;
    private int max_saves = 10;
    private Timer timer=new Timer();

    //called when game initializes
    @Override
    public void init(){
        Events.on(EventType.WaveEvent.class, event->{
            if (enabled && mode == opts.wave) {
                if(Vars.state.wave % interval == 0) {
                    saveGame(String.format("wave%d",Vars.state.wave));
                }
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("autosave", "[x] [<w/m>]", "auto-save game per x waves/minutes(default 10 waves). When x=0, disable autosave.", arg -> {
            if (arg.length == 0) {
                if(enabled){
                    Log.info(String.format("Autosave status:"));
                    Log.info(String.format("\tautosave %d %s(s)",interval, mode.toString()));
                    Log.info(String.format("\tat most %d save(s)",max_saves));
                    Log.info(String.format("Usage: autosave x <w/m> - auto-save game per x waves/minutes."));
                }
                else {
                    Log.info("Autosave disabled.");
                }
            }
            else if (arg.length == 1){
                try {
                    enabled = true;
                    interval = Integer.parseInt(arg[0]);
                    mode = opts.wave;
                    if(interval != 0)
                        Log.info("Default: Set the third parameter as default value:'wave'");
                }catch(NumberFormatException e){
                    Log.info("Error: Second parameter required: integer number");
                }
            }
            else {
                try {
                    enabled = true;
                    interval = Integer.parseInt(arg[0]);
                    if (arg[1].toLowerCase().startsWith("w"))
                        mode = opts.wave;
                    else if(arg[1].toLowerCase().startsWith("m")) {
                        mode = opts.minute;
                    }
                    else {
                        Log.info("Error: Third parameter required: <w/m>");
                    }
                }catch(NumberFormatException e){
                    Log.info("Error: Second parameter required: integer number");
                }
            }
            if(interval == 0 && enabled==true){
                enabled = false;
                Log.info("Autosave disabled.");
            }
            startSchedule();
        });

        handler.register("maxsave", "[x]", "auto-save at most x slots(default 10 slots).", arg -> {
            if (arg.length == 0) {
                if (enabled) {
                    Log.info(String.format("Autosave status:"));
                    Log.info(String.format("\tautosave %d %s(s)",interval, mode.toString()));
                    Log.info(String.format("\tat most %d save(s)",max_saves));
                    Log.info(String.format("Usage: maxsave x - set maxsave as x"));
                } else {
                    Log.info("Autosave disabled.");
                }
            } else {
                try {
                    max_saves = Integer.parseInt(arg[0]);
                } catch (NumberFormatException e) {
                    Log.info("Error: Second parameter required: integer number");
                }
            }
            if(enabled == false){
                Log.info("Warning: Autosave is disabled.");
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("autosave", "[x] [<w/m>]", "auto-save game per x waves/minutes(default 10 waves). When x=0, disable autosave.", (arg, player) -> {
            if (arg.length == 0) {
                if(enabled){
                    player.sendMessage(String.format("Autosave status:"));
                    player.sendMessage(String.format("\tautosave %d %s(s)",interval, mode.toString()));
                    player.sendMessage(String.format("\tat most %d save(s)",max_saves));
                    player.sendMessage(String.format("Usage: autosave x <w/m> - auto-save game per x waves/minutes."));
                }
                else {
                    player.sendMessage("Autosave disabled.");
                }
            }
            else if (arg.length == 1){
                try {
                    enabled = true;
                    interval = Integer.parseInt(arg[0]);
                    mode = opts.wave;
                    if(interval != 0)
                        player.sendMessage("Default: Set the third parameter as default value:'wave'");
                }catch(NumberFormatException e){
                    player.sendMessage("Error: Second parameter required: integer number");
                }
            }
            else {
                try {
                    enabled = true;
                    interval = Integer.parseInt(arg[0]);
                    if (arg[1].toLowerCase().startsWith("w"))
                        mode = opts.wave;
                    else if(arg[1].toLowerCase().startsWith("m")) {
                        mode = opts.minute;
                    }
                    else {
                        player.sendMessage("Error: Third parameter required: <w/m>");
                    }
                }catch(NumberFormatException e){
                    player.sendMessage("Error: Second parameter required: integer number");
                }
            }
            if(interval == 0 && enabled==true){
                enabled = false;
                player.sendMessage("Autosave disabled.");
            }
            startSchedule();
        });

        handler.<Player>register("maxsave", "[x]", "auto-save at most x slots(default 10 slots).", (arg, player) -> {
            if (arg.length == 0) {
                if (enabled) {
                    player.sendMessage(String.format("Autosave status:"));
                    player.sendMessage(String.format("\tautosave %d %s(s)",interval, mode.toString()));
                    player.sendMessage(String.format("\tat most %d save(s)",max_saves));
                    player.sendMessage(String.format("Usage: maxsave x - set maxsave as x"));
                } else {
                    player.sendMessage("Autosave disabled.");
                }
            } else {
                try {
                    max_saves = Integer.parseInt(arg[0]);
                } catch (NumberFormatException e) {
                    player.sendMessage("Error: Second parameter required: integer number");
                }
            }
            if(enabled == false){
                player.sendMessage("Warning: Autosave is disabled.");
            }
        });
    }

    private void startSchedule(){
        timer.cancel();
        if(!enabled || mode != opts.minute){
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                saveGame("minute");
            }
        }, 0, interval*60*1000);
    }

    private void saveGame(String suffix){
        if(!Vars.state.is(GameState.State.playing) || Vars.state.serverPaused == true){
            return;
        }
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("_yyyy_MM_dd_hhmm");
        String prefix = "autosave_";
        Fi file = saveDirectory.child(prefix + suffix + ft.format(dNow) + "." + saveExtension);
        Fi latest = saveDirectory.child("latest" + "." + saveExtension);
        Fi[] files = saveDirectory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix);
            }
        });
        Arrays.sort(files, new Comparator<Fi>() {
            public int compare(Fi f1, Fi f2) {
                long diff = f1.lastModified() - f2.lastModified();
                if (diff > 0)
                    return -1;
                else if (diff == 0)
                    return 0;
                else
                    return 1;
            }
            public boolean equals(Object obj) {
                return true;
            }
        });
        if(files.length >= max_saves){
            for(int index=max_saves-1; index<files.length; index++){
                files[index].delete();
            }
        }
        Core.app.post(() -> {
            SaveIO.save(file);
            SaveIO.save(latest);
            Log.info("Autosaved to @.",file);
        });
    }
}
