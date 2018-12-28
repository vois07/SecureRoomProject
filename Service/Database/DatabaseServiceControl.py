#!/usr/bin/python3

import pymysql

# ********* CONNECT TO MySQL ***********************
connDB = ''
curDB = ''
try:
    connDB = pymysql.connect(host='localhost', port=3306, user='engineer', passwd='en$5V6nR', db='psv_room')
    curDB = connDB.cursor()
    print(' [INFO] CONNECTED TO MySQL')
except:
    print(' [ERROR] Cannot connect to MySQL!!!!')
    exit()
# ***************************************************
def print_options():
    print("************************")
    print("Choose table:")
    print("1. Users")
    print("2. Room")
    print("3. Users time work")

def print_db_fun():
    print_options()
    option = int(input("You choose: "))
    if(option == 1):
        option = input("SELECT:")
        sqlTask = "SELECT " + str(option) + " FROM `users`"
        curDB.execute(sqlTask)
        result = curDB.fetchone()
        print("Solution:")
        print(result)
        print(type(result) is tuple)
    elif(option == 2):
        option = input("SELECT:")
        sqlTask = "SELECT " + str(option) + " FROM `room`"
        curDB.execute(sqlTask)
        result = curDB.fetchone()
        print("Solution:")
        print(result)
        print(type(result) is str)
    elif(option == 3):
        option = input("SELECT:")
        sqlTask = "SELECT " + str(option) + " FROM `user_time`"
        curDB.execute(sqlTask)
        result = curDB.fetchone()
        print("Solution:")
        print(result)
    else:
        print("Unknow option!!!")

def modify_db_fun():
    print_options()
    option = int(input("You choose: "))
    if(option == 1):
        print("Only can change bit_confirm")
        person = str(input('Person: '))
        bitvalue = int(input('bit_confirm: '))
        sqlTask = "UPDATE `users` SET bit_confirm=%s WHERE name=%s"
        curDB.execute(sqlTask, (bitvalue, person))
        connDB.commit()
    # elif(option == 2):
    # elif(option == 3):
    else:
        print("Unknow option!!!")

def main():
    loopControl = True
    print('DatabaseServiceControl v0.1')
    while(loopControl):
        print("Options:")
        print("1. Print DB content.")
        print("2. Modify DB.")
        print("3. Exit")
        option = int(input("You choose: "))
        if(option == 1):
            print_db_fun()
        elif(option == 2):
            modify_db_fun()
        elif(option == 3):
            print("By by")
            loopControl = False
        else:
            print("Unknow option!!!")
        print("************************")
    curDB.close()
    connDB.close()

if __name__ == "__main__":
  main()
