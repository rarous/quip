(defproject rarous/quip "0.1.0"
  :description "Clojure client for Quip APIs"
  :url "https://github.com/rarous/quip"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[com.cognitect/transit-clj "0.8.259"]
                 [http-kit "2.1.19"]]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :scm {:name "git" :url "https://github.com/rarous/quip"}
  :deploy-repositories [["releases" :clojars]
                        ["clojars" {:creds :gpg}]])
