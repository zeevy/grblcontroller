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

package in.co.gorest.grblcontroller.listners;


import android.databinding.BaseObservable;
import android.databinding.Bindable;

import java.io.File;
import java.util.ArrayList;

import in.co.gorest.grblcontroller.BR;
import in.co.gorest.grblcontroller.model.GcodeCommand;

public class FileSenderListner extends BaseObservable {

    private String gcodeFileName;
    private File gcodeFile;
    private Integer rowsInFile;
    private Integer rowsSent;

    private String status;

    public static final String STATUS_IDLE = "Idle";
    public static final String STATUS_READING = "Reading";
    public static final String STATUS_STREAMING = "Streaming";

    private long jobStartTime = 0L;
    private long jobEndTime = 0L;
    private String elaspsedTime = "00:00:00";

    private static FileSenderListner fileSenderListner = null;
    public static FileSenderListner getInstance(){
        if(fileSenderListner == null) fileSenderListner = new FileSenderListner();
        return fileSenderListner;
    }

    private FileSenderListner(){
        this.setStatus(STATUS_IDLE);
        this.gcodeFileName = "File types .tap | .gcode | .nc | .ngc";
        this.gcodeFile = null;
        this.rowsInFile = 0;
        this.rowsSent = 0;
    }

    @Bindable
    public String getGcodeFileName(){ return this.gcodeFileName; }
    private void setGcodeFileName(String gcodeFileName){
        this.gcodeFileName = gcodeFileName;
        notifyPropertyChanged(BR.gcodeFileName);
    }

    @Bindable
    public File getGcodeFile(){ return this.gcodeFile; }
    public void setGcodeFile(File gcodeFile){
        this.gcodeFile = gcodeFile;
        this.setGcodeFileName(gcodeFile.getName());
        notifyPropertyChanged(BR.gcodeFile);
    }

    @Bindable
    public Integer getRowsInFile(){ return this.rowsInFile; }
    public void setRowsInFile(Integer rowsInFile){
        this.rowsInFile = rowsInFile;
        notifyPropertyChanged(BR.rowsInFile);
    }

    @Bindable
    public Integer getRowsSent(){ return this.rowsSent; }
    public void setRowsSent(Integer rowsSent){
        this.rowsSent = rowsSent;
        notifyPropertyChanged(BR.rowsSent);
    }

    @Bindable
    public long getJobStartTime(){ return this.jobStartTime; }
    public void setJobStartTime(long startTime){
        this.jobStartTime = startTime;
        notifyPropertyChanged(BR.jobStartTime);
    }

    @Bindable
    public long getJobEndTime(){ return this.jobEndTime; }
    public void setJobEndTime(long endTime){
        this.jobEndTime = endTime;
        notifyPropertyChanged(BR.jobEndTime);
    }

    @Bindable
    public String getElaspsedTime(){ return this.elaspsedTime; }
    public void setElaspsedTime(String elaspsedTime){
        this.elaspsedTime = elaspsedTime;
        notifyPropertyChanged(BR.elaspsedTime);
    }

    @Bindable
    public String getStatus(){ return this.status; }
    public void setStatus(String status){
        this.status = status;
        notifyPropertyChanged(BR.status);
    }

}
