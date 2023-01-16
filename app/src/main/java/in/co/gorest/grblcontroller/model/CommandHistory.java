package in.co.gorest.grblcontroller.model;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.orm.util.NamingHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CommandHistory extends SugarRecord {

    public String command;
    public String gcode;
    public Integer usageCount;
    public String lastUsedOn;

    public CommandHistory(){}

    public CommandHistory(String command, String gcode){
        this.command = command;
        this.gcode = gcode;
        this.usageCount = 0;
        this.lastUsedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());
    }

    public String getCommand(){ return this.command; }
    public void setCommand(String command){ this.command = command; }

    public String getGcode(){ return this.gcode; }
    public void setGcode(String gcode){ this.gcode = gcode; }

    public Integer getUsageCount(){ return this.usageCount; }
    public void setUsageCount(Integer usageCount){ this.usageCount = usageCount; }

    public String getLastUsedOn(){ return this.lastUsedOn; }
    public void updateLastUsedOn(){
        this.lastUsedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());
    }

    public static void saveToHistory(String command, String gcode){
        CommandHistory commandHistory = Select.from(CommandHistory.class)
                .where(Condition.prop("gcode").eq(gcode))
                .first();

        if(commandHistory == null) commandHistory = new CommandHistory(command, gcode);
        commandHistory.usageCount++;
        commandHistory.updateLastUsedOn();

        commandHistory.save();
    }

    public static List<CommandHistory> getHistory(String offset, String limit){
        return Select.from(CommandHistory.class)
                .orderBy(NamingHelper.toSQLNameDefault("usageCount") + " DESC, " + NamingHelper.toSQLNameDefault("lastUsedOn") + " DESC")
                .limit(offset + ", " + limit).list();
    }

}
