(ns app
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [hiccup2.core :as h]
            [hiccup.util]
            [io.pedestal.log :as log]
            [io.pedestal.websocket :as ws]
            [clojure.core.async :as async]
            [clojure.data.json :as json]))

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
        [:body
         [:script {:src "/ws.js"}]
         [:p "Hello there"]
         [:p {:id "live-id"}]]])))

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
                  ::http/host "127.0.0.1"
                  ::http/secure-headers
                  {:content-security-policy-settings

                   "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' https: http:;"}
                  ::http/resource-path "/public"})))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

(comment
  ;; Can test server side update with this
  (async/put! (->> @ws-clients vals first) "{\"type\": \"REPLACE\", \"html\": \"test message from server\", \"id\": \"live-id\"}")

  ;; You can send html too
  (async/put! (->> @ws-clients vals first)
              (json/write-str
               {"type" "REPLACE"
                "id" "live-id"
                "html"
                (str
                 (h/html [:ul [:li "even"]
                          [:li "fancier"]
                          [:li "stuff"]]))}))
)
