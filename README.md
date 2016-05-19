# Demo: Using elasticsearch for the logging use-case

These are my written notes I used to demo the Elastic Stack at infoshare 2016.
The virtual machine ships with the whole Elastic Stack on version 5.0.0-alpha2

This includes showing

* elasticsearch
* kibana
* Indexing data into Elasticsearch using Console in Kibana
* Indexing data into Elasticsearch using filebeat
* Running topbeat, importing the dashboards and showing them off
* Indexing data into Elasticsearch using logstash
* Indexing data into Elasticsearch using logstash via filebeat
* Indexing data into Elasticsearch using logstash via redis
* Indexing data into Elasticsearch using logstash from redis from filebeat
* Showing how to write your own ingest processor, result is in `ingest-sum`

Obviously a fair share things are just listed here, but concrete solutions might be missing without the talk.

The vagrant vm resides in `ubuntu-15-vagrant-vm`, you can run `vagrant up` to get it up and running.
Then comment out this line in the Vagrantfile, so that you get access to the synced folder.

```
  config.vm.synced_folder ".", "/vagrant"
```

This folder contains the sample log line to test things as well as a tmux shell script which starts
half of your screen as your typing terminal, where as the other two can be used for checking systemd
logs or the redis monitoring.

This repository includes the Vagrantfile, sample files and the example processor that I hacked together thoughout the demo.
The processor obviously has flaws, like not being able to sum up doubles, but it serves its simple purpose being hacked together in 10 minutes.

# Installation

* Show setup of VM, show Vagrantfile
* SSH into vm, start tmux, explain tmux setup
* Start elasticsearch
* curl localhost:9200
* Start kibana, open brower at http://localhost:5601/


## Demo: Console

* `GET /`
* Create an index
* Index a document
* GET a document
* Search for a document

```
PUT /my-index

PUT my-index/article/1
{
  "title" : "Awesome logging using the Elastic Stack",
  "speaker" : "Alexander Reelsen",
  "day": "thursday",
  "date": "2016-05-17T11:00:00.000+01:00"
}

PUT my-index/article/2
{
  "title" : "Closing keynote",
  "speaker" : "An awesome speaker",
  "day": "friday"
}

GET my-index/article/1

GET my-index/_search

GET my-index/_search?q=Alex*

# show different queries, match, bool + filter, match prefix
GET my-index/_search
{
  "query": {
    "bool": {
      "filter": {
        "term": {
          "day": "friday"
        }
      }
    }
  }
}

PUT my-index/article/_bulk
{ "index" : { "_id" : 3 } }
{ "title" : "Something else" }

GET my-index/_search
{
  "size": 0,
  "aggs": {
    "count-by-day": {
      "terms": {
        "field": "speaker.keyword",
        "size": 10
      }
    }
  }
}
```

* Mapping (explain types like date, long and keyword)
* cat API


## Demo: Filebeat

* Ingesting data using filebeat, by echoing into a file (check `/vagrant/single-log-entry.txt`)
* Configure file to be `/tmp/infoshare.log`
* Show data in kibana console, by searching
* See that everything is just a single field by showing mapping


## Demo: Ingest node

* Explain ingest node processors
* Configure pipeline

```
PUT /_ingest/pipeline/access-log-pipeline
{
  "description" : "Apache Logs Pipeline",
  "processors" : [
    {
    "grok" : {
      "field" : "message",
      "pattern" : "%{COMBINEDAPACHELOG}"
    }
  },
    {
    "convert" : {
      "field": "response",
      "type": "integer"
    }
},
  {
    "convert" : {
      "field": "bytes",
      "type": "integer"
    }
},
  {
    "date" : {
      "field": "timestamp",
      "formats" : [ "dd/MMM/YYYY:HH:mm:ss Z" ]
    }
  },  {
    "geoip" : {
      "field" : "clientip"
    }
  }
 ]
}
```

* Show simulate API

```
POST _ingest/pipeline/access-log-pipeline/_simulate
{
  "docs" : [
    { "_source" : { "message" : "95.90.51.27 - - [06/May/2016:16:06:45 +0200] \"GET /posts/2015-05-04-producing-technical-documentation-an-overview.html HTTP/1.1\" 200 11755 \"http://www.google.de/\" \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/601.6.17 (KHTML, like Gecko)\"" } }
  ]
}
```

* Index a sample document

```
PUT my-index/log/1?pipeline=access-log-pipeline
{
  "message" : "95.90.51.27 - - [06/May/2016:16:06:45 +0200] \"GET /posts/2015-05-04-producing-technical-documentation-an-overview.html HTTP/1.1\" 200 11755 \"http://www.google.de/\" \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/601.6.17 (KHTML, like Gecko)\""
}
```


## Demo: filebeat with pipeline

* configure pipeline in filebeat, add
```
parameters:
  pipeline: access-log-pipeline
```

* index same documents from above again

```
echo '95.90.51.27 - - [06/May/2016:16:06:45 +0200] "GET /posts/2015-05-04-producing-technical-documentation-an-overview.html HTTP/1.1" 200 11755 "http://www.google.de/" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/601.6.17 (KHTML, like Gecko)' >> /tmp/mylog.log
```

* Show what happens on an outage of elasticsearch, picking up at the same position


## Demo: topbeat

* Start topbeat, indexes automatically into localhost
* Execute a search in sense and show dashboard demo later
* Show what happens on an outage of elasticsearch (documents get lost)


