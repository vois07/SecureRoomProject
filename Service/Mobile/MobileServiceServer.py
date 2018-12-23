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

MAX_CONNECTIONS_IN_QUEUE = 20
SERVER_PORT = 7131
NET_DEVICE_INTERFACE = 'enp0s3' # 'enp0s8'

clientRSA_PublicKeys = {}
if os.path.exists("publ_client_keys.dd") == True:
  with open('publ_client_keys.dd') as f:
    for line in f:
      cliNam = line.split()[0]
      cliPubKey = line.split()[1]
      clientRSA_PublicKeys[cliNam] = RSA.importKey(cliPubKey).publickey()

myKey = ''
if os.path.exists("priv_key.pem") == False:
  print("[ INFO ] Server not found RSA keys. Generate new one.")
  random_generator = Random.new().read
  # Public and Private key
  myKey = RSA.generate(1024, random_generator)
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
#print("[ DEBUG ] Server public key:")
#print(str_pubKey)


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
  def __init__(self, clientSock, clientAddr, clientHostname):
    super(Handler, self).__init__()
    self.clientSock = clientSock
    self.clientAddr = clientAddr
    self.clientHostNm = clientHostname

  def run(self):
    recvData = recv_until(self.clientSock, "\r\n\r\n", 102400) #102400 == 100KB
    if(recvData != False):
      machin, usrid, scode, rdata = recvData.split("$$$")
      test_code_value = False
      if(machin == "A"):
        try:
          code = int(scode)
        except:
          print("[ WARNING ] Cannot get msg code!")
          test_code_value = True
        if(test_code_value == False):
         # Client download public key from server
          if(code == 101):
            messg = "101$$$" + str_pubKey
            self.clientSock.sendall(messg.encode("utf-8"))
            print("[ INFO ] Sent public key to ", usrid)
        # Send android’s user server's public key
          elif(code == 102):
            try:
              rrdata = rdata.replace('\r\n\r\n', '')
              f = open('publ_client_keys.dd', 'a')
              rsa_android = base64.b64decode(rrdata)
              publicKeyAndroid = RSA.importKey(rsa_android).publickey()
              str_publicKeyAndroid = publicKeyAndroid.exportKey('PEM').decode('utf-8')
              str_publicKeyAndroid = str_publicKeyAndroid.replace('-----BEGIN PUBLIC KEY-----', '')
              str_publicKeyAndroid = str_publicKeyAndroid.replace('-----END PUBLIC KEY-----', '')
              str_publicKeyAndroid = str_publicKeyAndroid.replace('\n', '')
              print("[ INFO ] ", usrid, " public key: ", str_publicKeyAndroid)
              f.write("%s %s\n" % (usrid, str_publicKeyAndroid))
              f.close()
              clientRSA_PublicKeys[usrid] = publicKeyAndroid
            except:
              print("[ WARNING ] Exception on code 102")
          # Ask to enter to the room
          #elif(code == 103):
          # Ask to exit the room
          #elif(code == 104):
          # Send data to Android's user
          #elif(code == 105):
          #
          elif(code == 201):
            print("[ INFO ] TEST CONNECTION WITH ", usrid, " RECIVE: ", rdata.replace('\r\n\r\n', ''))
            messg = "SERV_ANSWER".encode("utf-8")
            self.clientSock.sendall(messg)
          elif(code == 202):
            try:
              rrdata = rdata.replace('\r\n\r\n', '')
              dec_data = myKey.decrypt(base64.b64decode(rrdata))
              print("[ INFO ] Recived data: ", dec_data.decode('utf-8'), " from: ", usrid)
              messg = "SERVER_HELLO_WORLD".encode('utf-8')
              enc_messg = clientRSA_PublicKeys[usrid].encrypt(messg, 32)
              sendData = "DATA:" + str(base64.b64encode(enc_messg[0]))
              self.clientSock.sendall(sendData.encode("utf-8"))
            except:
              print("[ WARNING ] Exception on code 202")
          else:
            print("[ WARNING ] Android unknow code: ", code)
      elif(machin == "Rrrrr"):
        try:
          code = int(scode)
        except:
          print("[ WARNING ] Cannot get msg code!")
          test_code_value = True
      else:
        print("[ ALERT ] Unknown machine!")
    #Close connection in both sides: client and server, no data send
    self.clientSock.shutdown(socket.SHUT_RDWR)
    self.clientSock.close()

def main():
  print("[ INFO ] APS v0.4")
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
      clientHostname = socket.gethostbyaddr(clientAddr[0])[0]
      print("[ INFO ] New connection from: %s:%i", clientAddr, " { ", clientHostname, " }");
      clientThread = SrvHandler(clientSock, clientAddr, clientHostname)
      clientThread.daemon = False #When kill main process, then it kill threads
      clientThread.start()
  except KeyboardInterrupt:
    print("[ Keyboard Interrupt ] Close server\n")
    server.close()

if __name__ == "__main__":
  main()
