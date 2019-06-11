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

import in.co.gorest.grblcontroller.util.GcodePreprocessorUtils;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class GcodeCommand {

    private String command;
    private String comment;
    private Integer size;
    private Boolean hasRomAccess;

    public GcodeCommand(){}

    public GcodeCommand(String command) {
        this.command = command;
        this.parseCommand();
    }

    public void setCommand(String command){
        this.command = command;
        this.parseCommand();
    }

    private void parseCommand(){
        this.comment = GcodePreprocessorUtils.parseComment(command);
        if(this.getHasComment()) this.command = GcodePreprocessorUtils.removeComment(command);
        this.command = GcodePreprocessorUtils.removeWhiteSpace(command);
        this.size = this.command.length() + 1;
        this.hasRomAccess = GrblUtils.hasRomAccess(this.command);
    }

    public String getCommandString() {
        return this.command;
    }

    private boolean getHasComment() {
        return this.comment != null && this.comment.length() != 0;
    }

    public Boolean getHasRomAccess(){ return this.hasRomAccess; }
    public void setHasRomAccess(Boolean hasRomAccess) { this.hasRomAccess = hasRomAccess; }
    public Integer getSize(){ return this.size; }


}
