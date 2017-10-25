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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GcodePreprocessorUtils {

    private static final String EMPTY = "";
    private static final Pattern WHITE_SPACE = Pattern.compile("\\s");
    private static final Pattern COMMENT = Pattern.compile("\\([^\\(]*\\)|\\s*;.*|%$");
    private static final Pattern GCODE_PATTERN = Pattern.compile("[Gg]0*(\\d+)");

    private static int decimalLength = -1;
    private static Pattern decimalPattern;
    private static DecimalFormat decimalFormatter;

    public static String removeWhiteSpace(String command){
        return WHITE_SPACE.matcher(command).replaceAll(EMPTY);
    }

    public static String removeComment(String command) {
        return COMMENT.matcher(command).replaceAll(EMPTY);
    }

    public static String truncateDecimals(int length, String command) {
        if (length != decimalLength) updateDecimalFormatter(length);
        Matcher matcher = decimalPattern.matcher(command);

        Double d;
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            d = Double.parseDouble(matcher.group());
            matcher.appendReplacement(sb, decimalFormatter.format(d));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void updateDecimalFormatter(int length) {
        StringBuilder df = new StringBuilder();
        df.append("#");

        if (length != 0) df.append(".");
        for (int i = 0; i < length; i++)df.append('#');

        decimalFormatter = new DecimalFormat(df.toString());

        df = new StringBuilder();
        df.append("\\d+\\.\\d");
        for (int i = 0; i < length; i++) df.append("\\d");
        df.append('+');
        decimalPattern = Pattern.compile(df.toString());
        decimalLength = length;
    }

    static private Pattern mPattern = Pattern.compile("[Mm]0*(\\d+)");
    static public List<Integer> parseMCodes(String command) {
        Matcher matcher = GCODE_PATTERN.matcher(command);
        List<Integer> codes = new ArrayList<>();

        while (matcher.find()) {
            codes.add(Integer.parseInt(matcher.group(1)));
        }

        return codes;
    }
}
