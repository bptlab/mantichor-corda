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
            for(let i = 0; i < rpcServer.length-1; i++) {
              setTimeout(() => exec('sh deployNodeServer.sh ' + requestJsonDeploy.id + ' ' + rpcServer[i]), 30000 + i * 1000);
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
        http.get('http://localhost:50005/api/generated' + requestJsonTasks.id.charAt(0).toLowerCase() + requestJsonTasks.id.slice(1) +
          '/choreographies', (resp) => {
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
      let bodyExecute = '';
      request.on('data', chunk => {
          bodyExecute += chunk.toString();
      });

      request.on('end', () => {
        const requestJsonExecute = JSON.parse(bodyExecute);
        fs.writeFileSync('./choreo.bpmn', requestJsonExecute.xml);
        const executeTask = requestJsonExecute.task[0].replace(' ', '\\ ');
        exec('java -jar InitGen.jar ' + executeTask, (err, stdout, stderr) => {
          const executeParam = fs.readFileSync('changeRequest.txt', 'utf8').split('\n');
          let paramsString = '?';
          for(let i = 1; i < executeParam.length; i++) {
            paramsString += executeParam[i];
            if(i < executeParam.length - 1) {
              paramsString += '&'
            }
          }
          while(paramsString.includes(' ')){
            paramsString = paramsString.replace(' ', '');
          }
          if(requestJsonExecute.task[0].charAt(0).toUpperCase() + requestJsonExecute.task[0].slice(1) == 'Init'){
            requestJsonExecute.task[0] = 'createChoreographie';
          }
          const options = {
            hostname: 'localhost',
            port: parseInt(executeParam[0]),
            path: '/api/generated' + requestJsonExecute.id.charAt(0).toLowerCase() + requestJsonExecute.id.slice(1) +
            '/' + requestJsonExecute.task[0].charAt(0).toUpperCase() + requestJsonExecute.task[0].slice(1) + paramsString,
            method: 'POST',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          };
          const postReq = http.request(options, (resp) => {
            let data = '';
            resp.on('data', chunk => {
              data += chunk.toString();
            });
            resp.on('end', () => {
              response.writeHead(200, {
                'Content-Type': 'text/plain'
              });
              response.write(data);
              response.end();
            });
          });
          postReq.on('error', (e) => {
            console.error(`problem with request: ${e.message}`)
            response.writeHead(500, {
              'Content-Type': 'text/plain'
            });
            response.write('ServerError');
            response.end();;
          });
          postReq.end();
        });
      });
      break;
  }
}).on('clientError', (err, socket) => {
  socket.end('HTTP/1.1 400 Bad Request\r\n\r\n');
}).listen(8080);
