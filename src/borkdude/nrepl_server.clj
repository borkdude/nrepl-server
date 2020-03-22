(ns borkdude.nrepl-server
  (:refer-clojure :exclude [send])
  (:require [bencode.core :refer [write-bencode read-bencode]]
            [clojure.stacktrace :as stacktrace])
  (:import [java.net ServerSocket] [java.io PushbackInputStream EOFException]))

(def port 1667)
(def debug? true)

(defn response-for [old-msg msg]
  (let [session (get old-msg :session "none")
        m (assoc msg "session" session)
        id (get old-msg :id "unknown")
        m (assoc m "id" id)]
    m))

(defn send [os msg]
  (when debug? (println "Sending" msg))
  (write-bencode os msg)
  (.flush os))

(defn send-exception [os msg ex]
  (when debug? (prn "sending ex" (with-out-str (stacktrace/print-throwable ex))))
  (send os (response-for msg {"ex" (with-out-str (stacktrace/print-throwable ex))
                              "status" #{"done"}})))

(defn eval-msg [o msg ns]
  (let [code-str (get msg :code)
        code (if (= :nil code-str)
               nil
               ;; TODO: sci parse, etc.
               (read-string code-str))
        ;; TODO: sci eval
        value (eval code)]
    (send o (response-for msg {"value" (pr-str value)
                               "status" #{"done"}}))))

(defn register-session [i o ns msg session-loop]
  (let [id (str (java.util.UUID/randomUUID))]
    (send o (response-for msg {"new-session" id "status" #{"done"}}))
    (session-loop i o id ns)))

(defn read-msg [msg]
  (-> (zipmap (map keyword (keys msg))
              (map #(if (instance? (Class/forName "[B") %)
                      (String. %)
                      %) (vals msg)))
      (update :op keyword)))

(defn session-loop [is os id ns]
  (println "Reading!" id (.available is))
  (when-let [msg (try (read-bencode is)
                 (catch EOFException _
                   (println "Client closed connection.")))]
    (let [msg (read-msg msg)]
      ;; (when debug? (prn "Received" msg))
      (case (get msg :op)
        :clone (do
                 (when debug? (println "Cloning!"))
                 (register-session is os ns msg session-loop))
        :eval (do
                (try (eval-msg os msg ns)
                     (catch Exception exn
                       (send-exception os msg exn)))
                (recur is os id ns))
        :describe
        (do (send os (response-for msg {"status" #{"done"}
                                        "aux" {}
                                        "ops" (zipmap #{"clone", "describe", "eval"}
                                                      (repeat {}))
                                        "versions" {"nrepl" {"major" "0"
                                                             "minor" "4"
                                                             "incremental" "0"
                                                             "qualifier" ""}
                                                    "clojure"
                                                    {"*clojure-version*"
                                                     (zipmap (map name (keys *clojure-version*))
                                                             (vals *clojure-version*))}}}))
            (recur is os id ns))
        (when debug?
          (println "Unhandled message" msg))))))

(defn listen [listener]
  (when debug? (println "Listening"))
  (let [client-socket (.accept listener)
        in (.getInputStream client-socket)
        in (PushbackInputStream. in)
        out (.getOutputStream client-socket)]
    (when debug? (println "Connected."))
    ;; TODO: run this in a future, but for debugging this is better
    (binding [*print-length* 20]
      (session-loop in out "pre-init" *ns*))
    #_(recur listener)))

(defn -main [& _args]
  (println "Starting on port" port)
  (listen (new ServerSocket port)))
