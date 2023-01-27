package in.co.gorest.grblcontroller.util;

import android.util.Pair;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import in.co.gorest.grblcontroller.model.Constants;

public  class SimpleGcodeMaker {
    private double xf;
    private double yf;
    private double xt;
    private double yt;
    private double xcenter;
    private double ycenter;
    private double xcirc;
    private double ycirc;

    private double zfrom;
    private double zstep;
    private double zdeep;
    private double ztraversal;
    private double feedrate;
    private double ray;
    private boolean Zaprox_pass;
    private double width;
    private double height;





    public SimpleGcodeMaker(
            double xf,
            double yf,
            double xt,
            double yt,



            double zfrom,
            double zstep,
            double zdeep,
            double ztraversal,
            double feedrate,
            boolean Zaprox_pass)  {


        this.xcenter=xf ;
        this.ycenter=yf ;
        this.xcirc=xt   ;
        this.ycirc=yt    ;
        this.zfrom       = zfrom;
        this.zstep       = zstep;
        this.zdeep       = zdeep;
        this.ztraversal  = ztraversal;
        this.feedrate    = feedrate;
        this.Zaprox_pass = Zaprox_pass;

        double X= Math.max(xf,xt);
        double Y=Math.max(yf,yt);
        double x= Math.min(xf,xt);
        double y=Math.min(yf,yt);

        this.xf=x;
        this.yf=Y;
        this.xt=X;
        this.yt=y;

        this.width= Math.abs(this.xt-this.xf);
        this.height=Math.abs(this.yt-this.yf);

        this.ray= BigDecimal.valueOf(Math.sqrt(Math.pow(this.xcirc - this.xcenter, 2)
                        + Math.pow(this.ycirc - this.ycenter, 2)))
                .round(new MathContext(4, RoundingMode.HALF_UP)).doubleValue();
    }


    public String cutCcw (double dist){
        String s="(cut ccw)\n";
        s += "G01X" + ( this.xf + dist ) + " Y" + ( this.yf - dist ) + "\n";
        s += "Y" + ( this.yt + dist ) + "\n";
        s += "X" + ( this.xt - dist ) + "\n";
        s += "Y" + ( this.yf - dist ) + "\n";
        s += "X" + ( this.xf + dist ) + "\n";

        return s;
    }
    public String cutCw (double dist){
        String s = "(cut cw)\n";
        s += "G01X" + (this.xf + dist) + " Y" + (this.yf - dist) + "\n";
        s += "X" + (this.xt - dist) + "\n";
        s += "Y" + (this.yt + dist) + "\n";
        s += "X" + (this.xf + dist) + "\n";
        s += "Y" + (this.yf - dist) + "\n";
        return s;
    }
    public String cutRectangle (double offset, boolean aprox_pass) {
        double min_edge = Math.min(this.height/2, this.width/2);
        int npass=1;
        if (offset >0) {
            Pair pass = this.pass(min_edge, offset, aprox_pass);//npass,step
            npass = (int) pass.first;
            offset = (double) pass.second;
        }
        double o;
        String s="(cut rectangle)\n";

        for (int i=0; i<npass; i++)
             s+=this.cutCw(offset * i);
        return  this.ZLoop(s,this.xf,this.yf);
    }




    public String  snakeX (double dist, boolean aprox_pass) {
        Pair pass = this.pass(this.height , dist, aprox_pass);//npass,step
        int npass = (int) pass.first;
        dist = (double) pass.second;
        boolean sw = true;
        double step = 0.0;


        String s="(snake x)\n";
        s += "G01X"+this.xf+" Y"+this.yf+"\n";

        for (int i = 1; i < npass + 1; i++) {
            step = Math.round(this.yf - (dist * i));
            if (sw) {
                s+="X"+this.xt+"\nY"+step+"\n";
                sw = false;
            } else {
                s+="X"+this.xf+"\nY"+step+"\n";
                sw = true;
            }
        }
        return this.ZLoop(s,this.xf,this.yf);
    }

