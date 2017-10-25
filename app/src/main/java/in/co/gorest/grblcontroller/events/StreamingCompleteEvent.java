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

public class StreamingCompleteEvent {

    private String message;
    private String fileName;
    private int rowsSent;
    private long timeMillis;
    private String timeTaken;

    public StreamingCompleteEvent(String message){
        this.message = message;
    }

    public StreamingCompleteEvent(String message, String fileName, int rowsSent, long timeMillis, String timeTaken){
        this.message = message;
        this.fileName = fileName;
        this.rowsSent = rowsSent;
        this.timeMillis = timeMillis;
        this.timeTaken = timeTaken;
    }

    public String getMessage(){ return this.message; }
    public void setMessage(String message){ this.message = message; }

    public String getFileName(){ return this.fileName; }
    public void setFileName(String fileName){ this.fileName = fileName; }

    public int getRowsSent(){ return this.rowsSent; }
    public void setRowsSent(int rowsSent){ this.rowsSent = rowsSent; }

    public long getTimeMillis(){ return this.timeMillis; }
    public void setTimeMillis(long timeMillis){ this.timeMillis = timeMillis; }

    public String getTimeTaken(){ return this.timeTaken; }
    public void setTimeTaken(String timeTaken){ this.timeTaken = timeTaken; }

}
