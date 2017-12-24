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

public class GcodeCommand {

    private String command;
    private final String comment;
    private final Boolean hasModalSet;

    public GcodeCommand(String command) {
        this.command = GcodePreprocessorUtils.removeWhiteSpace(command);
        this.comment = GcodePreprocessorUtils.parseComment(command);
        if(this.getHasComment()) this.command = GcodePreprocessorUtils.removeComment(command);
        this.hasModalSet = this.checkModalSet();
    }

    public String getCommandString() {
        return this.command;
    }

    private boolean getHasComment() {
        return this.comment != null && this.comment.length() != 0;
    }

    public Boolean getHasModalSet(){
        return this.hasModalSet;
    }

    private boolean checkModalSet(){
        return this.command.contains("G54") || this.command.contains("G55") || this.command.contains("G56")
                || this.command.contains("G57") || this.command.contains("G58") || this.command.contains("G59")
                || this.command.contains("G59.1") || this.command.contains("G59.2") ||this.command.contains("G59.3") || this.command.contains("G10") || this.command.contains("G28.1") || this.command.contains("G30.1")
                || this.command.contains("G20") || this.command.contains("G21") || this.command.contains("G90") || this.command.contains("G91") || this.command.contains("G28") || this.command.contains("G30");

    }

}
