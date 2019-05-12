hab-mqtt2pg
===========

This application is part of our **personal** "Home-Automation Bus".  It
persists MQTT messages to a [Timescale](https://www.timescale.com) or plain
[PostgreSQL](https://www.postgresql.org/) database.  

The application runs on the [node.js](https://nodejs.org) platform. The source
code is written in [ClojureScript](https://clojurescript.org/index) and build
with [shadow-cljs](https://github.com/thheller/shadow-cljs). There is also a
deployment recipe to be used with [Ansible](https://www.ansible.com/) which 
will install a systemd service on a recent Ubuntu installation. 


Development
-----------


See also `.mux.yml`.

Continuous build:

    shadow-cljs watch application


REPL 

    ./node_modules/.bin/shadow-cljs cljs-repl application

Run

    node mqtt2pg -h

    

Deployment
----------

```sh
ansible-playbook \
  -i ~/Programming/SystemEngineering/my-servers_ansible/hosts_home.yml -l hab deploy/deploy_play.yml \
  -e pg_password=REPLACE_ME \
  -e '{"clean_workingdir":False}'
```


Copyright
---------

Copyright © 2019 Thomas Schank <DrTom@schank.ch>
This work is free. You can redistribute it and/or modify it under the
terms of the Do What The Fuck You Want To Public License, Version 2,
as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.


Misc
----

### Show events 

```sh
ssh root@hab
mosquitto_sub -v -h 127.0.0.1 -p 1883 -t '#'
```

### Database Queries 


    SELECT DISTINCT ON (topic) number_events.topic, time, value FROM number_events ORDER BY topic, time DESC;

    SELECT count(*) FROM number_events GROUP BY topic ORDER BY topic ;



