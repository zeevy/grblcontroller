/*
 *
 *  *  /**
 *  *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *  *
 *  *  * This program is free software; you can redistribute it and/or modify
 *  *  * it under the terms of the GNU General Public License as published by
 *  *  * the Free Software Foundation; either version 2 of the License, or
 *  *  * (at your option) any later version.
 *  *  *
 *  *  * This program is distributed in the hope that it will be useful,
 *  *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *  * GNU General Public License for more details.
 *  *  *
 *  *  * You should have received a copy of the GNU General Public License along
 *  *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  *  * <http://www.gnu.org/licenses/>
 *  *
 *
 */

package in.co.gorest.grblcontroller.model;

import com.orm.SugarRecord;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class GrblNotification extends SugarRecord {

    public String title;
    public String message;
    public String type;
    public String categoryName;
    public String categoryValue;
    public String playload;
    public String recievedOn;
    public String status;

    public GrblNotification(){}

    public GrblNotification(String title, String message){
        this.title = title;
        this.message = message;
        this.recievedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        this.status = "unread";
    }

    public GrblNotification(String title, String message, String type, String categoryName, String categoryValue, String playload){
        this.title = title;
        this.message = message;
        this.type = type;
        this.categoryName = categoryName;
        this.categoryValue = categoryValue;
        this.playload = playload;
        this.recievedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        this.status = "unread";
    }

    public void setTitle(String title){ this.title = title; }
    public String getTitle(){ return this.title; }

    public void setMessage(String message){ this.message = message; }
    public String getMessage(){ return this.message; }

    public void setType(String type){ this.type = type; }
    public String getType(){ return this.type; }

    public void setCategoryName(String categoryName){ this.categoryName = categoryName; }
    public String getCategoryName(){ return this.categoryName; }

    public void setCategoryValue(String categoryValue){ this.categoryValue = categoryValue; }
    public String getCategoryValue(){ return this.categoryValue; }

    public void setPlayload(String playload){ this.playload = playload; }
    public String getPlayload(){ return this.playload; }

    public void setRecievedOn(String recievedOn){ this.recievedOn = recievedOn; }
    public String getRecievedOn(){ return this.recievedOn; }

    public void setStatus(String status){ this.status = status; }
    public String getStatus(){ return this.status; }

    public String getNotificationTime(){
        if(this.recievedOn != null){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            try {
                return new SimpleDateFormat("d MMM, yyyy HH:mm:ss", Locale.ENGLISH).format(simpleDateFormat.parse(this.recievedOn));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}