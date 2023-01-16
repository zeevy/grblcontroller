/*
 *  /**
 *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation; either version 2 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  * <http://www.gnu.org/licenses/>
 *
 */

package in.co.gorest.grblcontroller.events;


import androidx.annotation.NonNull;

import in.co.gorest.grblcontroller.GrblController;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.util.GrblLookups;

public class GrblAlarmEvent {

    private final String message;

    private int alarmCode;
    private String alarmName;
    private String alarmDescription;

    public GrblAlarmEvent(GrblLookups lookups, String message){
        this.message = message;

        String[] inputParts = message.split(":");
        if(inputParts.length == 2){
            String[] lookup = lookups.lookup(inputParts[1].trim());
            if(lookup != null){
                this.alarmCode = Integer.parseInt(lookup[0]);
                this.alarmName = lookup[1];
                this.alarmDescription = lookup[2];
            }
        }
    }

    @NonNull
    @Override
    public String toString(){
        return GrblController.getInstance().getString(R.string.text_grbl_alarm_format, alarmCode, alarmDescription);
    }

    public String getMessage(){ return this.message; }

    public int getAlarmCode(){return this.alarmCode; }

    public String getAlarmName(){
        return this.alarmName;
    }

    public String getAlarmDescription(){
        return this.alarmDescription;
    }

}
