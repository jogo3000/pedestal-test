(ns app
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [hiccup2.core :as h]
            [hiccup.util]
            [io.pedestal.log :as log]
            [io.pedestal.websocket :as ws]))

(defonce ws-clients (atom {}))
(defrecord WebsocketSession [id ch])

(def ws-paths
  {"/ws" {:on-open (fn [session _]
                     (let [id (.getId session)]
                       (log/info :msg (str "New ws session opened: " id))
                       (->WebsocketSession
                        id
                        (swap! ws-clients assoc id
                               (ws/start-ws-connection session {})))))
          :on-text (fn [{:keys [id]} msg]
                     (log/info :msg (str "Client " id " sent " msg)))
          :on-binary (fn [{:keys [id]} bb]
                       (log/info :msg (str "Binary message from " id)))
          :on-error (fn [{:keys [id]} _ t]
                      (log/error :msg (str "WS Error from client " id) :exception t))
          :on-close (fn [{:keys [id]} _ _]
                      (swap! ws-clients dissoc id)
                      (log/info :msg (str "Websocket closed by :" id)))}})

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
            ::http/websockets ws-paths
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
