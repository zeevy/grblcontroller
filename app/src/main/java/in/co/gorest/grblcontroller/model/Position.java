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

package in.co.gorest.grblcontroller.model;


import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class Position {
    private final Double cordX;
    private final Double cordY;
    private final Double cordZ;


    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
    private static final DecimalFormat decimalFormat = (DecimalFormat) numberFormat;

    public Position(double x, double y, double z){
        this.cordX = x; this.cordY = y; this.cordZ = z;
        decimalFormat.applyPattern("#0.###");
    }

    public Double getCordX(){ return this.cordX; }
    public Double getCordY(){ return this.cordY; }
    public Double getCordZ(){ return this.cordZ; }

    private Double roundDouble(Double value){
        String s = decimalFormat.format(value);
        return Double.parseDouble(s);
    }

    public boolean hasChanged(Position position){
        return (position.getCordX().compareTo(this.cordX) != 0) || (position.getCordY().compareTo(this.cordY) != 0) || (position.getCordZ().compareTo(this.cordZ) != 0);
    }

}
