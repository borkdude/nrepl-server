# nrepl-server

Proof of concept nREPL server for something that may end up in
[babashka](https://github.com/borkdude/babashka/). Mostly a port of
[ogion](https://gitlab.com/technomancy/ogion).

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

## Reverse engineering the nREPL protocol

When developing the server, it's useful to inspect messages that are sent back and forth between existing implementations.

E.g., start a lein nREPL server:

``` shell
$ lein repl
nREPL server started on port 56132 on host 127.0.0.1 - nrepl://127.0.0.1:56132
```

And connect to it with the development version of
[Calva](https://github.com/BetterThanTomorrow/calva).

To inspect messages, we're going to add some logging to `src/nrepl/index.ts`:

``` typescript
write(data: any) {
    console.log("write ->", data);
    ...
}
```

``` typescript
_response(data: any) {
    console.log("response ->", data);
    ...
}
```

Build Calva (`npm install` + run build task in VSCode) and run the extension in
debug mode (Run > Start Debugging).

Via the command palette, select `Calva: connect to a running REPL server outside project`.

Behold:

``` javascript
write ->
Object
op
"clone"
session
"2699f4ad-b917-4224-a879-a5bfc8273a57"
id
"4"
response ->
Object
id
"4"
new-session
"310e3c0b-2607-4490-ad14-b1a04cb614d9"
session
"2699f4ad-b917-4224-a879-a5bfc8273a57"
status
Array[1]
0
"done"
write ->
Object
op
"eval"
session
"310e3c0b-2607-4490-ad14-b1a04cb614d9"
code
"(+ 1 1)"
id
"5"
pprint
false
response ->
Object
id
"5"
ns
"user"
session
"310e3c0b-2607-4490-ad14-b1a04cb614d9"
value
"2"
response ->
Object
id
"5"
session
"310e3c0b-2607-4490-ad14-b1a04cb614d9"
status
Array[1]
0
"done"
```

## References

- [nrepl-cljs](https://github.com/djblue/nrepl-cljs)
- [ogion](https://gitlab.com/technomancy/ogion)
