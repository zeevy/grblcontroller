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

import in.co.gorest.grblcontroller.util.GcodePreprocessorUtils;

public class GrblGcodeCommandEvent {

    private String command;

    public GrblGcodeCommandEvent(String command){
        this.command = processCommand(command);

    }

    public String getCommand(){ return this.command; }
    public void setCommand(String command){ this.command = command; }

    private String processCommand(String command){
        command = GcodePreprocessorUtils.removeWhiteSpace(command);
        command = GcodePreprocessorUtils.removeComment(command);

        return command;
    }

}