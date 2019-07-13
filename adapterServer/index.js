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
      let bodyDeploy = '';
      request.on('data', chunk => {
          bodyDeploy += chunk.toString();
      });
      request.on('end', () => {
        const requestJsonDeploy = JSON.parse(bodyDeploy);
        fs.writeFileSync('./choreo.bpmn', requestJsonDeploy.xml);
        exec('sh deploy.sh ' + requestJsonDeploy.id, (err, stdout, stderr) => {
          if(err){
            console.log(err);
          } else {
            const rpcServer = fs.readFileSync('deployServer.txt', 'utf8').split('\n');
            for(let i = 0; i < rpcServer.length; i++) {
              exec('./gradlew ' + rpcServer[i]);
            }
            contentType = 'application/json';
            response.writeHead(200, {
              'Content-Type': contentType
            });
            response.write('Choreographie has been deployed successfully');
            response.end();
          }
        });
      });
      break;
    case 4:
      let bodyTasks = '';
      const returnObject = {tasks: []};
      request.on('data', chunk => {
          bodyTasks += chunk.toString();
      });
      request.on('end', () => {
        const requestJsonTasks = JSON.parse(bodyTasks);
        http.get('http://localhost:50005/api/generatedchoreo/choreographies', (resp) => {
          let data = '';
          resp.on('data', chunk => {
              data += chunk.toString();
          });
          resp.on('end', () => {
              const tasksCorda = data;
              if(tasksCorda == '[]') {
                returnObject.tasks = ['init'];
                contentType = 'application/json';
                response.writeHead(200, {
                  'Content-Type': contentType
                });
                response.write(JSON.stringify(returnObject));
                response.end();
              } else {
                let currentState = tasksCorda;
                const fields = currentState.split(',');
                for(let i = 0; i < fields.length; i++){
                  if(fields[i].includes('stateEnum')) {
                    currentState = parseInt(fields[i].split('=')[1]);
                  }
                }
                fs.writeFileSync('./choreo.bpmn', requestJsonTasks.xml);
                exec('java -jar TasksGen.jar ' + currentState, (err, stdout, stderr) => {
                  if(err) {
                    console.log('error', err);
                  } else {
                    const tasks = fs.readFileSync('./tasks.txt', 'utf8');
                    const taskArray = tasks.split('\n');
                    returnObject.tasks = taskArray;
                    contentType = 'application/json';
                    response.writeHead(200, {
                      'Content-Type': contentType
                    });
                    response.write(JSON.stringify(returnObject));
                    response.end();
                  }
                });
              }
          });
        });
      });
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