## Demo: logstash

Create this simple config file

```
input {
  stdin {}
}
output {
  stdout {
    codec => rubydebug
  }
}
```

Run `echo foo | /opt/logstash/bin/logstash -f simple.conf`

Create apache parsing configuration in `/etc/logstash/conf.d/apache.conf`

```
input {
  stdin { }
}

filter {
  grok {
    match => {
      "message" => '%{IPORHOST:clientip} %{USER:ident} %{USER:auth} \[%{HTTPDATE:timestamp}\] "%{WORD:verb} %{DATA:request} HTTP/%{NUMBER:httpversion}" %{NUMBER:response:int} (?:-|%{NUMBER:bytes:int}) %{QS:referrer} %{QS:agent}'
    }
  }

  date {
    match => [ "timestamp", "dd/MMM/YYYY:HH:mm:ss Z" ]
    locale => en
  }

  geoip {
    source => "clientip"
  }

  useragent {
    source => "agent"
    target => "useragent"
  }
}

output {
  elasticsearch {
    hosts => [ "localhost:9200" ]
  }
}
```

Run `cat /vagrant/single-log-entry.log | /opt/logstash/bin/logstash -f simple.conf`
Check in console that the entry is there

Fix the input to be beats as below

```
input {
  beats {
    port => 5044
  }
}

filter {
  grok {
    match => {
      "message" => '%{IPORHOST:clientip} %{USER:ident} %{USER:auth} \[%{HTTPDATE:timestamp}\] "%{WORD:verb} %{DATA:request} HTTP/%{NUMBER:httpversion}" %{NUMBER:response:int} (?:-|%{NUMBER:bytes:int}) %{QS:referrer} %{QS:agent}'
    }
  }

  date {
    match => [ "timestamp", "dd/MMM/YYYY:HH:mm:ss Z" ]
    locale => en
  }

  geoip {
    source => "clientip"
  }

  useragent {
    source => "agent"
    target => "useragent"
  }
}

output {
#  stdout { codec => rubydebug }
  elasticsearch {
    hosts => [ "localhost:9200" ]
  }
}
```

Configure filebeat to report to logstash instead of elasticsearch, also comment out all the elasticsearch output (including the template). And restart.
Also start logstash.

```
logstash:
  hosts: ["localhost:5044"]
```

Optional: Do this for topbeat

Show it works: `cat /vagrant/single-log-entry.log >> /tmp/infoshare.log`

Now we report to logstash. Logstash has an internal queue, but this is very small, so the backpressure is applied to the client. If the client is not able to handle this (like topbeat), we are not a step further.


## Demo: broker + logstash

Time to have another component in between that is good with caching. Lets use redis, but you could also use other brokers like zeromq or kafka.

Stop logstash. Redis is already running, see `redis-cli ping`

```
input {
  redis {
    codec => plain
    data_type => channel
    key => beats
  }
}
output {
  stdout {
    codec => rubydebug
  }
}
```

Run

```
redis-cli PUBLISH beats mymessage
redis-cli PUBLISH beats "`cat /vagrant/single-log-entry.txt`"
```

Run `redis-cli monitor` in another tab

Now apply add this logstash change to our configuration in `/etc/logstash/conf.d/apache.conf` as an additional input

```
redis {
  codec => plain
  data_type => channel
  key => beats
}
```

Next up: Change beats to write to redis

```
output:
  redis:
    hosts: ["localhost"]
    datatype: channel
    index: beats
```

Restart logstash, restart filebeat

```
cat /vagrant/single-log-entry.txt >> /tmp/infoshare.log
```

* Start feeding apache logs into the filebeat
* Check indices counts `_cat/indices?v&h=index,docs.count`


## Demo: topbeat dashboard

First we need to import the dashboards, while kibana is running

```
cd /usr/share/topbeat/kibana/
./import_dashboards.sh
```

## Demo: Kibana

* Add new index pattern
* Show discover tab
* Show visualizations tab
* Show dashboard tab

* Show topbeat dashboard
* Load apache logs dashboard `kibana-logs-dashboard.json`


## Demo: Write your own processor


* Run `cookiecutter gh:spinscale/cookiecutter-elasticsearch-ingest-processor`
* Run `gradle idea`
* Run `open *.ipr`
* Create processor that sums up an arbitrary number of fields (reading from an array)
* Run `gradle clean check`
* Install plugin `sudo /usr/share/elasticsearch/bin/elasticsearch-plugin install file:///vagrant/ingest-sum-0.0.1-SNAPSHOT.zip`

```
PUT /_ingest/pipeline/bytes-sum-pipeline
{
  "description" : "Bytes sum pipeline",
  "processors" : [
    {
      "sum" : {
        "field" : "bytes_total",
        "fields" : [ "bytes_out", "bytes_in" ]
      }
    }
  ]
}

PUT foo/bar/1?pipeline=bytes-sum-pipeline
{
  "bytes_in" : 12345,
  "bytes_out" : 54321
}

GET foo/bar/1
```


## Demo: Timelion

* Install timelion (might take some time), run `/opt/kibana/bin/kibana-plugin install timelion`
* Restart kibana
* Open http://localhost:5601/app/timelion


## Final notes

* Mention x-pack: security, alerting, monitoring as commercial extensions
* Pipeline aggregations