    public String snakeY (double dist, boolean aprox_pass) {

        Pair pass = this.pass(this.width, dist, aprox_pass);//npass,step
        int npass = (int) pass.first;
        dist = (double) pass.second;
        boolean sw = true;
        double step = 0.0;

        String s=  "(snake y)\n";
        s+="G01X"+this.xf+" Y"+this.yf+"\n";
        for (int i = 1; i < npass + 1; i++) {
            step = Math.round(this.xf - (dist * -i));
            if (sw) {
                s+="Y"+this.yt+"\nX"+step+"\n";
                sw = false;
            } else {
                s+="Y"+this.yf+"\nX"+step+"\n";
                sw = true;
            }

        }
        return this.ZLoop(s,this.xf,this.yf);
    }
    public String CorneringCut (double tool_radius) {//tr tool radius
        System.out.println("toolradius"+tool_radius);
        String s="(cornering cut)\n";
        s += "G01X"+this.xf+" Y"+(this.yf+tool_radius)+"\n";//start
        s += ("G01X" + this.xt  + "\n");//1seg
        s += ("G02X" + (this.xt +tool_radius)+"Y" + this.yf  +"I0.000J-"+tool_radius+ "\n");//1arc
        s += ("G01Y" + this.yt  + "\n");//2seg
        s += ("G02X" + this.xt +"Y" + (this.yt-tool_radius) +"I-"+tool_radius+"J0.0000\n");//2arc
        s += ("G01X" + this.xf+ "\n");//3seg
        s += ("G02X" + (this.xf-tool_radius) +"Y" + this.yt  +"I0.000J"+tool_radius+ "\n");//3arc
        s += ("G01Y" + this.yf + "\n");//4seg
        s += ("G02X" + this.xf +"Y" + (this.yf+tool_radius) +"I"+tool_radius+"J0.0000\n");//4arc

        return this.ZLoop(s, this.xf,this.yf+tool_radius);
    }
    public String circleCut (double offset, boolean aprox_pass) {

        double quadR= this.xcenter+this.ray;//punto x a destra del centro O POINT?
        double quadL=this.xcenter-this.ray;//punto x a sinistra del centro
        String s="(circle cut)\n";
        int npass=1;
        if (offset >0) {
            Pair pass = this.pass(this.ray, offset, aprox_pass);//npass,step
            npass = (int) pass.first;
            offset = (double) pass.second;
        }
        double o;
        for (int i=0; i<npass; i++) {
            o=offset * i;
            s += "G01X"+(quadR - o)+" Y"+this.ycenter+"\n";
            s += "G02X"+(quadL + o)+"I-"+(this.ray - o)+"J0.0\n";
            s +="X"+(quadR - o)+"I"+(this.ray - o)+"J0.0\n";
        }

        return this.ZLoop(s,quadR,this.ycenter);
    }

    public String lineCut( ) {
        Pair pass = this.pass(Math.abs(this.zdeep), this.zstep, this.Zaprox_pass);//npass,step
        //int npass =
        boolean sw=true;
        String gcode="(line cut)\n";
        gcode+=Constants.CAM_GCODE_HEAD;;
        gcode+="G00 Z"+(this.ztraversal+this.zfrom)+ '\n';
        gcode+="X"+this.xcenter+" Y"+this.ycenter+"\n";
        for (int i=1;i<=(int)pass.first ;i++){
            gcode+="G01Z"+(this.zfrom-((double)pass.second*i))+" F"+this.feedrate+"\n";
            if(sw) {
                gcode += "X" + this.xcirc + " Y" + this.ycirc + "\n";
            }else {
                gcode += "X" + this.xcenter + " Y" + this.ycenter + "\n";
            }
            sw=!sw;
        }
        gcode+="G00 Z"+(this.ztraversal+this.zfrom)+"\n";
        gcode+=Constants.CAM_GCODE_END;;
        return gcode;
    }

    public String ZLoop(String cut_path,double startX,double startY ) {
        Pair pass = this.pass(Math.abs(this.zdeep), this.zstep, this.Zaprox_pass);//npass,step
        //int npass =
        String gcode="";
        gcode+= Constants.CAM_GCODE_HEAD;
        gcode+="G00 Z"+(this.zfrom+this.ztraversal)+ '\n';
        gcode+="X"+startX+" Y"+startY+"\n";
        for (int i=1;i<=(int)pass.first ;i++){
            gcode+="G01Z"+(this.zfrom-((double)pass.second*i))+" F"+this.feedrate+"\n";
            gcode+=cut_path;
        }
        gcode+="G00 Z"+(this.ztraversal+this.zfrom)+"\n";
        gcode+=Constants.CAM_GCODE_END;
        return gcode;
    }

    private Pair pass (double tot_len, double step , boolean aprox_pass ){

        int npass = (int) Math.round(tot_len / step);
        if(npass <=0) npass=1;
        if (aprox_pass) step =  tot_len / npass;


        System.out.println("totlen:"+tot_len+" step:"+step+" npass:"+npass);
        Pair ret = new Pair(npass,step);

        return ret;
    }

}


