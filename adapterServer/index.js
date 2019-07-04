/* eslint-disable require-jsdoc */
const http = require('http');
const fs = require('fs');
const path = require('path');
const WebSocketServer = require('websocket').server;
const connections = new Set();
const exec = require('child_process').exec;

const server = http.createServer((request, response) => {
  const filePath = __dirname + ((request.url === '/') ? '/index.html' :
      path.normalize('/static/' + decodeURI(request.url)));
  switch(request.url.split('?')[0]) {
    case '/choreographies':
      let body = '';
      request.on('data', chunk => {
          body += chunk.toString();
      });
      request.on('end', () => {
          const requestJson = JSON.parse(body);
          fs.writeFileSync('./choreo.bpmn', requestJson.xml);
          exec('sh deploy.sh ' + requestJson.id);

      });
      contentType = 'application/json';
      break;
  }
  response.writeHead(200, {
    'Content-Type': contentType
  });
  response.write('Choreographie has been deployed successfully');
  response.end();
}).on('clientError', (err, socket) => {
  socket.end('HTTP/1.1 400 Bad Request\r\n\r\n');
}).listen(8080);


wsServer = new WebSocketServer({
  httpServer: server,
  autoAcceptConnections: false
});
wsServer.on('request', (request) => {
  const connection = request.accept();
  connections.add(connection);
  console.log((new Date()) + ' Connection accepted.');
  connection.on('close', (reasonCode, description) => {
    console.log((new Date()) + ' Peer ' +
        connection.remoteAddress + ' disconnected.');
    connections.delete(connection);
  });
});
