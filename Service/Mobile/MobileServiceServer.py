#!/usr/bin/python3

import socket
import subprocess
import sys
import os.path
import threading
import telnetlib
import netifaces as ni
import base64
from Crypto.PublicKey import RSA
from Crypto import Random
import logging
import time
from time import gmtime, strftime
import pymysql

# ********* CONNECT TO MySQL ***********************
connDB = ''
curDB = ''
try:
    connDB = pymysql.connect(host='localhost', port=3306, user='engineer', passwd='en$5V6nR', db='psv_room')
    curDB = connDB.cursor()
except:
    print(' [ERROR] Cannot connect to MySQL!!!!')
    exit()

# ***************** SETUP ***************************
logFileName = time.asctime(time.localtime(time.time())).replace(':','').replace(' ', '_') + '.log'
logging.basicConfig(filename=logFileName, level=logging.DEBUG)

MAX_CONNECTIONS_IN_QUEUE = 20
SERVER_PORT = 7131
NET_DEVICE_INTERFACE = 'enp0s3' # 'enp0s8'

clientRSA_PublicKeys = {}
if os.path.exists("publ_client_keys.dd") == True:
  cliNam = ''
  cliPubKey = ''
  with open('publ_client_keys.dd') as f:
    for line in f:
        if(str(line).find("User:") != -1):
            cliNam = str(line.split()[1])
        elif(str(line).find("END PUBLIC") != -1):
            cliPubKey += str(line)
            clientRSA_PublicKeys[cliNam] = RSA.importKey(cliPubKey).publickey()
        else:
            cliPubKey += line
myKey = ''
if os.path.exists("priv_key.pem") == False:
  print("[ INFO ] Server not found RSA keys. Generate new one.")
  logging.info('Server not found RSA keys. Generate new one.')
  random_generator = Random.new().read
  myKey = RSA.generate(4096, random_generator)
  f = open('priv_key.pem', 'wb')
  f.write(myKey.exportKey('PEM'))
  f.close()
else:
  f = open('priv_key.pem', 'r')
  myKey = RSA.importKey(f.read())
  f.close()

publicKey = myKey.publickey()
str_pubKey = publicKey.exportKey('PEM').decode('utf-8')
str_pubKey = str_pubKey.replace('-----BEGIN PUBLIC KEY-----', '')
str_pubKey = str_pubKey.replace('-----END PUBLIC KEY-----', '')
str_pubKey = str_pubKey.replace('\n', '')
# ******************************************************************

def recv_until(sock, marker, rcvlimit):
  data = ""
  while(data.find(marker) == -1):
    try:
      dnow = sock.recv(1)
      dnow = dnow.decode('utf-8')
      if(len(dnow) == 0):
        print("[ WARNING ] recv_until function failed at recv. Last recived data:", data)
        logging.warning('recv_until function failed at recv. Last recived data:' + str(data))
        return False
    except socket.error as msg:
      print("[ WARNING ] recv_until function failed: ", msg, " Last recived data:", data)
      logging.warning('recv_until function failed:' + str(msg) + ' Last recived data:' + str(data))
      return False
    data += dnow
    if len(data) > rcvlimit:
      print("[ WARNING ] recv_until function failed: DATA_LIMIT_REACHED: ", rcvlimit," Last recived data:", data)
      logging.warning('recv_until function failed: DATA_LIMIT_REACHED: ' + str(rcvlimit) + ' Last recived data:' + str(data))
      return False
  return data

