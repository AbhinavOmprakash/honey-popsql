(defproject honey-popsql "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [mount "0.1.16" :exclusions [org.clojure/clojure]]
                 [com.github.seancorfield/honeysql "2.0.783"]
                 [com.github.seancorfield/next.jdbc "1.2.674"]
                 [org.postgresql/postgresql  "42.3.5"]
                 [com.zaxxer/HikariCP "5.0.0"]
                 [metosin/jsonista "0.3.5"]]
  :plugins [[lein-eftest "0.5.9"]
            [lein-auto "0.1.3"]]
  :repl-options {:init-ns honey-popsql.core})
