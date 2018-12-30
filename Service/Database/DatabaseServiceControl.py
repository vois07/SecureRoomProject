#!/usr/bin/python3

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
# ***************************************************

def print_db_fun():
    sqlTask = "SELECT * FROM `users`"
    curDB.execute(sqlTask)
    connDB.commit()
    result = curDB.fetchone()
    while(type(result) is tuple):
        idperson = result[0]
        name = result[1]
        statusInRoom = result[2]
        bitvalue = result[3]
        print(idperson,". Name: ", name," Status in room: ", statusInRoom, ' Access: ', end='')
        if(bitvalue == None):
            print("Not set")
        else:
            print("Allowed")
        result = curDB.fetchone()
    if(type(result) is not tuple):
        print(' ^No users in database')

def modify_db_fun():
    person = str(input('Person: '))
    bitvalue = str(input('bit access: '))
    if (bitvalue.find("None") != -1):
        bitvalue = None
    else:
        bitvalue = int(bitvalue)
    sqlTask = "UPDATE `users` SET bit_confirm=%s WHERE name=%s"
    curDB.execute(sqlTask, (bitvalue, person))
    connDB.commit()

def remove_db_fun():
    person = str(input('Person to remove: '))
    sqlTask = "DELETE FROM `users` WHERE name=%s"
    curDB.execute(sqlTask, (person))
    connDB.commit()

def main():
    loopControl = True
    print('DatabaseServiceControl v0.1')
    while(loopControl):
        print("Options:")
        print("1. Print all users.")
        print("2. Modify user access rights.")
        print("3. Remove user.")
        print("4. Exit")
        option = int(input("You choose: "))
        if(option == 1):
            print_db_fun()
        elif(option == 2):
            modify_db_fun()
        elif(option == 3):
            remove_db_fun()
        elif(option == 4):
            print("By by")
            loopControl = False
        else:
            print("Unknow option!!!")
        print("************************")
    curDB.close()
    connDB.close()

if __name__ == "__main__":
  main()
