# PushPlay2S

PushPlay2S is a Play! Framework 2.0 project that aims to be a clone of the <a href="http://www.pusher.com">Pusher</a> service clone. The project is designed to be easy to add as a subproject of an existing Play! 2.0 project, Please see the section Usage As A Subproject. It is compatible with the MIT licensed pusher javascript library. 
This project is currently considered to be in alpha development. 

## Requirements

- Play 2.1
- Redis >= 2.4
- Websockets compatible browser

## Usage

The quick and easy way to get this project up and running is to just clone the repo and start it up normally. This is a good way to use this application as a sandboxed piece of your infrastructure separate from your web application regardless of the technologies used to build and run your application. 

With a running redis server on localhost at port 6379 it goes something like this:

- $ git clone git@github.com:danbeaulieu/PushPlay2S.git
- $ cd PushPlay2S
- $ play
- [PushPlay2S] $ run

point your browser at localhost:9000

You can also embed this project inside an existing Play! 2.0 as a module quite simply using git. This way you can deploy both your existing web application and real time messaging support at the same time on the same hardware. Please see the section Usage As A Subproject for more details.

In both cases you should amodify the conf/pushplay2s.conf file to include these three properties:
<pre>
pusher.appId
pusher.key
pusher.secret
</pre>

You'll also need to overwrite the pusher connection settings in the pusher javascript client library. Namely the Pusher host and ws_port settings. Check out app/views/index.scala.html to see how easy this is.

## Usage As A Subproject

TODO

## Alternatives

<a href="https://github.com/stevegraham/slanger">Slanger</a> - An open source, robust, ruby based, self contained Pusher protocol server from Stevie Graham.

<a href="http://pusher.com">Pusher</a> - The service itself! They even have a free service plan!

## TODO

- Show usage as subproject
- Create github project page
- Make the demo better
- Add webhooks support
- Add hazelcast/0mq support instead of Redis

## Notes

