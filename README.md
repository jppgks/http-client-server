# HTTP Client and Server [![Build Status](https://travis-ci.com/jppgks/http-client-server.svg?token=PY7TMg87v7PquV1Ujqhf&branch=master)](https://travis-ci.com/jppgks/http-client-server)
Client and server implementation of the Hypertext Transfer Protocol (HTTP) using Java Sockets.

Written by [@StijnCaerts](https://github.com/StijnCaerts) and [@jppgks](https://github.com/jppgks) for the second assignment of [Computer Networks](https://onderwijsaanbod.kuleuven.be/syllabi/e/G0Q43AE.htm#activetab=doelstellingen_idp535264) 
to learn Socket programming and get familiarized with the basics of distributed programming.

## Run server
```shell
# 🐳 Pulls image, then runs server publishing port 8080 on localhost
docker run -d -p 8080:8080 jppgks/http-server
```

Interact with the server at `localhost:8080`! 🎉

## Run client
```shell
cd client
# Modify parameters as you see fit
gradle run -Pmethod="GET" -Phost="localhost" -Pport="8080"
```
