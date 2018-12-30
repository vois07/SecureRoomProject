//MODUŁY
var http = require('http');
var url = require('url');
var events = require('events');
var mysql = require('mysql');
var fs = require('fs');
var express = require('express');
var app = express();
app.use(express.static('public'));


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

function simDate(date){
	
var dd = date.getDate();
var mm = date.getMonth()+1; //January is 0!
var yyyy = date.getFullYear();
var today;

if(dd<10) {
    dd = '0'+dd
} 

if(mm<10) {
    mm = '0'+mm
} 

today = yyyy + '-' + mm + '-' + dd;
return today
}
var eventEmitter = new events.EventEmitter();
//Główna pętla serwera

var server = app.listen(8081, function () {
   var host = server.address().address
   var port = server.address().port
   
   console.log("Example app listening at http://%s:%s", host, port)
})

app.get('/generate', function (req, res) {
    console.log('works');
});

app.get('/main', function (req, res) {
var q = url.parse(req.url, true);
var dateFrom = simDate(new Date());
var dateTo = simDate(new Date());
var temp1 = "on";
var temp2 = "on";
var temp3 = "on";
if(q.search != null && q.search != '' && q.search != undefined){
	if(q.query["temperature1"] == undefined){
		temp1 = "off";
	}
	if(q.query["temperature2"] == undefined){
		temp2 = "off";
	}
	if(q.query["temperature3"] == undefined){
		temp3 = "off";
	}
	if(q.query["dateFrom"] != undefined){
		dateFrom = q.query["dateFrom"];
	}
	if(q.query["dateTo"] != undefined){
		dateTo = q.query["dateTo"];
	}
}
console.log(dateFrom);
console.log(dateTo);
var labels;
var labelsResult;
var temperature1;
var temperature2;
var temperature3;
var people;
var data_time;
var temp_min = 200;
var temp_max = -30;
	//Wgrywanie strony głównej
	
con.query("select * from measurements where measur_date BETWEEN \'"+dateFrom+" 00:00:01\' AND \'"+dateTo+" 23:59:59\' order by measur_date ASC", function (err, result) {
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
	console.log(result.length);
	if(result.length>0){
		if(result.length<=32){
		for(var i in result){
		labelsResult.push(result[i]["measur_date"].toString().slice(4,-21));
		temperature1.push(result[i]["temperature1"]);
		if(result[i]["temperature1"]<temp_min){temp_min=result[i]["temperature1"];}
		else if(result[i]["temperature1"]>temp_max){temp_max=result[i]["temperature1"];}
		temperature2.push(result[i]["temperature2"]);
		if(result[i]["temperature2"]<temp_min){temp_min=result[i]["temperature2"];}
		else if(result[i]["temperature2"]>temp_max){temp_max=result[i]["temperature2"];}
		temperature3.push(result[i]["temperature3"]);
		if(result[i]["temperature3"]<temp_min){temp_min=result[i]["temperature3"];}
		else if(result[i]["temperature3"]>temp_max){temp_max=result[i]["temperature3"];}
		 if(Math.abs(data_time-newDate(result[i]["measur_date"].toString().slice(4,-21)))<data_value){
			 data_value = Math.abs(data_time-newDate(result[i]["measur_date"].toString().slice(4,-21)));
			 data_index = i;
		 };

	}
	}
	else{
		var k = Math.floor(result.length/32);
		for(var i=0;i<result.length;i=i+k){
		labelsResult.push(result[i]["measur_date"].toString().slice(4,-21));
		temperature1.push(result[i]["temperature1"]);
		temperature2.push(result[i]["temperature2"]);
		temperature3.push(result[i]["temperature3"]);
		 if(Math.abs(data_time-newDate(result[i]["measur_date"].toString().slice(4,-21)))<data_value){
			 data_value = Math.abs(data_time-newDate(result[i]["measur_date"].toString().slice(4,-21)));
			 data_index = i;
		 };

	}
		
	}
	var temper = (result[data_index]["temperature1"]+result[data_index]["temperature2"]+result[data_index]["temperature3"])/3;
	}
	
	
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
		  console.log("Temp1: "+temperature1);
		console.log("Temp2: "+temperature2);
		console.log("Temp3: "+temperature3);
		console.log("DateTo: "+dateTo);
		console.log("DateFrom: "+dateFrom);
        //response.writeHead(200, { 'Content-Type': 'text/html' });
		data=data.replace('XLABELSX', labels);
		if(result.length>0){
			var datasets = '';
		var dataset = "{borderColor: 'rgba(255, 0, 0, 0.5)',backgroundColor: 'rgba(0, 0, 0, 0)', label: 'Temperature1',data: [XDATAX]}";
		if(temp1 == "on"){
			datasets = datasets+dataset.replace("XDATAX",temperature1.toString())+',';
		}
	  dataset = "{borderColor: 'rgba(0, 255, 0, 0.5)',backgroundColor: 'rgba(0, 0, 0, 0)',label: 'Temperature2',data: [XDATAX]}";
		if(temp2=="on"){datasets = datasets+dataset.replace("XDATAX",temperature2.toString())+',';}
		dataset = "{borderColor: 'rgba(0, 0, 255, 0.5)',backgroundColor: 'rgba(0, 0, 0, 0)',label: 'Temperature3',data: [XDATAX]}";
		if(temp3=="on"){
			datasets = datasets+dataset.replace("XDATAX",temperature3.toString())+',';
		}
		data = data.replace('XDATASETSX', datasets);
		data = data.replace('XMINX', (temp_min-1).toString());
		data = data.replace('XMAXX', (temp_max+1).toString());
		data = data.replace('XPEOPLEX',people.toString());
		data = data.replace('XTEMPERATUREX',(Math.round(temper*10)/10).toString());
		if(result[data_index]["smoke_sensor"]==0){data = data.replace('XFIREX','Good');}
		else{data = data.replace('XFIREX','Bad');}
		}
		else{
			data = data.replace('XDATASETSX', '');
		data = data.replace('XPEOPLEX','0');
		data = data.replace('XTEMPERATUREX',"Unknown");
		data = data.replace('XFIREX','Unknown');
		data = data.replace('XMINX', 20);
		data = data.replace('XMAXX', 25);
		}
		
        res.send(data);
    });
  });
  
  
  });
  })