class SrvHandler(threading.Thread):
    def __init__(self, clientSock, clientAddr):
        super(SrvHandler, self).__init__()
        self.clientSock = clientSock
        self.clientAddr = clientAddr

    def run(self):
        recvData = recv_until(self.clientSock, "\r\n\r\n", 102400) #102400 == 100KB
        if(recvData != False):
            usrid, scode, rdata = recvData.split("$$$")
            if(not(bool(usrid.strip()))):
                print("[ ERROR ] Empty User")
                logging.error('Empty User')
                self.clientSock.shutdown(socket.SHUT_RDWR)
                self.clientSock.close()
                return
            test_code_value = False
            try:
              code = int(scode)
            except:
              print("[ WARNING ] Cannot get msg code!")
              logging.warning('Cannot get msg code!')
              test_code_value = True
            if(test_code_value == False):
             # Client download public key from server
              if(code == 101):
                messg = str(usrid) + "$$$101$$$DATA$$$" + str_pubKey
                self.clientSock.sendall(messg.encode("utf-8"))
                print("[ INFO ] Sent public key to ", usrid)
                logging.info('Sent public key to ' + str(usrid))
            # Send android’s user server's public key
              elif(code == 102):
                try:
                  rrdata = rdata.replace('\r\n\r\n', '')
                  rsa_android = base64.b64decode(rrdata)
                  publicKeyAndroid = RSA.importKey(rsa_android).publickey()
                  print("[ INFO ] Recive public RSA from: ", usrid)
                  valFlag = False
                  if os.path.exists("publ_client_keys.dd") == True:
                     usrTest = str(usrid)
                     with open('publ_client_keys.dd') as f:
                       for line in f:
                           if(str(line).find(usrTest) != -1):
                              print("[ INFO ] Server has RSA key from: ", usrid)
                              valFlag = True
                  if(valFlag == False):
                      f = open('publ_client_keys.dd', 'a')
                      str_publicKeyAndroid = publicKeyAndroid.exportKey('PEM').decode('utf-8')
                      f.write("User: %s\n%s\n" % (usrid, str_publicKeyAndroid))
                      f.close()
                      clientRSA_PublicKeys[usrid] = publicKeyAndroid
                      try:
                          sqlTask = "INSERT INTO `users` (`name`) VALUES (%s)"
                          curDB.execute(sqlTask, (usrid))
                          connDB.commit()
                          print("[ INFO ] Add user: ", usrid)
                          logging.info('Add user: ' + str(usrid))
                      except:
                          print("[ INFO ] Problem with add user: ", usrid)
                  else:
                      print("[ INFO ] User: ", usrid, " is in db.")
                      logging.info('User: ' + str(usrid) + " is in db.")
                  messg = str(usrid) + "$$$102$$$DATA$$$OK"
                  self.clientSock.sendall(messg.encode("utf-8"))
                except:
                  print("[ WARNING ] Exception on code 102")
                  logging.warning('Exception on code 102')
                  messg = str(usrid) + "$$$102$$$DATA$$$NO"
                  self.clientSock.sendall(messg.encode("utf-8"))
              # Ask to enter to the room
              elif(code == 103):
                  print("[ INFO ] User: ", usrid, ' ask to enter room')
                  logging.info('User: ' + str(usrid) + ' ask to enter room')
                  sqlTask = "SELECT * FROM `users` WHERE `name`=%s"
                  curDB.execute(sqlTask, (usrid))
                  connDB.commit()
                  result = curDB.fetchone()
                  if(type(result) is tuple):
                      userIDdb = result[0]
                      bitvalue = result[3]
                      if(bitvalue == None):
                          print('[ WARNING ] CODE 103 Problem with User: ', usrid, ' no bit_confirm set.')
                          logging.warning("CODE 103 Problem with User: " + str(usrid) + " no bit_confirm set.")
                          messg = "NO_ACCESS".encode('utf-8')
                          enc_messg = clientRSA_PublicKeys[usrid].encrypt(messg, 32)
                          sendData =  str(usrid) + "$$$103$$$DATA$$$" + str(base64.b64encode(enc_messg[0]))
                          self.clientSock.sendall(sendData.encode("utf-8"))
                      else:
                          print('[ INFO ] User: ', usrid, ' enter to room.')
                          logging.info("User: " + str(usrid) + " enter to room")
                          flagDoor = False
                          # HOSTRPI3 = '192.168.88.248'
                          # PORTRPI3 = 7755
                          # try:
                          #     with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                          #         s.connect((HOSTRPI3, PORTRPI3))
                          #         s.sendall("OPEN$".encode('utf-8'))
                          # except:
                          #     print("[ ERROR ] Cannot open door")
                          #     logging.error("Cannot open door")
                          #     flagDoor = True
                          messg = ''
                          if (flagDoor == False):
                              messg = "ENTER"
                              print('[ INFO ] Update ENTER User: ', usrid, ' status in room.')
                              logging.info("Update ENTER User: " + str(usrid) + " status in room.")
                              sqlTask = "UPDATE `users` SET status_in_room=%s WHERE name=%s"
                              curDB.execute(sqlTask, (1, result[1]))
                              connDB.commit()
                              print('[ INFO ] Update ENTER User: ', usrid, ' time in room.')
                              logging.info("Update ENTER User: " + str(usrid) + " time in room.")
                              sqlTask = "INSERT INTO `user_times` (`user_id`, `start_time`) VALUES(%s, %s)"
                              curDB.execute(sqlTask, (userIDdb, strftime("%Y-%m-%d %H:%M:%S", gmtime())))
                              connDB.commit()
                          else:
                              messg = "FAILED"
                          messg = messg.encode('utf-8')
                          enc_messg = clientRSA_PublicKeys[usrid].encrypt(messg, 32)
                          sendData =  str(usrid) + "$$$103$$$DATA$$$" + str(base64.b64encode(enc_messg[0]))
                          self.clientSock.sendall(sendData.encode("utf-8"))
                  else:
                      print('[ WARNING ] CODE 103 Problem with User: ', usrid)
                      logging.warning("CODE 103 Problem with User: " + str(usrid))
                      messg = "NO_USER".encode('utf-8')
                      sendData =  str(usrid) + "$$$103$$$DATA$$$" + str(base64.b64encode(messg))
                      self.clientSock.sendall(sendData.encode("utf-8"))
              # Ask to exit the room
              elif(code == 104):
                  print("[ INFO ] User: ", usrid, ' leave room')
                  logging.info('User: ' + str(usrid) + ' leave room')
                  sqlTask = "SELECT * FROM `users` WHERE `name`=%s"
                  curDB.execute(sqlTask, (usrid))
                  connDB.commit()
                  result = curDB.fetchone()
                  if(type(result) is tuple):
                      userIDdb = result[0]
                      bitvalue = result[3]
                      if(bitvalue == None):
                          print('[ WARNING ] CODE 104 Problem with User: ', usrid, ' no bit_confirm set.')
                          logging.warning("CODE 104 Problem with User: " + str(usrid) + " no bit_confirm set.")
                          messg = "NO_ACCESS".encode('utf-8')
                          enc_messg = clientRSA_PublicKeys[usrid].encrypt(messg, 32)
                          sendData =  str(usrid) + "$$$104$$$DATA$$$" + str(base64.b64encode(enc_messg[0]))
                          self.clientSock.sendall(sendData.encode("utf-8"))
                      else:
                           print('[ INFO ] User: ', usrid, ' EXIT room.')
                           logging.info("User: " + str(usrid) + " EXIT room")
                           flagDoor = False
                           # HOSTRPI3 = '192.168.88.248'
                           # PORTRPI3 = 7755
                           # try:
                           #     with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                           #         s.connect((HOSTRPI3, PORTRPI3))
                           #         s.sendall("OPEN$".encode('utf-8'))
                           # except:
                           #     print("[ ERROR ] Cannot open door")
                           #     logging.error("Cannot open door")
                           #     flagDoor = True
                           if (flagDoor == False):
                                messg = "EXIT"
                                print('[ INFO ] Update EXIT User: ', usrid, ' status in room.')
                                logging.info("Update EXIT User: " + str(usrid) + " status in room.")
                                sqlTask = "UPDATE `users` SET status_in_room=%s WHERE name=%s"
                                curDB.execute(sqlTask, (0, result[1]))
                                connDB.commit()

                                print('[ INFO ] Update EXIT User: ', usrid, ' time in room.')
                                logging.info("Update EXIT User: " + str(usrid) + " time in room.")
                                sqlTask = "UPDATE `user_times` SET `end_time`=%s WHERE user_id=%s AND end_time=%s"
                                curDB.execute(sqlTask, (strftime("%Y-%m-%d %H:%M:%S", gmtime()), userIDdb, None))
                                connDB.commit()

                           else:
                               messg = "FAILED"
                           messg = messg.encode('utf-8')
                           enc_messg = clientRSA_PublicKeys[usrid].encrypt(messg, 32)
                           sendData =  str(usrid) + "$$$104$$$DATA$$$" + str(base64.b64encode(enc_messg[0]))
                           self.clientSock.sendall(sendData.encode("utf-8"))
                  else:
                       print('[ WARNING ] CODE 104 Problem with no User: ', usrid)
                       logging.warning("CODE 104 Problem with no User: " + str(usrid))
                       messg = (str(usrid) + "$$$104$$$DATA$$$NO_USER").encode('utf-8')
                       self.clientSock.sendall(messg)
              # Send data to Android's user
              elif(code == 105):
                  print("[ INFO ] User: ", usrid, ' ask for data from DB')
                  logging.info('User: ' + str(usrid) + ' ask for data from DB')
                  sqlTask = "SELECT * FROM `users` WHERE `name`=%s"
                  curDB.execute(sqlTask, (usrid))
                  connDB.commit()
                  result = curDB.fetchone()
                  if(type(result) is tuple):
                       if(result[3] == None):
                           print('[ INFO ] Send NO_ACCESS to User: ', usrid)
                           logging.info("Send NO_ACCESS to User: " + str(usrid))
                           messg = "NO_ACCESS".encode('utf-8')
                           enc_messg = clientRSA_PublicKeys[usrid].encrypt(messg, 32)
                           sendData =  str(usrid) + "$$$105$$$DATA$$$" + str(base64.b64encode(enc_messg[0]))
                           self.clientSock.sendall(sendData.encode("utf-8"))
                       else:
                            print('[ INFO ] Send data from db to User: ', usrid)
                            logging.info("Send data from db to User: " + str(usrid))
                            sqlTask = "SELECT * FROM `measurements` ORDER BY `measur_date` DESC LIMIT 1"
                            curDB.execute(sqlTask)
                            connDB.commit()
                            result = curDB.fetchone()
                            measure_time = result[1]
                            temp1 = result[2]
                            temp2 = result[3]
                            temp3 = result[4]
                            smoke = result[5]
                            sqlData = str(measure_time).split(" ")[0] + "$$$" + str(measure_time).split(" ")[1] + "$$$Temperatura1$$$" + str(temp1) + "$$$Temperatura2$$$" + str(temp2) + "$$$Temperatura3$$$" + str(temp3) + "$$$Air$$$" + str(smoke)
                            messg = sqlData.encode('utf-8')
                            enc_messg = clientRSA_PublicKeys[usrid].encrypt(messg, 32)
                            sendData = str(usrid) + "$$$105$$$DATA$$$" + str(base64.b64encode(enc_messg[0]))
                            self.clientSock.sendall(sendData.encode("utf-8"))
                  else:
                      print('[ WARNING ] CODE 105 Problem with no User: ', usrid)
                      logging.warning("CODE 105 Problem with no User: " + str(usrid))
                      messg = "$$$105$$$DATA$$$NO_USER".encode('utf-8')
                      self.clientSock.sendall(messg)
              elif(code == 201):
                print("[ INFO ] TEST CONNECTION WITH ", usrid, " RECIVE: ", rdata.replace('\r\n\r\n', ''))
                messg =  str(usrid) + "$$$202$$$DATA$$$" + "SERV_OK"
                messg = messg.encode("utf-8")
                self.clientSock.sendall(messg)
              elif(code == 202):
                try:
                  print("[ INFO ] TEST ENCRYPTION WITH ", usrid)
                  rrdata = rdata.replace('\r\n\r\n', '')
                  dec_data = myKey.decrypt(base64.b64decode(rrdata))
                  print("[ INFO ] Recived data: ", dec_data.decode('utf-8'), " from: ", usrid)
                  logging.info("Code 202 Recived data: " + str(dec_data.decode('utf-8')) + " from: " + str(usrid))
                  messg = "SERV_OK".encode('utf-8')
                  enc_messg = clientRSA_PublicKeys[usrid].encrypt(messg, 32)
                  sendData = str(usrid) + "$$$202$$$DATA$$$" + str(base64.b64encode(enc_messg[0]))
                  self.clientSock.sendall(sendData.encode("utf-8"))
                except:
                  print("[ WARNING ] Exception on code 202")
                  logging.warning('Exception on code 202')
              else:
                print("[ WARNING ] Unknow code: ", code)
                logging.warning('Unknow code: ' + str(code))
        #Close connection in both sides: client and server, no data send
        self.clientSock.shutdown(socket.SHUT_RDWR)
        self.clientSock.close()

def main():
  print("[ INFO ] MobileServiceServer v0.4")
  print("[ INFO ] Server start at: ", strftime("%Y-%m-%d %H:%M:%S", gmtime()))
  logging.info("Server start at: " + str(strftime("%Y-%m-%d %H:%M:%S", gmtime())))
  ip_server = ni.ifaddresses(NET_DEVICE_INTERFACE)[ni.AF_INET][0]['addr']
  print("[ INFO ] IP: ",ip_server," PORT: ",SERVER_PORT, " DEV: ", NET_DEVICE_INTERFACE)
  server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
  server.bind((ip_server, SERVER_PORT))
  server.listen(MAX_CONNECTIONS_IN_QUEUE)
  try:
    while True:
      clientSock, clientAddr = server.accept()
      print("[ INFO ] Connection from: ", clientAddr);
      logging.info('Connection from: ' + str(clientAddr))
      clientThread = SrvHandler(clientSock, clientAddr)
      clientThread.daemon = False
      clientThread.start()
  except KeyboardInterrupt:
    print("[ Keyboard Interrupt ] Close server\n")
    logging.info('[ Keyboard Interrupt ] Close server')
    server.close()
    curDB.close()
    connDB.close()

if __name__ == "__main__":
  main()
