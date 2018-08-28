#include <SoftwareSerial.h>
SoftwareSerial hcSerial(10, 11); // RX, TX
 
String fromPC = "";
 
void setup() { 
  Serial.begin(9600); // hardware serial for the USB-PC
  hcSerial.begin(9600);  // software serial Arduino to HC-06 (9600 is default)
 
  // print instructions
  Serial.println("HC-06 AT Command Programming");
  Serial.println(" -- Command Reference ---");
  Serial.println("AT (simply checks connection)");
  Serial.println("AT+VERSION (sends the firmware verison)");
  Serial.println("AT+NAMExxxxx (to change name to xxxxx");
  Serial.println("AT+PINnnnn (to change password to 4 digit nnnn");
  Serial.println("AT+BAUDn (to change to baud rate #1");
  Serial.println("  BAUD1 = 1200");
  Serial.println("  BAUD2 = 2400");
  Serial.println("  BAUD3 = 4800");
  Serial.println("  BAUD4 = 9600");
  Serial.println("  BAUD5 = 19200");
  Serial.println("  BAUD6 = 38400");
  Serial.println("  BAUD7 = 57600");
  Serial.println("  BAUD8 = 115200");
}
 
void loop() {
  // Read from HC-06
  if (hcSerial.available()) {
    while(hcSerial.available()) { // While there is more to be read, keep reading.
      Serial.print((char)hcSerial.read());
      }   
  }
   
  // Read from PC
  if (Serial.available()){
    delay(10); //     
    fromPC = (char)Serial.read();    
  
    if (fromPC == "r" || fromPC == "n") {  // don't send carriage returns to HC-06          
      Serial.println();  // echo it back to the PC
    }
    else {
      hcSerial.print(fromPC); // show the HC-06 responce
      Serial.print(fromPC); // echo it back to the PC
    }
  }
}