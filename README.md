# nrepl-server

Proof of concept nREPL server. Mostly a port of [ogion](https://gitlab.com/technomancy/ogion).

## Run

``` shell
$ clojure -m borkdude.nrepl-server
Starting on port 1667
```

Connect with your favorite nREPL client:

``` shell
$ rep -p 1667 '(+ 1 2 3)'
6
```

TODO:

Make it work for `lein repl :connect 1667`.
