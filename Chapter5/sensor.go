package service

import (
 "fmt"
 "log"
 "os/exec"
 "strings"

 "github.com/paypal/gatt"
)

func NewSensorService() *gatt.Service {
 s := gatt.NewService(gatt.MustParseUUID("19fc95c0-c111-11e3-9904-0002a5d5c51b"))
 s.AddCharacteristic(gatt.MustParseUUID("21fac9e0-c111-11e3-9246-0002a5d5c51b")).HandleReadFunc(
  func(rsp gatt.ResponseWriter, req *gatt.ReadRequest) {
   out, err := exec.Command("sh", "-c", "sudo /home/pi/temperature.py").Output()
    if err != nil {
     fmt.Fprintf(rsp, "error occured %s", err)
     log.Println("Wrote: error %s", err)
    } else {
     stringout := string(out)
     stringout = strings.TrimSpace(stringout)
     fmt.Fprintf(rsp, stringout)
     log.Println("Wrote:", stringout)
    }
 })

 s.AddCharacteristic(gatt.MustParseUUID("31fac9e0-c111-11e3-9246-0002a5d5c51b")).HandleReadFunc(
  func(rsp gatt.ResponseWriter, req *gatt.ReadRequest) {
   out, err := exec.Command("sh", "-c", "sudo /home/pi/humidity.py").Output()
    if err != nil {
     fmt.Fprintf(rsp, "error occured %s", err)
     log.Println("Wrote: error %s", err)
    } else {
     stringout := string(out)
     stringout = strings.TrimSpace(stringout)
     fmt.Fprintf(rsp, stringout)
     log.Println("Wrote:", stringout)
   }
 })

s.AddCharacteristic(gatt.MustParseUUID("41fac9e0-c111-11e3-9246-0002a5d5c51b")).HandleWriteFunc(
  func(r gatt.Request, data []byte) (status byte) {
   log.Println("Command received")
   exec.Command("sh", "-c", "sudo reboot").Output()
   return gatt.StatusSuccess
 })

s.AddCharacteristic(gatt.MustParseUUID("51fac9e0-c111-11e3-9246-0002a5d5c51b")).HandleWriteFunc(
  func(r gatt.Request, data []byte) (status byte) {
   log.Println("Command received to turn on")
   exec.Command("sh", "-c", "gpio -g mode 17 out").Output()
   exec.Command("sh", "-c", "gpio -g write 17 1").Output()
   return gatt.StatusSuccess
 })

 s.AddCharacteristic(gatt.MustParseUUID("61fac9e0-c111-11e3-9246-0002a5d5c51b")).HandleWriteFunc(
  func(r gatt.Request, data []byte) (status byte) {
   log.Println("Command received to turn off")
   exec.Command("sh", "-c", "gpio -g mode 17 out").Output()
   exec.Command("sh", "-c", "gpio -g write 17 0").Output()
   return gatt.StatusSuccess
 })

 s.AddCharacteristic(gatt.MustParseUUID("71fac9e0-c111-11e3-9246-0002a5d5c51b")).HandleWriteFunc(
  func(r gatt.Request, data []byte) (status byte) {
   log.Println("Command received to whistle ")
   exec.Command("sh", "-c", "aplay /home/pi/whistle_blow_01.wav").Output()
   return gatt.StatusSuccess
 })

 s.AddCharacteristic(gatt.MustParseUUID("81fac9e0-c111-11e3-9246-0002a5d5c51b")).HandleWriteFunc(
  func(r gatt.Request, data []byte) (status byte) {
   log.Println("Command received to turn on and whistle")
   exec.Command("sh", "-c", "aplay /home/pi/whistle_blow_01.wav").Output()
   exec.Command("sh", "-c", "gpio -g mode 17 out").Output()
   exec.Command("sh", "-c", "gpio -g write 17 1").Output()
   return gatt.StatusSuccess
 })


 return s
}
