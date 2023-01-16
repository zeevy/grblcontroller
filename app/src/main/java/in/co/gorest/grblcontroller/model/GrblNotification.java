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
import java.util.Objects;

public class GrblNotification extends SugarRecord {

    public String title;
    public String message;
    public String type;
    public String categoryName;
    public String categoryValue;
    public String payload;
    public String receivedOn;
    public String status;

    public GrblNotification(){}

    public GrblNotification(String title, String message){
        this.title = title;
        this.message = message;
        this.receivedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        this.status = "unread";
    }

    public GrblNotification(String title, String message, String type, String categoryName, String categoryValue, String payload){
        this.title = title;
        this.message = message;
        this.type = type;
        this.categoryName = categoryName;
        this.categoryValue = categoryValue;
        this.payload = payload;
        this.receivedOn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime());
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

    public void setPayload(String payload){ this.payload = payload; }
    public String getPayload(){ return this.payload; }

    public void setReceivedOn(String receivedOn){ this.receivedOn = receivedOn; }
    public String getReceivedOn(){ return this.receivedOn; }

    public void setStatus(String status){ this.status = status; }
    public String getStatus(){ return this.status; }

    public String getNotificationTime(){
        if(this.receivedOn != null){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            try {
                return new SimpleDateFormat("d MMM, yyyy HH:mm:ss", Locale.ENGLISH).format(Objects.requireNonNull(simpleDateFormat.parse(this.receivedOn)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}