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

package in.co.gorest.grblcontroller.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GcodePreprocessorUtils {

    private static final String EMPTY = "";
    private static final Pattern WHITE_SPACE = Pattern.compile("\\s");
    private static final Pattern COMMENT = Pattern.compile("\\(.*\\)|\\s*;.*|%$");
    private static final Pattern COMMENT_PARSE = Pattern.compile("(?<=\\()[^()]*|(?<=;).*|%");

    public static String removeWhiteSpace(String command){
        return WHITE_SPACE.matcher(command).replaceAll(EMPTY).toUpperCase();
    }

    public static String parseComment(String command) {
        String comment = EMPTY;

        Matcher matcher = COMMENT_PARSE.matcher(command);
        if(matcher.find()) comment = matcher.group(0);
        return comment;
    }

    public static String removeComment(String command) {
        return COMMENT.matcher(command).replaceAll(EMPTY);
    }

}
