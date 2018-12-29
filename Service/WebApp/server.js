//MODUŁY
var http = require('http');
var url = require('url');
var events = require('events');
var mysql = require('mysql');
var fs = require('fs');


//Dane bazy danych
var con = mysql.createConnection({
  host: "localhost",
  user: "engineer",
  password: "en$5V6nR",
  database: 'psv_room'
});


//DANE GŁÓWNE


//Podłączanie się do bazy danych + wysyłanie jednego zapytania
  con.connect(function(err) {
  if (err) throw err;
  console.log("Connected!");
  /*
  console.log("\n");
  con.query("select * from users", function (err, result) {
    if (err) throw err;
	console.log("----PRACOWNCY----");
	for(var i in result){		
		console.log("Pracownik: " + result[i]["name"]+" W biurze: "+result[i]["status_in_room"].toString()+" Pozwolenia: "+result[i]["bit_confirm"]);
	}
    console.log("\n");
  });
  con.query("select * from measurements", function (err, result) {
    if (err) throw err;
	console.log("----POMIARY----");
	for(var i in result){
		console.log("Data: " + result[i]["measur_date"]+" | Temperatura 1: "+result[i]["temperature1"].toString()+" | Temperatura 2: "+result[i]["temperature2"].toString()+" | Temperatura 3: "+result[i]["temperature3"].toString()+" Pożar: "+result[i]["smoke_sensor"].toString());
	}
	console.log("\n");
    
  });
  con.query("select p.name,p.id,k.user_id,k.start_time,k.end_time from users p join user_times k on p.id = k.user_id", function (err, result) {
    if (err) throw err;
	console.log("----PRACE PRACOWNIKÓW----");
	for(var i in result){
		console.log("Pracownik: " + result[i]["name"]+" Czas przybycia: "+result[i]["start_time"]+" Czas wyjścia: "+result[i]["end_time"]);
	}
    console.log("\n");
  });
  */
});
function newDate(x){
	var res = x.split(':');
	res.push(res[0].split(" "));
	var res2 = [];
	res2.push(res[3][2]);
	res2.push(res[3][0]);
	res2.push(res[3][1]);
	res2.push(res[3][3]);
	res2.push(res[1]);
	res2.push(res[2]);
	res2[0]=parseInt(res2[0]);
	res2[2]=parseInt(res2[2]);
	res2[3]=parseInt(res2[3]);
	res2[4]=parseInt(res2[4]);
	res2[5]=parseInt(res2[5]);
	switch(res2[1]){
		case "Jan":
		res2[1]=1;
		break;
		case "Feb":
		res2[1]=2;
		break;
		case "Mar":
		res2[1]=3;
		break;
		case "Apr":
		res2[1]=4;
		break;
		case "May":
		res2[1]=5;
		break;
		case "Jun":
		res2[1]=6;
		break;
		case "Jul":
		res2[1]=7;
		break;
		case "Aug":
		res2[1]=8;
		break;
		case "Sep":
		res2[1]=9;
		break;
		case "Oct":
		res2[1]=10;
		break;
		case "Nov":
		res2[1]=11;
		break;
		case "Dec":
		res2[1]=12;
		break;
	}
	var date = new Date(res2[0],res2[1],res2[2],res2[3]+1,res2[4],res2[5])
	return date;
}

var eventEmitter = new events.EventEmitter();
//Główna pętla serwera
http.createServer(function (req, response) {
var labels;
var labelsResult;
var temperature1;
var temperature2;
var temperature3;
var people;
var data_time;
	//Wgrywanie strony głównej
	
con.query("select * from measurements order by measur_date ASC", function (err, result) {
    if (err) throw err;
	labels = '';
	labelsResult = [];
	temperature1 = [];
	temperature2 = [];
	temperature3 = [];
	people = 0;
	data_time = new Date();
	var data_index=0;
	var data_value = 1000000000000;
	for(var i in result){
		labelsResult.push(result[i]["measur_date"].toString().slice(4,-21));
		temperature1.push(result[i]["temperature1"]);
		temperature2.push(result[i]["temperature2"]);
		temperature3.push(result[i]["temperature3"]);
		 if(Math.abs(data_time-newDate(result[i]["measur_date"].toString().slice(4,-21)))<data_value){
			 data_value = Math.abs(data_time-newDate(result[i]["measur_date"].toString().slice(4,-21)));
			 data_index = i;
		 };

	}
	console.log(result[i]["measur_date"]);
	var temper = (result[data_index]["temperature1"]+result[data_index]["temperature2"]+result[data_index]["temperature3"])/3;
	     for(i in labelsResult){
	  labels=labels+'\''+labelsResult[i]+'\',';
  }
  con.query("select * from users", function (err, result2) {
	  if (err) throw err;
	  for(var j in result2){
		  if(result2[j]["status_in_room"]==1){
			  people++;
		  }
	  }
	  fs.readFile('index.html', 'utf-8', function (err, data) {
        response.writeHead(200, { 'Content-Type': 'text/html' });
		data=data.replace('XLABELSX', labels);
		var datasets = '';
		var dataset = "{borderColor: 'rgba(255, 0, 0, 0.5)',backgroundColor: 'rgba(0, 0, 0, 0)', label: 'Temperature1',data: [XDATAX]}";
		datasets = datasets+dataset.replace("XDATAX",temperature1.toString())+',';
		dataset = "{borderColor: 'rgba(0, 255, 0, 0.5)',backgroundColor: 'rgba(0, 0, 0, 0)',label: 'Temperature2',data: [XDATAX]}";
		datasets = datasets+dataset.replace("XDATAX",temperature2.toString())+',';
		dataset = "{borderColor: 'rgba(0, 0, 255, 0.5)',backgroundColor: 'rgba(0, 0, 0, 0)',label: 'Temperature3',data: [XDATAX]}";
		datasets = datasets+dataset.replace("XDATAX",temperature3.toString())+',';
		data = data.replace('XDATASETSX', datasets);
		data = data.replace('XPEOPLEX',people.toString());
		data = data.replace('XTEMPERATUREX',temper.toString());
		data = data.replace('XFIREX',result[data_index]["smoke_sensor"].toString());
        response.write(data);
        response.end();
    });
  });
  
  
  });
 
	

    

  /* PLIKI - Do późniejszego użycia
	fs.appendFile('test.txt', 'tt', function (err) {
  if (err) throw err;
  console.log('Saved!');
});

	fs.open('test2.txt', 'w', function (err, file) {
  if (err) throw err;
  console.log('Saved!');
});

	fs.writeFile('test3.txt', 'tt', function (err) {
  if (err) throw err;
  console.log('Saved!');
});

//Usuwanie plików
fs.unlink('test2.txt', function (err) {
  if (err) throw err;
  console.log('File deleted!');
});


fs.rename('test.txt', 'test4.txt', function (err) {
  if (err) throw err;
  console.log('File Renamed!');
});
*/

/* URL - Do późniejszego użycia
var adr = 'http://localhost:8080/default.htm?year=2017&month=february';
var q = url.parse(adr, true);

console.log(q.host); //returns 'localhost:8080'
console.log(q.pathname); //returns '/default.htm'
console.log(q.search); //returns '?year=2017&month=february'

var qdata = q.query; //returns an object: { year: 2017, month: 'february' }
console.log(qdata.month); //returns 'february'

*/

/*EVENTY - Do późniejszego użycia
var myEventHandler = function () {
  console.log('ttt');
}
eventEmitter.on('scream', myEventHandler);

eventEmitter.emit('scream');
*/





}).listen(8080);
