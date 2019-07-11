/* eslint-disable require-jsdoc */
const http = require('http');
const fs = require('fs');
const path = require('path');
const connections = new Set();
const exec = require('child_process').exec;

const server = http.createServer((request, response) => {
  const filePath = __dirname + ((request.url === '/') ? '/index.html' :
      path.normalize('/static/' + decodeURI(request.url)));
  const params = request.url.split('?')[0].split('/');
  switch(params.length) {
    case 2:
      console.log(request.url.split('?')[0].split('/').length);
      let body = '';
      request.on('data', chunk => {
          body += chunk.toString();
      });
      request.on('end', () => {
          const requestJson = JSON.parse(body);
          fs.writeFileSync('./choreo.bpmn', requestJson.xml);
          //exec('sh deployWindows.sh ' + requestJson.id);
          contentType = 'application/json';
          response.writeHead(200, {
            'Content-Type': contentType
          });
          response.write('Choreographie has been deployed successfully');
          response.end();
      });
      break;
    case 4:
      console.log(params[2]);
      contentType = 'application/json';
      response.writeHead(200, {
        'Content-Type': contentType
      });
      response.write('Choreographie has been deployed successfully');
      response.end();
      break;
    case 5:
      console.log(params[2]);
      contentType = 'application/json';
      response.writeHead(200, {
        'Content-Type': contentType
      });
      response.write('Choreographie has been deployed successfully');
      response.end();
      break;
    /*case '/testRPC':
      http.get('http://localhost:50005/api/generatedchoreo/peers', (resp) => {
        let data = '';
        resp.on('data', chunk => {
            data += chunk.toString();
        });
        resp.on('end', () => {
            const peers = JSON.parse(data);
            console.log(peers)
            contentType = 'application/json';
            response.writeHead(200, {
              'Content-Type': contentType
            });
            response.write(JSON.stringify(peers));
            response.end();
        });
      });
      break;*/
  }
}).on('clientError', (err, socket) => {
  socket.end('HTTP/1.1 400 Bad Request\r\n\r\n');
}).listen(8080);

