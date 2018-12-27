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
  database: 'smart_office'
});


//Podłączanie się do bazy danych + wysyłanie jednego zapytania
  con.connect(function(err) {
  if (err) throw err;
  console.log("Connected!");
  con.query("select * from temperatures", function (err, result) {
    if (err) throw err;
    console.log("Result: " + result);
  });
});


var eventEmitter = new events.EventEmitter();
//Główna pętla serwera
http.createServer(function (req, response) {
	//Wgrywanie strony głównej
    fs.readFile('index.html', 'utf-8', function (err, data) {
        response.writeHead(200, { 'Content-Type': 'text/html' });

        //var chartData = [];
        //for (var i = 0; i < 7; i++)
         //   chartData.push(Math.random() * 50);

		//var result = data.replace('{{chartData}}', JSON.stringify(chartData));
        response.write(data);
        response.end();
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