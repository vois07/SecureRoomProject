#!/usr/bin/python3

import socket
import subprocess
import sys
import os.path
import threading
import telnetlib
import netifaces as ni
import pymysql
from time import localtime, strftime

MAX_CONNECTIONS_IN_QUEUE = 5
SERVER_PORT = 7133
NET_DEVICE_INTERFACE = 'enp0s8'

# ********* CONNECT TO MySQL ***********************
connDB = ''
curDB = ''
try:
    connDB = pymysql.connect(host='localhost', port=3306, user='engineer', passwd='en$5V6nR', db='psv_room')
    curDB = connDB.cursor()
except:
    print(' [ERROR] Cannot connect to MySQL!!!!')
    exit()
# ***************************************************

def recv_until(sock, marker, rcvlimit):
  data = ""
  while(data.find(marker) == -1):
    try:
      dnow = sock.recv(1)
      dnow = dnow.decode('utf-8')
      if(len(dnow) == 0):
        print("[ WARNING ] recv_until function failed at recv. Last recived data:", data)
        return False
    except socket.error as msg:
      print("[ WARNING ] recv_until function failed: ", msg, " Last recived data:", data)
      return False
    data += dnow
    if len(data) > rcvlimit:
      print("[ WARNING ] recv_until function failed: DATA_LIMIT_REACHED: ", rcvlimit," Last recived data:", data)
      return False
  return data

class SrvHandler(threading.Thread):
    def __init__(self, clientSock, clientAddr):
        super(SrvHandler, self).__init__()
        self.clientSock = clientSock
        self.clientAddr = clientAddr

    def run(self):
        recvData = recv_until(self.clientSock, "%&", 102400) #102400 == 100KB
        if(recvData != False):
            timeMeasure = strftime("%Y-%m-%d %H:%M:%S", localtime())
            temp1 = 0
            temp2 = 0
            temp3 = 0
            smoke = 0
            dataLen = len(recvData.split("%&")[0].split(" "))
            if dataLen == 1:
                temp1 = temp2 = temp3 = recvData.split("%&")[0].split(" ")[0]
            elif dataLen == 2:
                temp1 = recvData.split("%&")[0].split(" ")[0]
                temp2 = recvData.split("%&")[0].split(" ")[1]
                temp3 = temp2
            elif dataLen == 3:
                temp1, temp2, temp3 = recvData.split("%&")[0].split(" ")
            elif dataLen == 4:
                temp1, temp2, temp3, smoke = recvData.split("%&")[0].split(" ")
            print("[ ",  timeMeasure, temp1, temp2, temp3, smoke, " ]")
            sqlTask = "INSERT INTO `measurements` (`measur_date`, `temperature1`, `temperature2`, `temperature3`, `smoke_sensor`) VALUES (%s, %s, %s, %s, %s)"
            curDB.execute(sqlTask, (timeMeasure, temp1, temp2, temp3, smoke))
            connDB.commit()
        #Close connection in both sides: client and server, no data send
        self.clientSock.shutdown(socket.SHUT_RDWR)
        self.clientSock.close()


def main():
  print("[ INFO ] RpiServiceServer v0.2")
  print("[ INFO ] Server start at: ", strftime("%Y-%m-%d %H:%M:%S", localtime()))
  #Get server IP
  ip_server = ni.ifaddresses(NET_DEVICE_INTERFACE)[ni.AF_INET][0]['addr']
  print("[ INFO ] IP: ",ip_server," PORT: ",SERVER_PORT, " DEV: ", NET_DEVICE_INTERFACE)
  #TCP
  server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
  server.bind((ip_server, SERVER_PORT))
  #Max connection in queue
  server.listen(MAX_CONNECTIONS_IN_QUEUE)

  try:
    while True:
      clientSock, clientAddr = server.accept()
      #print("[ INFO ] Connection from: ", clientAddr)
      clientThread = SrvHandler(clientSock, clientAddr)
      clientThread.daemon = False #When kill main process, then it kill threads
      clientThread.start()
  except KeyboardInterrupt:
    print("[ Keyboard Interrupt ] Close server\n")
    server.close()
    curDB.close()
    connDB.close()

if __name__ == "__main__":
  main()
