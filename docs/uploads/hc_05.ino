#include <SoftwareSerial.h>

SoftwareSerial BTSerial(10, 11); // RX | TX

void setup() {
  pinMode(9, OUTPUT);  // this pin will pull the HC-05 pin 34 (key pin) HIGH to switch module to AT mode
  digitalWrite(9, HIGH);
  Serial.begin(9600);

  Serial.print(&quot;For Arduino Rx use pin &quot;);
  Serial.println(10);
   
  Serial.print(&quot;For Arduino Tx use pin &quot;);
  Serial.println(11);  

  Serial.println(&quot; -- Command Reference ---&quot;);
  Serial.println(&quot;To Read use '?', Like AT+PSWD?&quot;);
  Serial.println(&quot;AT (simply checks connection)&quot;);
  Serial.println(&quot;AT+VERSION (requests the firmware verison)&quot;);
  Serial.println(&quot;AT+ROLE=x (0 =Slave role, 1 =  Master role, 2 = Slave-Loop role  default = 0)&quot;);
  Serial.println(&quot;AT+NAME=xxxxx (to change name to xxxxx default=HC-05&quot;);
  Serial.println(&quot;AT+PSWD=nnnn (to change password to 4 digit nnnn default = 1234&quot;);
  Serial.println(&quot;AT+UART=nnnn,s,p (nnnn=Baud, s=stop bits (0=1, 1=2), p=parity (0=None, 1=Odd, 2=Even)&quot;);
  Serial.println(&quot;AT+POLAR=a,b (a=PIO8 (LED), b=PIO9 for both 0=low turn on, 1 = high turn on.&quot;);  
  Serial.println(&quot;AT+ORGL (reset all parameters to defaults)&quot;);
  Serial.println(&quot;AT+RESET (restarts the HC-05. Will not be in AT mode afterward unless button held&quot;);

  Serial.println("Enter AT commands:");
  BTSerial.begin(38400);  // HC-05 default speed in AT command more

  

}

void loop() {
  
  // Keep reading from HC-05 and send to Arduino Serial Monitor
  if (BTSerial.available())
    Serial.write(BTSerial.read());

  // Keep reading from Arduino Serial Monitor and send to HC-05
  if (Serial.available())
    BTSerial.write(Serial.read());
}
