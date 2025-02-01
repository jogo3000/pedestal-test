(ns app
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [hiccup2.core :as h]
            [hiccup.util]))

(defn main-page []
  (str
   (h/html
       (hiccup.util/raw-string "<!DOCTYPE html>\n")
       [:html
        [:head]
        [:p "Hello there"]])))

(defn respond-hello [request]
  {:status 200 :body (main-page)
   :headers {"Content-Type" "text/html"}})

(def routes
  (route/expand-routes
   #{["/" :get respond-hello :route-name :greet]}))

(defonce server (atom nil))

(defn server-definition
  ([]
   (server-definition {}))
  ([props]
   (http/create-server
    (merge {::http/routes routes
            ::http/type :jetty
            ::http/port 8890}
           props))))

(defn start
  ([]
   (start {}))
  ([props]
   (http/start (server-definition props))))

(defn start-dev []
  (reset! server
          (start {::http/join? false
                  ::http/host "127.0.0.1"})))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))
