(ns honey-popsql.test-utils
  (:require
    [honey.sql :as sql]
    [jsonista.core :as json]
    [mount.core :refer [defstate] :as mount]
    [next.jdbc :as jdbc]
    [next.jdbc.connection :as connection]
    [next.jdbc.prepare :as prepare]
    [next.jdbc.result-set :as rs])
  (:import
    com.zaxxer.hikari.HikariDataSource
    (java.sql
      PreparedStatement)
    (org.postgresql.util
      PGobject)))


(declare conn)


(defn create-connection
  [db-spec]
  (connection/->pool HikariDataSource
                     db-spec))


(def postgres
  {:dbtype "postgres"
   :host "localhost"
   :port 5432
   :dbname "honey_popsql"
   :username "popsicle"
   :password nil})


(defstate conn
  :start (create-connection postgres)
  :stop (.close ^HikariDataSource conn))


(mount/start #'conn)


(defn jdbc-execute!
  [conn q]
  (jdbc/execute! conn (sql/format q)
                 {:builder-fn rs/as-unqualified-kebab-maps}))


(defmacro with-tx-rollback
  "Runs a function/body inside a transaction. 
  Will rollback the transaction after the execution is over."
  [[sym transactable opts] & body]
  `(jdbc/with-transaction [~sym ~transactable ~opts]
                          (binding [next.jdbc.transaction/*nested-tx* :ignore]
                            (try
                              ~@body
                              (catch Throwable t#
                                (throw t#))
                              (finally
                                (.rollback ~sym))))))


;; taken from https://cljdoc.org/d/seancorfield/next.jdbc/1.2.659/doc/getting-started/tips-tricks#working-with-json-and-jsonb

;; :decode-key-fn here specifies that JSON-keys will become keywords:
(def mapper (json/object-mapper {:decode-key-fn keyword}))
(def ->json json/write-value-as-string)
(def <-json #(json/read-value % mapper))


(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))


(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^org.postgresql.util.PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (<-json value) {:pgtype type}))
      value)))


(set! *warn-on-reflection* true)


;; if a SQL parameter is a Clojure hash map or vector, it'll be transformed
;; to a PGobject for JSON/JSONB:
(extend-protocol prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))

  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))


;; if a row contains a PGobject then we'll convert them to Clojure data
;; while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v)))
